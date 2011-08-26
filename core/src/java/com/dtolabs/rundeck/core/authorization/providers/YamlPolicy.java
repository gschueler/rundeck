/*
 * Copyright 2011 DTO Labs, Inc. (http://dtolabs.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

/*
* YamlPolicy.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 8/25/11 11:25 AM
* 
*/
package com.dtolabs.rundeck.core.authorization.providers;

import com.dtolabs.rundeck.core.authorization.Explanation;
import com.dtolabs.rundeck.core.utils.PairImpl;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.PredicateUtils;
import org.apache.commons.collections.Transformer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * YamlPolicy implements a policy from a yaml document input or map.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
final class YamlPolicy implements Policy {

    public static final String TYPE_PROPERTY = "type";
    public static final String FOR_SECTION = "for";
    public static final String JOB_TYPE = "job";
    public static final String RULES_SECTION = "rules";
    public static final String ACTIONS_SECTION = "actions";
    public Map policyInput;

    private Set<String> usernames = new HashSet<String>();
    private Set<Object> groups = new HashSet<Object>();
    private static ConcurrentHashMap<String, Pattern> patternCache = new ConcurrentHashMap<String, Pattern>();
    AclContext aclContext;
    private final ConcurrentHashMap<String, AclContext> typeContexts = new ConcurrentHashMap<String, AclContext>();

    public YamlPolicy(final Map yamlDoc) {
        policyInput = yamlDoc;
        parseByClause();
        createAclContext();
    }

    public Set<String> getUsernames() {
        return usernames;
    }

    public Set<Object> getGroups() {
        return groups;
    }

    public AclContext getContext() {

        return aclContext;
    }

    private void createAclContext() {
        aclContext = new YamlAclContext(policyInput);
    }


    private AclContext createLegacyContext(final Map rules) {
        return new LegacyRulesContext(rules);

    }

    private AclContext createTypeContext(final List typeSection) {

        return new TypeContext(createTypeRules(typeSection));
    }

    List<ContextMatcher> createTypeRules(List typeSection) {
        ArrayList<ContextMatcher> rules = new ArrayList<ContextMatcher>();
        for (final Object o : typeSection) {
            Map section = (Map) o;
            rules.add(createTypeRuleContext(section));
        }
        return rules;
    }

    /**
     * Create acl context  for specific rule in a type context
     */
    ContextMatcher createTypeRuleContext(final Map section) {
        return new TypeRuleContextMatcher(section);
    }

    private static String createLegacyJobResourcePath(Map<String, String> resource) {
        return resource.get("group") + "/" + resource.get("job");
    }

    /**
     * parse the by: clause.
     */
    private void parseByClause() {
        Object byClause = policyInput.get("by");
        if (byClause == null) {
            return;
        }
        if (!(byClause instanceof Map)) {
            return;
        }
        @SuppressWarnings ("rawtypes")
        Map by = (Map) byClause;
        @SuppressWarnings ("rawtypes")
        Set<Map.Entry> entries = by.entrySet();
        for (@SuppressWarnings ("rawtypes") Map.Entry policyGroup : entries) {

            if ("username".equals(policyGroup.getKey())) {
                usernames.add(policyGroup.getValue().toString());
            }

            if ("group".equals(policyGroup.getKey())) {
                groups.add(policyGroup.getValue());
            }

            // TODO Support LDAP
        }
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("YamlPolicy[id:");
        sb.append(policyInput.get("id")).append(", groups:");
        for (Object group : getGroups()) {
            sb.append(group.toString()).append(" ");
        }
        sb.append("]");
        return sb.toString();
    }

    static class TypeContext implements AclContext {
        private final List<ContextMatcher> typeRules;

        public TypeContext(List<ContextMatcher> typeRules) {
            this.typeRules = typeRules;
        }

