package org.rundeck.web.infosec

import javax.servlet.http.HttpServletRequest
import java.util.regex.Pattern

/**
 * Created by greg on 11/11/15.
 */
class PreauthenticatedHeaderRoleSource implements AuthorizationRoleSource {
    String headerName
    String delimiter=','
    boolean enabled

    @Override
    Collection<String> getUserRoles(final String username, final HttpServletRequest request) {
        if(enabled && headerName){
            def value=request.getHeader(headerName)
            if(value && value instanceof String){
                return value.split(" *${Pattern.quote(delimiter)} *").collect{it.trim()} as List<String>
            }
        }
        []
    }

}
