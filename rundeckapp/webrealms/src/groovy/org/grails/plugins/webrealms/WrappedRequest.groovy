package org.grails.plugins.webrealms

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletRequestWrapper
import java.security.Principal

/**
 * override remoteUser and remoteHost
 */
class WrappedRequest extends HttpServletRequestWrapper {
    private String remoteUser
    private String remoteHost
    private UserPrincipal userPrincipal

    WrappedRequest(final HttpServletRequest request, String remoteUser, String remoteHost) {
        super(request)
        this.remoteUser = remoteUser
        this.remoteHost = remoteHost
        if (remoteUser) {
            userPrincipal = new UserPrincipal(name: remoteUser)
        }
    }

    @Override
    Principal getUserPrincipal() {
        userPrincipal ?: super.getUserPrincipal()
    }

    @Override
    String getRemoteUser() {
        remoteUser ?: super.getRemoteUser()
    }

    @Override
    String getRemoteHost() {
        remoteHost ?: super.getRemoteHost()
    }

}