        public ContextDecision includes(final Map<String, String> resource, final String action) {
            ArrayList<ContextEvaluation> evaluations = new ArrayList<ContextEvaluation>();
            boolean allowed = false;
            boolean denied = false;
            ContextEvaluation deniedEvaluation;
            for (final ContextMatcher evaluator : typeRules) {
                final MatchedContext matched = evaluator.includes(resource, action);
                if (!matched.isMatched()) {
                    //indicates the section did not match
                    continue;
                }
                final ContextDecision decision = matched.getDecision();
                if (decision.granted()) {
                    allowed = true;
                }
                evaluations.addAll(decision.getEvaluations());
                if (!denied) {
                    for (final ContextEvaluation contextEvaluation : decision.getEvaluations()) {
                        if (contextEvaluation.id == Explanation.Code.REJECTED_DENIED) {
                            deniedEvaluation = contextEvaluation;
                            denied = true;
                            break;
                        }
                    }
                }
            }
            return new ContextDecision(denied ? Explanation.Code.REJECTED_DENIED
                                              : allowed ? Explanation.Code.GRANTED : Explanation.Code.REJECTED,
                allowed && !denied, evaluations);

        }
    }
    static class MatchedContext extends PairImpl<Boolean,ContextDecision> {
        MatchedContext(Boolean first, ContextDecision second) {
            super(first, second);
        }
        public Boolean isMatched() {
            return getFirst();
        }
        public ContextDecision getDecision(){
            return getSecond();
        }
    }
    static interface ContextMatcher {
        public MatchedContext includes(Map<String, String> resource, String action);
    }

    /**
     * returns an allow/reject decision for a specific rule within a type section, can return null indicating there was
     * no match.
     * Format:
     * <pre>
     *     match:
     *       key: regex
     *       key2: [regexa, regexb]
     *     equals:
     *       key: value
     *     contains:
     *       key: value
     *       key2: [value1,value2]
     *     allow: action
     *     # or
     *     allow: [action1,action2]
     *     deny: action
     *     #or
     *     deny: [action1,action2]
     * </pre>
     */
    static class TypeRuleContextMatcher implements ContextMatcher {
        public static final String MATCH_SECTION = "match";
        public static final String EQUALS_SECTION = "equals";
        public static final String CONTAINS_SECTION = "contains";
        public static final String ALLOW_ACTIONS = "allow";
        public static final String DENY_ACTIONS = "deny";
        Map ruleSection;


        TypeRuleContextMatcher(final Map ruleSection) {
            this.ruleSection = ruleSection;
        }

        private static ConcurrentHashMap<String, Pattern> patternCache = new ConcurrentHashMap<String, Pattern>();
        private Pattern patternForRegex(final String regex) {
            if (!patternCache.containsKey(regex)) {
                patternCache.putIfAbsent(regex, Pattern.compile(regex));
            }
            return patternCache.get(regex);
        }

        public MatchedContext includes(Map<String, String> resource, String action) {
            boolean matched = true;
            List<ContextEvaluation> evaluations = new ArrayList<ContextEvaluation>();

            matched = matchesRuleSections(resource,evaluations);

            if (!matched) {
                return new MatchedContext(false, new ContextDecision(Explanation.Code.REJECTED, false, evaluations));
            }
            return new MatchedContext(true, evaluateActions(action, evaluations));
        }

