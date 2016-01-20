package org.grails.plugins.webrealms

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.support.WebApplicationContextUtils

import javax.servlet.*
import javax.servlet.http.HttpServletRequest

/**
 * <p>Allows HTTP header values to be substituted for request remoteUser, and remoteHost values, for
 * use behind a pre-authenticated proxy which provides authentication information via HTTP Header.</p>
 * <p>
 *     <code>rundeck.web.auth.filter.forwardedAuthentication.enabled=true/false</code>
 *     <code>rundeck.web.auth.filter.forwardedAuthentication.enableForwardedUserHeader=true/false</code>
 *     <code>rundeck.web.auth.filter.forwardedAuthentication.forwardedUserHeader=X-Forwarded-User</code>
 *     <code>rundeck.web.auth.filter.forwardedAuthentication.enableForwardedForHeader=true/false</code>
 *     <code>rundeck.web.auth.filter.forwardedAuthentication.forwardedForHeader=X-Forwarded-For</code>
 *     </p>
 */
class ForwardedAuthenticationFilter implements Filter {
    String xForwardedUserHeader = null;
    String xForwardedForHeader = null;
    boolean enabled = false

    @Override
    void init(final FilterConfig filterConfig) throws ServletException {

        WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(
                filterConfig.servletContext
        );
        def gapp = context.getBean(GrailsApplication.class)
        if (gapp == null) {
            throw new IllegalStateException("grailsApplication not found in context")
        }


        def config = gapp.config.rundeck?.web?.auth?.filter?.forwardedAuthentication

        enabled = config?.enabled in [true, 'true']
        if (config?.enableForwardedUserHeader in [true, 'true']) {
            xForwardedUserHeader = config?.forwardedUserHeader ?:
                    'X-Forwarded-User'
        }
        if (config?.enableForwardedForHeader in [true, 'true']) {
            xForwardedForHeader = config?.forwardedForHeader ?:
                    'X-Forwarded-For'
        }


    }

    @Override
    void doFilter(
            final ServletRequest servletRequest,
            final ServletResponse servletResponse,
            final FilterChain filterChain
    ) throws IOException, ServletException
    {
        ServletRequest newRequest = servletRequest
        if (enabled) {
            newRequest = extractForwardedValues((HttpServletRequest) servletRequest)
        }
        filterChain.doFilter(newRequest, servletResponse)
    }

    ServletRequest extractForwardedValues(final HttpServletRequest servletRequest) {
        String remoteUser = xForwardedUserHeader ? servletRequest.getHeader(xForwardedUserHeader) : null
        String remoteHost = xForwardedForHeader ? servletRequest.getHeader(xForwardedForHeader) : null
        if (remoteUser || remoteHost) {
            return wrapRequest(servletRequest, remoteUser, remoteHost)
        }
        return servletRequest
    }

    ServletRequest wrapRequest(
            final HttpServletRequest httpServletRequest,
            final String remoteUser,
            final String remoteHost
    )
    {
        new WrappedRequest(httpServletRequest, remoteUser, remoteHost)
    }

    @Override
    void destroy() {

    }
}
