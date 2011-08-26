/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
* TestYamlPolicy.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 8/25/11 11:49 AM
* 
*/
package com.dtolabs.rundeck.core.authorization.providers;

import com.dtolabs.rundeck.core.authorization.Explanation;
import com.dtolabs.rundeck.core.utils.Converter;
import junit.framework.TestCase;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.PredicateUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

/**
 * TestYamlPolicy is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class TestYamlPolicy extends TestCase {
    File testdir;
    File test1;
    File test2;
    Yaml yaml;

    public void setUp() throws Exception {
        testdir = new File("src/test/com/dtolabs/rundeck/core/authorization/providers");
        test1 = new File(testdir, "test1.yaml");
        test2 = new File(testdir, "test2.yaml");
        yaml = new Yaml();
    }

    public void tearDown() throws Exception {

    }

    public void testTypeRuleContextMatcherEvaluateActionsAllow() {
        {
            //no allow or deny should result in REJECTED
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
            );
            assertTrue(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
                ruleSection);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            assertNotNull(decision);
            assertFalse(decision.granted());
            assertEquals(Explanation.Code.REJECTED, decision.getCode());
            assertEquals(0, decision.getEvaluations().size());
        }
        {
            //allow '*' should allow any action 
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "allow: '*'"
            );
            assertTrue(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
                ruleSection);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            assertNotNull(decision);
            assertTrue(decision.granted());
            assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision.getCode());
            assertEquals(1, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("test2", contextEvaluations);
            assertNotNull(decision2);
            assertTrue(decision2.granted());
            assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision2.getCode());
            assertEquals(1, decision2.getEvaluations().size());
        }
        {
            //allow string should only allow exact action
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "allow: 'testaction'"
            );
            assertTrue(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
                ruleSection);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            assertNotNull(decision);
            assertFalse(decision.granted());
            assertEquals(Explanation.Code.REJECTED, decision.getCode());
            assertEquals(0, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("testaction", contextEvaluations);
            assertNotNull(decision2);
            assertTrue(decision2.granted());
            assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision2.getCode());
            assertEquals(1, decision2.getEvaluations().size());
        }
        {
            //allow list should allow any action in the list
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "allow: ['testaction',zah,zee]"
            );
            assertTrue(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
                ruleSection);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            assertNotNull(decision);
            assertFalse(decision.granted());
            assertEquals(Explanation.Code.REJECTED, decision.getCode());
            assertEquals(0, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("testaction", contextEvaluations);
            assertNotNull(decision2);
            assertTrue(decision2.granted());
            assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision2.getCode());
            assertEquals(1, decision2.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision3 = typeRuleContext.evaluateActions("zah", contextEvaluations);
            assertNotNull(decision3);
            assertTrue(decision3.granted());
            assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision3.getCode());
            assertEquals(1, decision3.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision4 = typeRuleContext.evaluateActions("zee", contextEvaluations);
            assertNotNull(decision4);
            assertTrue(decision4.granted());
            assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision4.getCode());
            assertEquals(1, decision4.getEvaluations().size());
        }
        {
            //allow list with '*' will also allow all actions
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "allow: ['*',zah,zee]"
            );
            assertTrue(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
                ruleSection);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            assertNotNull(decision);
            assertTrue(decision.granted());
            assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision.getCode());
            assertEquals(1, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("testaction", contextEvaluations);
            assertNotNull(decision2);
            assertTrue(decision2.granted());
            assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision2.getCode());
            assertEquals(1, decision2.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision3 = typeRuleContext.evaluateActions("zah", contextEvaluations);
            assertNotNull(decision3);
            assertTrue(decision3.granted());
            assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision3.getCode());
            assertEquals(1, decision3.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision4 = typeRuleContext.evaluateActions("zee", contextEvaluations);
            assertNotNull(decision4);
            assertTrue(decision4.granted());
            assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision4.getCode());
            assertEquals(1, decision4.getEvaluations().size());
        }

    }


    public void testTypeRuleContextMatcherEvaluateActionsDeny() {
        {
            //allow '*' should deny any action
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "deny: '*'"
            );
            assertTrue(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
                ruleSection);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            assertNotNull(decision);
            assertFalse(decision.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision.getCode());
            assertEquals(1, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("test2", contextEvaluations);
            assertNotNull(decision2);
            assertFalse(decision2.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision2.getCode());
            assertEquals(1, decision2.getEvaluations().size());
        }
        {
            //deny string should only deny exact action
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "deny: 'testaction'"
            );
            assertTrue(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
                ruleSection);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            assertNotNull(decision);
            assertFalse(decision.granted());
            assertEquals(Explanation.Code.REJECTED, decision.getCode());
            assertEquals(0, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("testaction", contextEvaluations);
            assertNotNull(decision2);
            assertFalse(decision2.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision2.getCode());
            assertEquals(1, decision2.getEvaluations().size());
        }
        {
            //deny list should deny any action in the list
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "deny: ['testaction',zah,zee]"
            );
            assertTrue(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
                ruleSection);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            assertNotNull(decision);
            assertFalse(decision.granted());
            assertEquals(Explanation.Code.REJECTED, decision.getCode());
            assertEquals(0, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("testaction", contextEvaluations);
            assertNotNull(decision2);
            assertFalse(decision2.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision2.getCode());
            assertEquals(1, decision2.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision3 = typeRuleContext.evaluateActions("zah", contextEvaluations);
            assertNotNull(decision3);
            assertFalse(decision3.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision3.getCode());
            assertEquals(1, decision3.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision4 = typeRuleContext.evaluateActions("zee", contextEvaluations);
            assertNotNull(decision4);
            assertFalse(decision4.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision4.getCode());
            assertEquals(1, decision4.getEvaluations().size());
        }
        {
            //deny list with '*' will also deny all actions
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "deny: ['*',zah,zee]"
            );
            assertTrue(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
                ruleSection);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            assertNotNull(decision);
            assertFalse(decision.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision.getCode());
            assertEquals(1, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("testaction", contextEvaluations);
            assertNotNull(decision2);
            assertFalse(decision2.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision2.getCode());
            assertEquals(1, decision2.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision3 = typeRuleContext.evaluateActions("zah", contextEvaluations);
            assertNotNull(decision3);
            assertFalse(decision3.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision3.getCode());
            assertEquals(1, decision3.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision4 = typeRuleContext.evaluateActions("zee", contextEvaluations);
            assertNotNull(decision4);
            assertFalse(decision4.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision4.getCode());
            assertEquals(1, decision4.getEvaluations().size());
        }

    }

    public void testTypeRuleContextMatcherEvaluateActionsCombined() {
        {
            //allow '*' and deny '*' should always deny
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "deny: '*'\n"
                                          + "allow: '*'\n"
            );
            assertTrue(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
                ruleSection);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            assertNotNull(decision);
            assertFalse(decision.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision.getCode());
            assertEquals(1, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("test2", contextEvaluations);
            assertNotNull(decision2);
            assertFalse(decision2.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision2.getCode());
            assertEquals(1, decision2.getEvaluations().size());
        }
        {
            //allow 'X' and deny '*' should always deny
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "deny: '*'\n"
                                          + "allow: 'blah'\n"
            );
            assertTrue(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
                ruleSection);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            assertNotNull(decision);
            assertFalse(decision.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision.getCode());
            assertEquals(1, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("test2", contextEvaluations);
            assertNotNull(decision2);
            assertFalse(decision2.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision2.getCode());
            assertEquals(1, decision2.getEvaluations().size());
        }
        {
            //allow '*' and deny 'X' should only deny X
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "deny: 'blah'\n"
                                          + "allow: '*'\n"
            );
            assertTrue(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
                ruleSection);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            assertNotNull(decision);
            assertFalse(decision.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision.getCode());
            assertEquals(1, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("test2", contextEvaluations);
            assertNotNull(decision2);
            assertTrue(decision2.granted());
            assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision2.getCode());
            assertEquals(1, decision2.getEvaluations().size());
            contextEvaluations.clear();

            ContextDecision decision3 = typeRuleContext.evaluateActions("blah blee", contextEvaluations);
            assertNotNull(decision3);
            assertTrue(decision3.granted());
            assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision3.getCode());
            assertEquals(1, decision3.getEvaluations().size());
        }
        {
            //allow 'X' and deny 'Y' should only deny Y and only allow X
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "deny: 'blah'\n"
                                          + "allow: 'blee'\n"
            );
            assertTrue(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
                ruleSection);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            assertNotNull(decision);
            assertFalse(decision.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision.getCode());
            assertEquals(1, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("test2", contextEvaluations);
            assertNotNull(decision2);
            assertFalse(decision2.granted());
            assertEquals(Explanation.Code.REJECTED, decision2.getCode());
            assertEquals(0, decision2.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision4 = typeRuleContext.evaluateActions("blah blee", contextEvaluations);
            assertNotNull(decision4);
            assertFalse(decision4.granted());
            assertEquals(Explanation.Code.REJECTED, decision4.getCode());
            assertEquals(0, decision4.getEvaluations().size());
            contextEvaluations.clear();

            ContextDecision decision3 = typeRuleContext.evaluateActions("blee", contextEvaluations);
            assertNotNull(decision3);
            assertTrue(decision3.granted());
            assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision3.getCode());
            assertEquals(1, decision3.getEvaluations().size());
        }
        {
            //allow List and deny '*' should deny all
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "deny: '*'\n"
                                          + "allow: [abc,def]\n"
            );
            assertTrue(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
                ruleSection);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            assertNotNull(decision);
            assertFalse(decision.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision.getCode());
            assertEquals(1, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("abc", contextEvaluations);
            assertNotNull(decision2);
            assertFalse(decision2.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision2.getCode());
            assertEquals(1, decision2.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision4 = typeRuleContext.evaluateActions("def", contextEvaluations);
            assertNotNull(decision4);
            assertFalse(decision4.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision4.getCode());
            assertEquals(1, decision4.getEvaluations().size());
            contextEvaluations.clear();

            ContextDecision decision3 = typeRuleContext.evaluateActions("blee", contextEvaluations);
            assertNotNull(decision3);
            assertFalse(decision3.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision3.getCode());
            assertEquals(1, decision3.getEvaluations().size());
        }
        {
            //allow List with '*' and deny '*' should deny all
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "deny: '*'\n"
                                          + "allow: ['*',abc,def]\n"
            );
            assertTrue(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
                ruleSection);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            assertNotNull(decision);
            assertFalse(decision.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision.getCode());
            assertEquals(1, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("abc", contextEvaluations);
            assertNotNull(decision2);
            assertFalse(decision2.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision2.getCode());
            assertEquals(1, decision2.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision4 = typeRuleContext.evaluateActions("def", contextEvaluations);
            assertNotNull(decision4);
            assertFalse(decision4.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision4.getCode());
            assertEquals(1, decision4.getEvaluations().size());
            contextEvaluations.clear();

            ContextDecision decision3 = typeRuleContext.evaluateActions("blee", contextEvaluations);
            assertNotNull(decision3);
            assertFalse(decision3.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision3.getCode());
            assertEquals(1, decision3.getEvaluations().size());
        }
        {
            //allow List with '*' and deny 'X' should deny X only
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "deny: 'zam'\n"
                                          + "allow: ['*',abc,def]\n"
            );
            assertTrue(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
                ruleSection);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            assertNotNull(decision);
            assertTrue(decision.granted());
            assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision.getCode());
            assertEquals(1, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("abc", contextEvaluations);
            assertNotNull(decision2);
            assertTrue(decision2.granted());
            assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision2.getCode());
            assertEquals(1, decision2.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision4 = typeRuleContext.evaluateActions("def", contextEvaluations);
            assertNotNull(decision4);
            assertTrue(decision4.granted());
            assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision4.getCode());
            assertEquals(1, decision4.getEvaluations().size());
            contextEvaluations.clear();

            ContextDecision decision3 = typeRuleContext.evaluateActions("zam", contextEvaluations);
            assertNotNull(decision3);
            assertFalse(decision3.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision3.getCode());
            assertEquals(1, decision3.getEvaluations().size());
        }
        {
            //allow List and deny 'X' should deny X only, and allow list only
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "deny: 'zam'\n"
                                          + "allow: ['ghi',abc,def]\n"
            );
            assertTrue(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
                ruleSection);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            assertNotNull(decision);
            assertFalse(decision.granted());
            assertEquals(Explanation.Code.REJECTED, decision.getCode());
            assertEquals(0, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("abc", contextEvaluations);
            assertNotNull(decision2);
            assertTrue(decision2.granted());
            assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision2.getCode());
            assertEquals(1, decision2.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision4 = typeRuleContext.evaluateActions("def", contextEvaluations);
            assertNotNull(decision4);
            assertTrue(decision4.granted());
            assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision4.getCode());
            assertEquals(1, decision4.getEvaluations().size());
            contextEvaluations.clear();


            contextEvaluations.clear();
            ContextDecision decision5 = typeRuleContext.evaluateActions("ghi", contextEvaluations);
            assertNotNull(decision5);
            assertTrue(decision5.granted());
            assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision5.getCode());
            assertEquals(1, decision5.getEvaluations().size());
            contextEvaluations.clear();

            ContextDecision decision3 = typeRuleContext.evaluateActions("zam", contextEvaluations);
            assertNotNull(decision3);
            assertFalse(decision3.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision3.getCode());
            assertEquals(1, decision3.getEvaluations().size());
        }
        {
            //allow List and deny List should deny list only, and allow list only
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "deny: ['zam','zee']\n"
                                          + "allow: ['ghi',abc,def]\n"
            );
            assertTrue(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
                ruleSection);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            assertNotNull(decision);
            assertFalse(decision.granted());
            assertEquals(Explanation.Code.REJECTED, decision.getCode());
            assertEquals(0, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("abc", contextEvaluations);
            assertNotNull(decision2);
            assertTrue(decision2.granted());
            assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision2.getCode());
            assertEquals(1, decision2.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision4 = typeRuleContext.evaluateActions("def", contextEvaluations);
            assertNotNull(decision4);
            assertTrue(decision4.granted());
            assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision4.getCode());
            assertEquals(1, decision4.getEvaluations().size());
            contextEvaluations.clear();


            contextEvaluations.clear();
            ContextDecision decision5 = typeRuleContext.evaluateActions("ghi", contextEvaluations);
            assertNotNull(decision5);
            assertTrue(decision5.granted());
            assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision5.getCode());
            assertEquals(1, decision5.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision3 = typeRuleContext.evaluateActions("zam", contextEvaluations);
            assertNotNull(decision3);
            assertFalse(decision3.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision3.getCode());
            assertEquals(1, decision3.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision6 = typeRuleContext.evaluateActions("zee", contextEvaluations);
            assertNotNull(decision6);
            assertFalse(decision6.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision6.getCode());
            assertEquals(1, decision6.getEvaluations().size());
        }
        {
            //allow List and deny List with * should deny all
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "deny: ['*','zee']\n"
                                          + "allow: ['ghi',abc,def]\n"
            );
            assertTrue(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
                ruleSection);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            assertNotNull(decision);
            assertFalse(decision.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision.getCode());
            assertEquals(1, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("abc", contextEvaluations);
            assertNotNull(decision2);
            assertFalse(decision2.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision2.getCode());
            assertEquals(1, decision2.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision4 = typeRuleContext.evaluateActions("def", contextEvaluations);
            assertNotNull(decision4);
            assertFalse(decision4.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision4.getCode());
            assertEquals(1, decision4.getEvaluations().size());
            contextEvaluations.clear();


            contextEvaluations.clear();
            ContextDecision decision5 = typeRuleContext.evaluateActions("ghi", contextEvaluations);
            assertNotNull(decision5);
            assertFalse(decision5.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision5.getCode());
            assertEquals(1, decision5.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision3 = typeRuleContext.evaluateActions("zam", contextEvaluations);
            assertNotNull(decision3);
            assertFalse(decision3.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision3.getCode());
            assertEquals(1, decision3.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision6 = typeRuleContext.evaluateActions("zee", contextEvaluations);
            assertNotNull(decision6);
            assertFalse(decision6.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision6.getCode());
            assertEquals(1, decision6.getEvaluations().size());
        }
        {
            //allow List with * and deny List with * should deny all
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "deny: ['*','zee']\n"
                                          + "allow: ['*',abc,def]\n"
            );
            assertTrue(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
                ruleSection);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            assertNotNull(decision);
            assertFalse(decision.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision.getCode());
            assertEquals(1, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("abc", contextEvaluations);
            assertNotNull(decision2);
            assertFalse(decision2.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision2.getCode());
            assertEquals(1, decision2.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision4 = typeRuleContext.evaluateActions("def", contextEvaluations);
            assertNotNull(decision4);
            assertFalse(decision4.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision4.getCode());
            assertEquals(1, decision4.getEvaluations().size());
            contextEvaluations.clear();


            contextEvaluations.clear();
            ContextDecision decision5 = typeRuleContext.evaluateActions("ghi", contextEvaluations);
            assertNotNull(decision5);
            assertFalse(decision5.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision5.getCode());
            assertEquals(1, decision5.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision3 = typeRuleContext.evaluateActions("zam", contextEvaluations);
            assertNotNull(decision3);
            assertFalse(decision3.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision3.getCode());
            assertEquals(1, decision3.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision6 = typeRuleContext.evaluateActions("zee", contextEvaluations);
            assertNotNull(decision6);
            assertFalse(decision6.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision6.getCode());
            assertEquals(1, decision6.getEvaluations().size());
        }
        {
            //allow X and deny List should deny only in list, grant only X
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "deny: ['zam','zee']\n"
                                          + "allow: abc\n"
            );
            assertTrue(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
                ruleSection);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            assertNotNull(decision);
            assertFalse(decision.granted());
            assertEquals(Explanation.Code.REJECTED, decision.getCode());
            assertEquals(0, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("abc", contextEvaluations);
            assertNotNull(decision2);
            assertTrue(decision2.granted());
            assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision2.getCode());
            assertEquals(1, decision2.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision4 = typeRuleContext.evaluateActions("def", contextEvaluations);
            assertNotNull(decision4);
            assertFalse(decision4.granted());
            assertEquals(Explanation.Code.REJECTED, decision4.getCode());
            assertEquals(0, decision4.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision3 = typeRuleContext.evaluateActions("zam", contextEvaluations);
            assertNotNull(decision3);
            assertFalse(decision3.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision3.getCode());
            assertEquals(1, decision3.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision6 = typeRuleContext.evaluateActions("zee", contextEvaluations);
            assertNotNull(decision6);
            assertFalse(decision6.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision6.getCode());
            assertEquals(1, decision6.getEvaluations().size());
        }
        {
            //allow X and deny List with * should deny all
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "deny: ['*','zee']\n"
                                          + "allow: abc\n"
            );
            assertTrue(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
                ruleSection);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            assertNotNull(decision);
            assertFalse(decision.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision.getCode());
            assertEquals(1, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("abc", contextEvaluations);
            assertNotNull(decision2);
            assertFalse(decision2.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision2.getCode());
            assertEquals(1, decision2.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision4 = typeRuleContext.evaluateActions("def", contextEvaluations);
            assertNotNull(decision4);
            assertFalse(decision4.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision4.getCode());
            assertEquals(1, decision4.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision3 = typeRuleContext.evaluateActions("zam", contextEvaluations);
            assertNotNull(decision3);
            assertFalse(decision3.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision3.getCode());
            assertEquals(1, decision3.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision6 = typeRuleContext.evaluateActions("zee", contextEvaluations);
            assertNotNull(decision6);
            assertFalse(decision6.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision6.getCode());
            assertEquals(1, decision6.getEvaluations().size());
        }
        {
            //allow '*' and deny List with * should deny all
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "deny: ['*','zee']\n"
                                          + "allow: '*'\n"
            );
            assertTrue(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
                ruleSection);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            assertNotNull(decision);
            assertFalse(decision.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision.getCode());
            assertEquals(1, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("abc", contextEvaluations);
            assertNotNull(decision2);
            assertFalse(decision2.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision2.getCode());
            assertEquals(1, decision2.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision4 = typeRuleContext.evaluateActions("def", contextEvaluations);
            assertNotNull(decision4);
            assertFalse(decision4.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision4.getCode());
            assertEquals(1, decision4.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision3 = typeRuleContext.evaluateActions("zam", contextEvaluations);
            assertNotNull(decision3);
            assertFalse(decision3.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision3.getCode());
            assertEquals(1, decision3.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision6 = typeRuleContext.evaluateActions("zee", contextEvaluations);
            assertNotNull(decision6);
            assertFalse(decision6.granted());
            assertEquals(Explanation.Code.REJECTED_DENIED, decision6.getCode());
            assertEquals(1, decision6.getEvaluations().size());
        }
    }

    public void testApplyTest() {
        {
            //match any resource with name=~ blah, and allow all actions
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "allow: '*'");
            assertTrue(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
                ruleSection);
            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            Converter<String, Predicate> test1 = new Converter<String, Predicate>() {
                public Predicate convert(String s) {
                    return PredicateUtils.equalPredicate(s);
                }
            };

            //test single value predicate value is returned

            assertTrue(typeRuleContext.applyTest(resmap, false, test1, "name", "blah"));
            assertFalse(typeRuleContext.applyTest(resmap, false, test1, "name", "blee"));
            assertFalse(typeRuleContext.applyTest(resmap, false, test1, "name", new ArrayList()));
            assertFalse(typeRuleContext.applyTest(resmap, false, test1, "name", Arrays.asList("blah")));
            assertFalse(typeRuleContext.applyTest(resmap, false, test1, "name", Arrays.asList("blah", "blah")));
            assertFalse(typeRuleContext.applyTest(resmap, false, test1, "name", new Object()));

            //test multivalue predicate value is AND result
            assertTrue(typeRuleContext.applyTest(resmap, true, test1, "name", Arrays.asList("blah")));
            assertTrue(typeRuleContext.applyTest(resmap, true, test1, "name", Arrays.asList("blah", "blah")));
            assertFalse(typeRuleContext.applyTest(resmap, true, test1, "name", Arrays.asList("blah", "blee")));
            assertFalse(typeRuleContext.applyTest(resmap, true, test1, "name", Arrays.asList("blee", "blah")));
            assertFalse(typeRuleContext.applyTest(resmap, true, test1, "name", Arrays.asList("blee", "blee")));


        }
    }

    public void testPredicateMatchRules() {
        //match any resource with name=~ blah, and allow all actions
        final Object load = yaml.load("match: \n"
                                      + "  name: '.*blah.*'\n"
                                      + "allow: '*'");
        assertTrue(load instanceof Map);
        final Map ruleSection = (Map) load;
        final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
            ruleSection);
        final HashMap<String, String> resmap = new HashMap<String, String>();
        resmap.put("name", "blah");
        resmap.put("king", "true");
        resmap.put("wave", "bland");

        Converter<String, Predicate> test1 = new Converter<String, Predicate>() {
            public Predicate convert(String s) {
                return PredicateUtils.equalPredicate(s);
            }
        };
        HashMap rules = new HashMap();

        //test empty rules
        assertTrue(typeRuleContext.predicateMatchRules(rules, resmap, false, test1));
        assertTrue(typeRuleContext.predicateMatchRules(rules, resmap, true, test1));

        //set rules, match false
        rules.put("name", "bloo");
        assertFalse(typeRuleContext.predicateMatchRules(rules, resmap, false, test1));
        assertFalse(typeRuleContext.predicateMatchRules(rules, resmap, true, test1));

        //set rules,  match true
        rules.put("name", "blah");
        assertTrue(typeRuleContext.predicateMatchRules(rules, resmap, false, test1));
        assertTrue(typeRuleContext.predicateMatchRules(rules, resmap, true, test1));

        //set rules,  match all, false
        rules.put("name", "blah");
        rules.put("king", "false");
        assertFalse(typeRuleContext.predicateMatchRules(rules, resmap, false, test1));
        assertFalse(typeRuleContext.predicateMatchRules(rules, resmap, true, test1));

        //set rules,  match all, true
        rules.put("name", "blah");
        rules.put("king", "true");
        assertTrue(typeRuleContext.predicateMatchRules(rules, resmap, false, test1));
        assertTrue(typeRuleContext.predicateMatchRules(rules, resmap, true, test1));

        //set rules,  match all, false
        rules.put("name", "blah");
        rules.put("king", "true");
        rules.put("wave", "bloo");
        assertFalse(typeRuleContext.predicateMatchRules(rules, resmap, false, test1));
        assertFalse(typeRuleContext.predicateMatchRules(rules, resmap, true, test1));

        //set rules,  match all, true
        rules.put("name", "blah");
        rules.put("king", "true");
        rules.put("wave", "bland");
        assertTrue(typeRuleContext.predicateMatchRules(rules, resmap, false, test1));
        assertTrue(typeRuleContext.predicateMatchRules(rules, resmap, true, test1));

        //set rules,  additional rules match false
        rules.put("name", "blah");
        rules.put("king", "true");
        rules.put("wave", "bland");
        rules.put("another", "blee");
        assertFalse(typeRuleContext.predicateMatchRules(rules, resmap, false, test1));
        assertFalse(typeRuleContext.predicateMatchRules(rules, resmap, true, test1));

    }

    public void testTypeRuleContextMatcherMatchRule() {
        {
            //match any resource with name=~ blah, and allow all actions
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "allow: '*'");
            assertTrue(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
                ruleSection);

            final HashMap<String, String> resmap = new HashMap<String, String>();

            //false result for no match
            assertFalse(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));

            resmap.put("name", "something");
            assertFalse(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));
            resmap.put("name", "blah");
            assertTrue(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));
            resmap.put("name", "ablahz");
            assertTrue(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));

        }
        {
            //multiple regexes must all match
            final Object load = yaml.load("match: \n"
                                          + "  name: ['.*blah.*','.*nada.*']\n"
                                          + "allow: '*'");
            assertTrue(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
                ruleSection);

            final HashMap<String, String> resmap = new HashMap<String, String>();

            //false result for no match
            assertFalse(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));

            resmap.put("name", "something");
            assertFalse(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));
            resmap.put("name", "blah");
            assertFalse(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));
            resmap.put("name", "ablahz");
            assertFalse(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));
            resmap.put("name", "nada");
            assertFalse(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));
            resmap.put("name", "ablahz nada");
            assertTrue(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));

        }
        {
            //multiple attributes must all match
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "  something: '.*else.*'\n"
                                          + "allow: '*'");
            assertTrue(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
                ruleSection);

            final HashMap<String, String> resmap = new HashMap<String, String>();

            //false result for no match
            assertFalse(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));

            resmap.put("name", "something");
            assertFalse(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));
            resmap.put("name", "blah");
            assertFalse(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));
            resmap.put("name", "ablahz");
            assertFalse(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));


            resmap.put("something", "els");
            assertFalse(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));
            resmap.put("something", "else");
            assertTrue(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));
            resmap.put("something", "bloo else zaaf");
            assertTrue(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));

            resmap.put("name", "naba");
            assertFalse(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));

            resmap.remove("name");
            assertFalse(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));

        }
    }

    public void testTypeRuleContextMatcherEqualsRule() {
        {
            //equality for single attribute 'name'
            final Object load = yaml.load("equals: \n"
                                          + "  name: blah\n"
                                          + "allow: '*'");
            assertTrue(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
                ruleSection);

            final HashMap<String, String> resmap = new HashMap<String, String>();

            //false result for no match
            assertFalse(typeRuleContext.ruleMatchesEqualsSection(resmap, ruleSection));

            resmap.put("name", "something");
            assertFalse(typeRuleContext.ruleMatchesEqualsSection(resmap, ruleSection));
            resmap.put("name", "blah");
            assertTrue(typeRuleContext.ruleMatchesEqualsSection(resmap, ruleSection));
            resmap.put("name", "ablahz");
            assertFalse(typeRuleContext.ruleMatchesEqualsSection(resmap, ruleSection));
        }
        {
            //equality for multiple attributes
            final Object load = yaml.load("equals: \n"
                                          + "  name: blah\n"
                                          + "  something: zelse\n"
                                          + "allow: '*'");
            assertTrue(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
                ruleSection);

            final HashMap<String, String> resmap = new HashMap<String, String>();

            //false result for no match
            assertFalse(typeRuleContext.ruleMatchesEqualsSection(resmap, ruleSection));

            resmap.put("name", "something");
            assertFalse(typeRuleContext.ruleMatchesEqualsSection(resmap, ruleSection));
            resmap.put("name", "ablahz");
            assertFalse(typeRuleContext.ruleMatchesEqualsSection(resmap, ruleSection));
            resmap.put("name", "blah");
            assertFalse(typeRuleContext.ruleMatchesEqualsSection(resmap, ruleSection));

            resmap.put("something", "else");
            assertFalse(typeRuleContext.ruleMatchesEqualsSection(resmap, ruleSection));
            resmap.put("something", "zelse");
            assertTrue(typeRuleContext.ruleMatchesEqualsSection(resmap, ruleSection));


            resmap.put("name", "ablahz");
            assertFalse(typeRuleContext.ruleMatchesEqualsSection(resmap, ruleSection));
            resmap.remove("name");
            assertFalse(typeRuleContext.ruleMatchesEqualsSection(resmap, ruleSection));
        }
    }

    public void testTypeRuleContextMatcherContainsRule() {
        {
            //match single attribute
            final Object load = yaml.load("contains: \n"
                                          + "  name: blah\n"
                                          + "allow: '*'");
            assertTrue(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
                ruleSection);

            final HashMap<String, String> resmap = new HashMap<String, String>();

            //false result for no match
            assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));

            resmap.put("name", "something");
            assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("name", "blah");
            assertTrue(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("name", "ablahz");
            assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("name", "blah, test");
            assertTrue(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
        }
        {
            //list must all match the attribute
            final Object load = yaml.load("contains: \n"
                                          + "  name: [blah,shamble]\n"
                                          + "allow: '*'");
            assertTrue(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
                ruleSection);

            final HashMap<String, String> resmap = new HashMap<String, String>();

            //false result for no match
            assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));

            resmap.put("name", "something");
            assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("name", "blah");
            assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("name", "shamble");
            assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("name", "blah, test");
            assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("name", "shamble, test");
            assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("name", "shamble, blah");
            assertTrue(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("name", "blah, shamble");
            assertTrue(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("name", "blah,shamble");
            assertTrue(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("name", " blah,shamble   ");
            assertTrue(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
        }
        {
            //multiple attributes must all match
            final Object load = yaml.load("contains: \n"
                                          + "  name: [blah,shamble]\n"
                                          + "  something: [plead]\n"
                                          + "allow: '*'");
            assertTrue(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
                ruleSection);

            final HashMap<String, String> resmap = new HashMap<String, String>();

            //false result for no match
            assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));

            resmap.put("name", "something");
            assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("name", "blah");
            assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("name", "shamble");
            assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("name", "blah, test");
            assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("name", "shamble, test");
            assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("name", "shamble, blah");
            assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("name", "blah, shamble");
            assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("name", "blah,shamble");
            assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("name", " blah,shamble   ");
            assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));

            //set 'something' attribute
            resmap.put("something", " bloo   ");
            assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("something", " blee   ");
            assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("something", " blee  , plead ");
            assertTrue(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
        }
    }

    public void testTypeRuleContextMatcher() {
        {
            //match any resource with name=~ blah, and allow all actions
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "allow: '*'");
            assertTrue(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
                ruleSection);

            final HashMap<String, String> resmap = new HashMap<String, String>();

            //false result for no match
            ArrayList<ContextEvaluation> list = new ArrayList<ContextEvaluation>();
            assertFalse(typeRuleContext.matchesRuleSections(resmap, list));
            final YamlPolicy.MatchedContext any = typeRuleContext.includes(resmap, "any");
            assertNotNull(any);
            assertFalse(any.getDecision().getEvaluations().toString(), any.isMatched());

            resmap.put("name", "something");
            assertFalse(typeRuleContext.includes(resmap, "any").isMatched());
            resmap.put("name", "blah");
            assertTrue(typeRuleContext.includes(resmap, "any").isMatched());
            resmap.put("name", "ablahz");
            assertTrue(typeRuleContext.includes(resmap, "any").isMatched());

        }
        {
            //add other match sections
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "equals: \n"
                                          + "  group: potato\n"
                                          + "allow: '*'");
            assertTrue(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
                ruleSection);

            final HashMap<String, String> resmap = new HashMap<String, String>();

            //false result for no match
            ArrayList<ContextEvaluation> list = new ArrayList<ContextEvaluation>();
            assertFalse(typeRuleContext.matchesRuleSections(resmap, list));
            final YamlPolicy.MatchedContext any = typeRuleContext.includes(resmap, "any");
            assertNotNull(any);
            assertFalse(any.getDecision().getEvaluations().toString(), any.isMatched());

            resmap.put("name", "something");
            assertFalse(typeRuleContext.includes(resmap, "any").isMatched());
            resmap.put("name", "blah");
            assertFalse(typeRuleContext.includes(resmap, "any").isMatched());
            resmap.put("name", "ablahz");
            assertFalse(typeRuleContext.includes(resmap, "any").isMatched());

            //set 'group'
            resmap.put("group", "loop");
            assertFalse(typeRuleContext.includes(resmap, "any").isMatched());
            resmap.put("group", "potato");
            assertTrue(typeRuleContext.includes(resmap, "any").isMatched());

        }
        {
            //add other match sections
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "equals: \n"
                                          + "  group: potato\n"
                                          + "contains: \n"
                                          + "  elf: [brand,wake]\n"
                                          + "allow: '*'");
            assertTrue(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
                ruleSection);

            final HashMap<String, String> resmap = new HashMap<String, String>();

            //false result for no match
            ArrayList<ContextEvaluation> list = new ArrayList<ContextEvaluation>();
            assertFalse(typeRuleContext.matchesRuleSections(resmap, list));
            final YamlPolicy.MatchedContext any = typeRuleContext.includes(resmap, "any");
            assertNotNull(any);
            assertFalse(any.getDecision().getEvaluations().toString(), any.isMatched());

            resmap.put("name", "something");
            assertFalse(typeRuleContext.includes(resmap, "any").isMatched());
            resmap.put("name", "blah");
            assertFalse(typeRuleContext.includes(resmap, "any").isMatched());
            resmap.put("name", "ablahz");
            assertFalse(typeRuleContext.includes(resmap, "any").isMatched());

            //set 'group'
            resmap.put("group", "loop");
            assertFalse(typeRuleContext.includes(resmap, "any").isMatched());
            resmap.put("group", "potato");
            assertFalse(typeRuleContext.includes(resmap, "any").isMatched());

            //set 'elf' attribute
            resmap.put("elf", "brand, plaid");
            assertFalse(typeRuleContext.includes(resmap, "any").isMatched());
            resmap.put("elf", "wake, plaid");
            assertFalse(typeRuleContext.includes(resmap, "any").isMatched());
            resmap.put("elf", "wake, plaid, milk, brand");
            assertTrue(typeRuleContext.includes(resmap, "any").isMatched());

        }
    }

    public void testRegexPredicate() {
        final YamlPolicy.RegexPredicate blah = new YamlPolicy.RegexPredicate(Pattern.compile("a|b"));

        assertFalse(blah.evaluate(null));
        assertFalse(blah.evaluate(new Object()));
        assertFalse(blah.evaluate("c"));
        assertFalse(blah.evaluate("ababababa"));
        assertTrue(blah.evaluate("a"));
        assertTrue(blah.evaluate("b"));

    }

    public void testSetContainsPredicate() {
        final YamlPolicy.SetContainsPredicate blah = new YamlPolicy.SetContainsPredicate("blah");
        final ArrayList<String> strings = new ArrayList<String>();
        assertFalse(blah.evaluate(strings));
        assertFalse(blah.evaluate(""));
        assertFalse(blah.evaluate(null));
        strings.add("nomatch");
        assertFalse(blah.evaluate(strings));
        assertFalse(blah.evaluate("nomatch"));
        strings.add("blah");
        assertTrue(blah.evaluate(strings));
        assertTrue(blah.evaluate("blah"));
        assertTrue(blah.evaluate("blah, nomatch"));

        final ArrayList<String> input = new ArrayList<String>();
        input.add("test1");
        input.add("test2");
        final YamlPolicy.SetContainsPredicate multiple = new YamlPolicy.SetContainsPredicate(input);
        final ArrayList<String> strings2 = new ArrayList<String>();
        assertFalse(multiple.evaluate(strings2));
        assertFalse(multiple.evaluate(""));
        assertFalse(multiple.evaluate(null));
        strings2.add("nomatch");
        assertFalse(multiple.evaluate(strings2));
        assertFalse(multiple.evaluate("nomatch"));
        strings2.add("blah");
        assertFalse(multiple.evaluate(strings2));
        assertFalse(multiple.evaluate("nomatch, blah"));
        strings2.add("test1");
        assertFalse(multiple.evaluate(strings2));
        assertFalse(multiple.evaluate("nomatch, blah, test1"));
        strings2.remove("test1");
        strings2.add("test2");
        assertFalse(multiple.evaluate(strings2));
        assertFalse(multiple.evaluate("nomatch, blah, test2"));
        strings2.add("test1");

        assertTrue(multiple.evaluate(strings2));
        assertTrue(multiple.evaluate("nomatch, blah, test1, test2"));
    }
}