        ContextDecision evaluateActions(String action, List<ContextEvaluation> evaluations) {
            //evaluate actions
            boolean denied = false;

            if (ruleSection.containsKey(DENY_ACTIONS)) {
                HashSet<String> actions = new HashSet<String>();
                final Object actionsObj = ruleSection.get(DENY_ACTIONS);
                if (actionsObj instanceof String) {
                    final String actionStr = (String) actionsObj;
                    actions.add(actionStr);
                } else if (actionsObj instanceof List) {
                    actions.addAll((List<String>) actionsObj);
                } else {
                    evaluations.add(new ContextEvaluation(Explanation.Code.REJECTED_CONTEXT_EVALUATION_ERROR,
                        "Invalid action type."));
                }
                if (0 == actions.size()) {
                    //xxx:warn
                } else if (actions.contains("*") || actions.contains(action)) {
                    evaluations.add(new ContextEvaluation(Explanation.Code.REJECTED_DENIED,
                        super.toString() + ": rule: " + ruleSection + " action: " + actions));
                    denied = true;
                }
            }
            if (denied) {
                return new ContextDecision(Explanation.Code.REJECTED_DENIED, false, evaluations);
            }
            boolean allowed = false;
            if (ruleSection.containsKey(ALLOW_ACTIONS)) {
                HashSet<String> actions = new HashSet<String>();
                final Object actionsObj = ruleSection.get(ALLOW_ACTIONS);
                if (actionsObj instanceof String) {
                    final String actionStr = (String) actionsObj;
                    actions.add(actionStr);
                } else if (actionsObj instanceof List) {
                    actions.addAll((List<String>) actionsObj);
                } else {
                    evaluations.add(new ContextEvaluation(Explanation.Code.REJECTED_CONTEXT_EVALUATION_ERROR,
                        "Invalid action type."));
                }
                if (0 == actions.size()) {
                    //xxx:warn
                } else if (actions.contains("*") || actions.contains(action)) {
                    evaluations.add(new ContextEvaluation(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED,
                        super.toString() + ": rule: " + ruleSection + " action: " + actions));
                    allowed = true;
                }
            }

            if (allowed) {
                return new ContextDecision(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, true, evaluations);
            } else {
                return new ContextDecision(Explanation.Code.REJECTED, false, evaluations);
            }
        }

        boolean matchesRuleSections(final Map<String, String> resource, final List<ContextEvaluation> evaluations) {
            int matchesRequired=0;
            int matchesMet=0;
            //evaluate match:
            if (ruleSection.containsKey(MATCH_SECTION)) {
                matchesRequired++;
                boolean matches = ruleMatchesMatchSection(resource, this.ruleSection);
                if (matches) {
                    matchesMet++;
                }else {
                    evaluations.add(new ContextEvaluation(Explanation.Code.REJECTED, MATCH_SECTION+" section did not match"));
                }
            }
            //evaluate equals:
            if (ruleSection.containsKey(EQUALS_SECTION)) {
                matchesRequired++;
                boolean matches = ruleMatchesEqualsSection(resource, this.ruleSection);
                if (matches) {
                    matchesMet++;
                } else {
                    evaluations.add(new ContextEvaluation(Explanation.Code.REJECTED, EQUALS_SECTION+" section did not match"));
                }
            }

            //evaluate contains:
            if (ruleSection.containsKey(CONTAINS_SECTION)) {
                matchesRequired++;
                boolean matches = ruleMatchesContainsSection(resource, this.ruleSection);
                if (matches) {
                    matchesMet++;
                } else {
                    evaluations.add(new ContextEvaluation(Explanation.Code.REJECTED, CONTAINS_SECTION+" section did not match"));
                }
            }
            return matchesMet == matchesRequired && matchesRequired > 0;
        }

        boolean ruleMatchesContainsSection(Map<String, String> resource, final Map ruleSection) {
            Map section = (Map) ruleSection.get(CONTAINS_SECTION);
            return predicateMatch(section, resource, true, new Transformer() {
                public Object transform(Object o) {
                    return new SetContainsPredicate(o);
                }
            });
        }

        boolean ruleMatchesEqualsSection(Map<String, String> resource, final Map ruleSection) {
            Map section = (Map) ruleSection.get(EQUALS_SECTION);
            return predicateMatch(section, resource, false, new Transformer() {
                public Object transform(Object o) {
                    return PredicateUtils.equalPredicate(o);
                }
            });
        }

        boolean ruleMatchesMatchSection(Map<String, String> resource, final Map ruleSection) {
            Map section = (Map) ruleSection.get(MATCH_SECTION);
            return predicateMatch(section, resource, true, new Transformer() {
                public Object transform(Object o) {
                    return new RegexPredicate(patternForRegex((String) o));
                }
            });
        }

        private boolean predicateMatch(Map match, Map<String, String> resource, final boolean allowListMatch,
                                       Transformer predicateTransformer) {
            for (final Object o : match.entrySet()) {
                Map.Entry entry = (Map.Entry) o;
                final String key = (String) entry.getKey();
                final Object test = entry.getValue();

                boolean matched = applyTest(resource, allowListMatch, predicateTransformer, key, test);
                if(!matched){
                    return false;
                }
            }
            return true;
        }

