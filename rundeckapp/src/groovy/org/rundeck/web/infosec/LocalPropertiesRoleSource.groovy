package org.rundeck.web.infosec

import org.springframework.beans.factory.InitializingBean

import javax.servlet.http.HttpServletRequest
import java.util.regex.Pattern

/**
 * Reads roles from a properties file, settings:
 * <li>enabled=true/false</li>
 * <li>legacyFormatSkipFirst=true/false, if true, then the first entry in the properties file for a user is skipped (default: true)</li>
 * <li>delimiter=, the delimiter char (default comma) separating role name entries for a user</li>
 * <li>file=/path/to/file, path to a java .properties file containing "user: role,role2,.." listing authorization roles</li>
 * <li>defaultRoles=role1,role2 default authorization roles to assign if user entry is not found in the file (default: none)</li>
 *
 */
class LocalPropertiesRoleSource implements AuthorizationRoleSource, InitializingBean {
    boolean enabled
    boolean legacyFormatSkipFirst
    String delimiter = ','
    String file
    String defaultRoles

    private Properties props
    private long timestamp = -1L
    private File filepath

    private Properties readProperties() {
        if (filepath.lastModified() > timestamp) {
            synchronized (this) {
                props = new Properties()
                filepath.withInputStream {
                    props.load(it)
                }
                timestamp = filepath.lastModified()
            }
        }
        props
    }

    @Override
    void afterPropertiesSet() throws Exception {
        if (!enabled) {
            return
        }
        if (enabled && !file) {
            throw new IllegalStateException("File is required")
        }
        filepath = new File(file)
        if (!filepath.file) {
            throw new IllegalStateException("File does not exist or is not readable: ${file}")
        }
    }

    @Override
    Collection<String> getUserRoles(final String username, final HttpServletRequest request) {
        if (enabled) {
            Properties loaded = readProperties()
            String rolestring = loaded.get(username)
            if (rolestring || defaultRoles) {

                List<String> roles = (rolestring?:defaultRoles).
                        split(" *${Pattern.quote(delimiter)} *").
                        collect { it.trim() } as List<String>
                if (legacyFormatSkipFirst && rolestring && roles) {
                    roles.remove(0)
                }
                return roles.findAll { it }
            }
        }
        []
    }
}
