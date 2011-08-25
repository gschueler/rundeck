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
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.PredicateUtils;
import org.apache.commons.collections.Transformer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
* YamlPolicy is ...
*
* @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
*/
final class YamlPolicy implements Policy {

    public static final String TYPE_PROPERTY = "type";
    public static final String FOR_SECTION = "for";
    public static final String JOB_TYPE = "job";
    public static final String RULES_SECTION = "rules";
    public static final String ACTIONS_SECTION = "actions";
    public Map rawInput;

    private Set<String> usernames = new HashSet<String>();
    private Set<Object> groups = new HashSet<Object>();
    private static ConcurrentHashMap<String, Pattern> patternCache = new ConcurrentHashMap<String, Pattern>();
    AclContext aclContext;
    private final ConcurrentHashMap<String, AclContext> typeContexts = new ConcurrentHashMap<String, AclContext>();

    public YamlPolicy(final Object yamlDoc) {
        rawInput = (Map) yamlDoc;
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
        aclContext = new AclContext() {
            private String description = "Not Evaluated: " + super.toString();

            public String toString() {
                return "Context: " + description;
            }

            @SuppressWarnings ("rawtypes")
            public ContextDecision includes(Map<String, String> resourceMap, String action) {
                List<ContextEvaluation> evaluations = new ArrayList<ContextEvaluation>();
                Object descriptionValue = rawInput.get("description");
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
                Object forMap = rawInput.get(FOR_SECTION);
                if (null != forMap && !(forMap instanceof Map)) {
                    evaluations.add(new ContextEvaluation(Explanation.Code.REJECTED_INVALID_FOR_SECTION,
                        "'" + FOR_SECTION + "' was not declared"));
                    return new ContextDecision(Explanation.Code.REJECTED_INVALID_FOR_SECTION, false, evaluations);
                }

                Map forsection = (Map) forMap;
                final Object typeMap = null != forsection ? forsection.get(type) : null;

                final boolean useLegacyRules = JOB_TYPE.equals(type) && rawInput.containsKey(
                    RULES_SECTION)
                                               && rawInput.get(
                    RULES_SECTION) instanceof Map;

                if (null == typeContexts.get(type)) {
                    if (null != typeMap) {
                        typeContexts.putIfAbsent(type, createTypeContext((List) typeMap));
                    } else if (useLegacyRules) {
                        Object rulesValue = rawInput.get(RULES_SECTION);
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
                final ContextDecision includes = typeContext.includes(resourceMap, action);
                return includes;

            }


        };
    }

    private boolean regexMatches(final String regex, final String value) {
        if (!patternCache.containsKey(regex)) {
            patternCache.putIfAbsent(regex, Pattern.compile(regex));
        }
        Pattern pattern = patternCache.get(regex);
        Matcher matcher = pattern.matcher(value);
        return matcher.matches();
    }

    private AclContext createLegacyContext(final Map rules) {
        return new AclContext() {
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
        };

    }

    private AclContext createTypeContext(final List typeSection) {
        final List<AclContext> typeRules = createTypeRules(typeSection);

        return new AclContext() {
            public ContextDecision includes(final Map<String, String> resource, final String action) {
                ArrayList<ContextEvaluation> evaluations = new ArrayList<ContextEvaluation>();
                boolean allowed = false;
                boolean denied = false;
                ContextEvaluation deniedEvaluation;
                for (final AclContext evaluator : typeRules) {
                    final ContextDecision includes = evaluator.includes(resource, action);
                    if (null == includes) {
                        //indicates the section did not match
                        continue;
                    }
                    if (includes.granted()) {
                        allowed = true;
                    }
                    evaluations.addAll(includes.getEvaluations());
                    if (!denied) {
                        for (final ContextEvaluation contextEvaluation : includes.getEvaluations()) {
                            if (contextEvaluation.id == Explanation.Code.REJECTED_DENIED) {
                                deniedEvaluation = contextEvaluation;
                                denied = true;
                            }
                        }
                    }
                }
                return new ContextDecision(denied ? Explanation.Code.REJECTED_DENIED : allowed ? Explanation.Code.GRANTED : Explanation.Code.REJECTED,
                    allowed && !denied, evaluations);

            }
        };
    }

    private List<AclContext> createTypeRules(List typeSection) {
        ArrayList<AclContext> evaluators = new ArrayList<AclContext>();
        for (final Object o : typeSection) {
            Map section = (Map) o;
            evaluators.add(createTypeRuleContext(section));
        }
        return evaluators;
    }

    /**
     * Create acl context  for specific rule in a type context
     */
    private AclContext createTypeRuleContext(Map section) {
        return new TypeRuleContext(section);
    }

    private static String createLegacyJobResourcePath(Map<String, String> resource) {
        return resource.get("group") + "/" + resource.get("job");
    }

    /**
     * parse the by: clause.
     */
    private void parseByClause() {
        Object byClause = rawInput.get("by");
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
        sb.append(rawInput.get("id")).append(", groups:");
        for (Object group : getGroups()) {
            sb.append(group.toString()).append(" ");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * AclContext implementation is responsible for a single match and allow/deny result.
     */
    private class TypeRuleContext implements AclContext {
        public static final String MATCH_SECTION = "match";
        public static final String EQUALS_SECTION = "equals";
        public static final String CONTAINS_SECTION = "contains";
        public static final String ALLOW_ACTIONS = "allow";
        public static final String DENY_ACTIONS = "deny";
        Map ruleSection;

        private TypeRuleContext(Map ruleSection) {
            this.ruleSection = ruleSection;
        }

        public ContextDecision includes(Map<String, String> resource, String action) {
            boolean matched = true;
            List<ContextEvaluation> evaluations = new ArrayList<ContextEvaluation>();

            //evaluate match:
            if (ruleSection.containsKey(MATCH_SECTION)) {
                Map section = (Map) this.ruleSection.get(MATCH_SECTION);
                boolean matches = predicateMatch(section, resource, new Transformer() {
                    public Object transform(Object o) {
                        return new RegexPredicate((String) o);
                    }
                });
                if (!matches) {
                    matched = false;
                }
            }
            //evaluate equals:
            if (ruleSection.containsKey(EQUALS_SECTION)) {
                Map section = (Map) this.ruleSection.get(EQUALS_SECTION);
                boolean matches = predicateMatch(section, resource, new Transformer() {
                    public Object transform(Object o) {
                        return PredicateUtils.equalPredicate(o);
                    }
                });
                if (!matches) {
                    matched = false;
                }
            }

            //evaluate contains:
            if (ruleSection.containsKey(CONTAINS_SECTION)) {
                Map section = (Map) this.ruleSection.get(CONTAINS_SECTION);
                boolean matches = predicateMatch(section, resource, new Transformer() {
                    public Object transform(Object o) {
                        return new SetContainsPredicate((String) o);
                    }
                });
                if (!matches) {
                    matched = false;
                }
            }

            if (!matched) {
                return null;
            }

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
                return null;
            }
        }

        private boolean predicateMatch(Map match, Map<String, String> resource, Transformer predicateTransformer) {
            boolean matched = true;
            //
            for (final Object o : match.entrySet()) {
                Map.Entry entry = (Map.Entry) o;
                final String key = (String) entry.getKey();
                final Object test = entry.getValue();

                if (null == resource.get(key)) {
                    continue;
                }
                ArrayList tests = new ArrayList();
                if (test instanceof List) {
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
                    break;
                }

                if (!PredicateUtils.allPredicate(tests).evaluate(resource.get(key))) {
                    matched = false;
                    break;
                }
            }
            return matched;
        }

    }

    /**
     * evaluates to true if the input matches a regular expression
     */
    class RegexPredicate implements Predicate {
        String regex;

        RegexPredicate(String regex) {
            this.regex = regex;
        }

        public boolean evaluate(Object o) {
            return regexMatches(regex, (String) o);
        }

    }

    /**
     * Evaluates to true if the input is a string or collection of strings, and they are a superset of this object's
     * collection.
     */
    class SetContainsPredicate implements Predicate {
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
}