        private boolean applyTest(Map<String, String> resource, boolean allowListMatch, Transformer predicateTransformer,
                               String key, Object test) {
            boolean matched = false;

            ArrayList tests = new ArrayList();
            if (allowListMatch && test instanceof List) {
                //must match all values
                for (final Object item : (List) test) {
                    tests.add(predicateTransformer.transform(item));
                }
            } else if (test instanceof String) {
                //match single test
                tests.add(predicateTransformer.transform(test));
            } else {
                //xxx:warn
                //unexpected format, do not match
                matched = false;
            }

            if (!PredicateUtils.allPredicate(tests).evaluate(resource.get(key))) {
                matched = false;
            }else{
                matched=true;
            }
            return matched;
        }

    }

    /**
     * evaluates to true if the input matches a regular expression
     */
    static class RegexPredicate implements Predicate {
        Pattern regex;

        RegexPredicate(final Pattern regex) {
            this.regex = regex;
        }

        public boolean evaluate(final Object o) {
            return o instanceof String && regex.matcher((String) o).matches();
        }

    }

    /**
     * Evaluates to true if the input is a string or collection of strings, and they are a superset of this object's
     * collection.
     */
    static class SetContainsPredicate implements Predicate {
        HashSet<String> items = new HashSet<String>();

        SetContainsPredicate(Object item) {
            if (item instanceof String) {
                items.add((String) item);
            } else if (item instanceof List) {
                items.addAll((List<String>) item);
            } else {
                //xxx:warn
                //unexpected, will reject everything
                items = null;
            }
        }

        public boolean evaluate(Object o) {
            if (null == items || null == o) {
                return false;
            }
            Collection input;
            if (o instanceof String) {
                HashSet<String> hs = new HashSet<String>();
                //treat o as comma-seperated list of strings
                String str = (String) o;
                final String[] split = str.split(",");
                for (final String s : split) {
                    hs.add(s.trim());
                }
                input = hs;
            } else if (o instanceof Collection) {
                input = (Collection) o;
            } else {
                return false;
            }
            return CollectionUtils.isSubCollection(items, input);
        }
    }

    /**
     * Makes a decision for a job resource based on the "rules: " section
     */
    class LegacyRulesContext implements AclContext {
        private final Map rules;

        private ConcurrentHashMap<String, Pattern> patternCache = new ConcurrentHashMap<String, Pattern>();
        AclContext aclContext;

        public LegacyRulesContext(Map rules) {
            this.rules = rules;
        }

        private boolean regexMatches(final String regex, final String value) {
            if (!patternCache.containsKey(regex)) {
                patternCache.putIfAbsent(regex, Pattern.compile(regex));
            }
            Pattern pattern = patternCache.get(regex);
            Matcher matcher = pattern.matcher(value);
            return matcher.matches();
        }

        public ContextDecision includes(Map<String, String> resourceMap, String action) {
            String resource = createLegacyJobResourcePath(resourceMap);
            List<ContextEvaluation> evaluations = new ArrayList<ContextEvaluation>();
            Set<Map.Entry> entries = rules.entrySet();
            for (Map.Entry entry : entries) {
                Object ruleKey = entry.getKey();
                if (!(ruleKey instanceof String)) {
                    evaluations.add(new ContextEvaluation(Explanation.Code.REJECTED_CONTEXT_EVALUATION_ERROR,
                        "Invalid key type: " + ruleKey.getClass().getName()));
                    continue;
                }

                String rule = (String) ruleKey;
                if (rule == null || rule.length() == 0) {
                    evaluations.add(new ContextEvaluation(Explanation.Code.REJECTED_CONTEXT_EVALUATION_ERROR,
                        "Resource is empty or null"));
                }

                if (regexMatches(rule, resource)) {
                    Map ruleMap = (Map) entry.getValue();
                    Object actionsKey = ruleMap.get(ACTIONS_SECTION);
                    if (actionsKey == null) {
                        evaluations.add(new ContextEvaluation(Explanation.Code.REJECTED_ACTIONS_DECLARED_EMPTY,
                            "No actions configured"));
                        continue;
                    }

                    if (actionsKey instanceof String) {
                        String actions = (String) actionsKey;
                        if ("*".equals(actions) || actions.contains(action)) {
                            evaluations.add(new ContextEvaluation(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED,
                                super.toString() + ": rule: " + rule + " action: " + actions));
                            return new ContextDecision(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, true,
                                evaluations);
                        }
                    } else if (actionsKey instanceof List) {
                        List actions = (List) actionsKey;
                        if (actions.contains(action)) {
                            evaluations.add(new ContextEvaluation(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED,
                                super.toString() + ": rule: " + rule + " action: " + actions));
                            return new ContextDecision(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, true,
                                evaluations);
                        }
                    } else {
                        evaluations.add(new ContextEvaluation(Explanation.Code.REJECTED_CONTEXT_EVALUATION_ERROR,
                            "Invalid action type."));

                    }

                    evaluations.add(new ContextEvaluation(Explanation.Code.REJECTED_NO_ACTIONS_MATCHED,
                        "No actions matched"));
                }
            }
            return new ContextDecision(Explanation.Code.REJECTED, false, evaluations);
        }
    }

