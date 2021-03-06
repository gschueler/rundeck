/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
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

package com.dtolabs.rundeck.core.authorization;

import com.dtolabs.rundeck.core.authentication.Group;
import com.dtolabs.rundeck.core.authentication.LdapGroup;
import com.dtolabs.rundeck.core.authentication.Username;
import com.dtolabs.rundeck.core.authorization.providers.Policies;
import com.dtolabs.rundeck.core.authorization.providers.Policies.Context;
import junit.framework.TestCase;

import javax.security.auth.Subject;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TestPolicies extends TestCase {

    private Policies policies;
    
    public void setUp() throws Exception {

        policies = Policies.load(getPath("com/dtolabs/rundeck/core/authorization"));
    }

    /**
     * @return
     */
    public static File getPath(String name) {
        
        URL url = ClassLoader.getSystemResource(name);
        File f;
        try {
          f = new File(url.toURI());
        } catch(URISyntaxException e) {
          f = new File(url.getPath());
        }
        return f;
    }
    
    public void testPoliciesStructural() throws Exception {
        assertEquals("Policy count mismatch", 6, policies.count());
    }
    
    public void testSelectOnUsers() throws Exception {
        
        Subject formalSubject = new Subject();
        formalSubject.getPrincipals().add(new Username("johnwayne"));
        formalSubject.getPrincipals().add(new Group("admin"));
        formalSubject.getPrincipals().add(new Group("foo"));
        formalSubject.getPrincipals().add(new LdapGroup("OU=Foo,dc=example,dc=com"));
        
        Set<Attribute> environment = new HashSet<Attribute>();
        List<Context> contexts = policies.narrowContext(formalSubject, environment);
        assertNotNull("Context is null.", contexts);
        assertEquals("Incorrect number of contexts returned when matching on username.", 1, contexts.size());
         
        formalSubject = new Subject();
        formalSubject.getPrincipals().add(new Group("admin")); // <-- will match on group membership.
        contexts = policies.narrowContext(formalSubject, environment);
        assertNotNull("Context is null.", contexts);
        assertEquals("Incorrect number of contexts returned when matching on group.", 1, contexts.size());
        
        formalSubject = new Subject();
        formalSubject.getPrincipals().add(new LdapGroup("OU=Foo,dc=example,dc=com")); // <-- will match on ldapgroup membership.
        contexts = policies.narrowContext(formalSubject, environment);
        assertNotNull("Context is null.", contexts);
        assertEquals("Incorrect number of usings returned when matching on ldap:group.", 1, contexts.size());
        
    }
    
    public void testListAllRoles() throws Exception {
        List<String> results = policies.listAllRoles();
        assertEquals("Results did not return the correct number of policies.", 6, results.size());
        results.containsAll(Arrays.asList(new String[]{"admin","foo","admin-environment","ou=Foo,dn=example,dn=com"}));
    }
}