    /**
     *
     */
    class YamlAclContext implements AclContext {
        private String description = "Not Evaluated: " + super.toString();
        Map policyDef;

        YamlAclContext(final Map policyDef) {
            this.policyDef = policyDef;
        }

        public String toString() {
            return "Context: " + description;
        }

        @SuppressWarnings ("rawtypes")
        public ContextDecision includes(Map<String, String> resourceMap, String action) {
            List<ContextEvaluation> evaluations = new ArrayList<ContextEvaluation>();
            policyDef = policyInput;
            Object descriptionValue = policyDef.get("description");
            if (descriptionValue == null || !(descriptionValue instanceof String)) {
                evaluations.add(new ContextEvaluation(Explanation.Code.REJECTED_NO_DESCRIPTION_PROVIDED,
                    "Policy is missing a description."));
                return new ContextDecision(Explanation.Code.REJECTED_NO_DESCRIPTION_PROVIDED, false, evaluations);
            }
            description = (String) descriptionValue;

            String type = resourceMap.get(TYPE_PROPERTY);
            if (null == type) {
                evaluations.add(new ContextEvaluation(Explanation.Code.REJECTED_NO_RESOURCE_TYPE,
                    "Resource has no '" + TYPE_PROPERTY + "'."));
                return new ContextDecision(Explanation.Code.REJECTED_NO_RESOURCE_TYPE, false, evaluations);
            }
            Object forMap = policyDef.get(FOR_SECTION);
            if (null != forMap && !(forMap instanceof Map)) {
                evaluations.add(new ContextEvaluation(Explanation.Code.REJECTED_INVALID_FOR_SECTION,
                    "'" + FOR_SECTION + "' was not declared"));
                return new ContextDecision(Explanation.Code.REJECTED_INVALID_FOR_SECTION, false, evaluations);
            }

            Map forsection = (Map) forMap;
            final Object typeMap = null != forsection ? forsection.get(type) : null;

            final boolean useLegacyRules = JOB_TYPE.equals(type) && policyDef.containsKey(
                RULES_SECTION)
                                           && policyDef.get(
                RULES_SECTION) instanceof Map;

            if (null == typeContexts.get(type)) {
                if (null != typeMap) {
                    typeContexts.putIfAbsent(type, createTypeContext((List) typeMap));
                } else if (useLegacyRules) {
                    Object rulesValue = policyDef.get(RULES_SECTION);
                    Map rules = (Map) rulesValue;
                    typeContexts.putIfAbsent(type, createLegacyContext(rules));
                } else {
                    evaluations.add(new ContextEvaluation(Explanation.Code.REJECTED_NO_RULES_DECLARED,
                        "Section for type '" + type + "' was not declared in " + FOR_SECTION + " section"));
                    return new ContextDecision(Explanation.Code.REJECTED_NO_RULES_DECLARED, false, evaluations);
                }
            }

            //evaluate resource given the type specific matcher

            final AclContext typeContext = typeContexts.get(type);
            return typeContext.includes(resourceMap, action);

        }


    }
}
