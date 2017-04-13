package rundeck.filters

import com.dtolabs.client.utils.Constants
import com.dtolabs.rundeck.core.authentication.Group
import com.dtolabs.rundeck.core.authentication.Username
import grails.core.GrailsApplication

import javax.security.auth.Subject


class AuthorizationCheckInterceptor {
    GrailsApplication grailsApplication

    AuthorizationCheckInterceptor() {
        matchAll().excludes(controller: 'user', action: '(logout|login|error|loggedout)',)
    }

    boolean before() {
        if (request.api_version && request.remoteUser &&
                !(grailsApplication.config.rundeck?.security?.apiCookieAccess?.enabled in ['true', true])) {
            //disallow api access via normal login
            request.invalidApiAuthentication = true
            response.setStatus(403)
            def authid = session.user ?: "(${request.invalidAuthToken ?: 'unauthenticated'})"
            log.error("${authid} UNAUTHORIZED for ${controllerName}/${actionName}");
            if (request.api_version) {
                //api request
                if (response.format in ['json']) {
                    render(contentType: "application/json", encoding: "UTF-8") {
                        error = true
                        apiversion = ApiRequestFiltersUtil.API_CURRENT_VERSION
                        errorCode = "unauthorized"
                        message = ("${authid} is not authorized for: ${request.forwardURI}")
                    }
                } else {
                    render(contentType: "text/xml", encoding: "UTF-8") {
                        result(error: "true", apiversion: ApiRequestFiltersUtil.API_CURRENT_VERSION) {
                            delegate.'error'(code: "unauthorized") {
                                message("${authid} is not authorized for: ${request.forwardURI}")
                            }
                        }
                    }
                }
                return false
            }
            flash.title = "Unauthorized"
            flash.error = "${authid} is not authorized"
            response.setHeader(Constants.X_RUNDECK_ACTION_UNAUTHORIZED_HEADER, flash.error)
            redirect(
                    controller: 'user',
                    action: actionName ==~ /^.*(Fragment|Inline)$/ ? 'deniedFragment' : 'denied',
                    params: params.xmlreq ? params.subMap(['xmlreq']) : null
            )
            return false;
        }
        if (request.remoteUser && session.user != request.remoteUser) {
            session.user = request.remoteUser

            Subject subject = AuthorizationFiltersUtil.createAuthSubject(request,applicationContext)

            request.subject = subject
            session.subject = subject
        } else if (request.remoteUser && session.subject &&
                grailsApplication.config.rundeck.security.authorization.preauthenticated.enabled in
                ['true', true]) {
            // Preauthenticated mode is enabled, handle upstream role changes
            Subject subject = AuthorizationFiltersUtil.createAuthSubject(request,applicationContext)
            request.subject = subject
            session.subject = subject
        } else if (request.remoteUser && session.subject &&
                grailsApplication.config.rundeck.security.authorization.preauthenticated.enabled in
                ['false', false]) {
            request.subject = session.subject
        } else if (request.api_version && !session.user) {
            //allow authentication token to be used
            def authtoken = params.authtoken ? params.authtoken : request.getHeader('X-RunDeck-Auth-Token')
            String user = AuthorizationFiltersUtil.lookupToken(authtoken, servletContext,log)
            List<String> roles = AuthorizationFiltersUtil.lookupTokenRoles(authtoken, servletContext,log)

            if (user) {
                session.user = user
                request.authenticatedToken = authtoken
                request.authenticatedUser = user
                def subject = new Subject();
                subject.principals << new Username(user)

                roles.each { role ->
                    subject.principals << new Group(role.trim());
                }

                request.subject = subject
                session.subject = subject
            } else {
                request.subject = null
                session.subject = null
                session.user = null
                if (authtoken) {
                    request.invalidAuthToken = "Token:" + (authtoken.size() > 5 ? authtoken.substring(0, 5) : '') +
                            "****"
                }
                request.authenticatedToken = null
                request.authenticatedUser = null
                request.invalidApiAuthentication = true
                if (authtoken) {
                    log.error("Invalid API token used: ${authtoken}");
                } else {
                    log.error("Unauthenticated API request");
                }
                return false
            }
        } else if (!request.remoteUser && controllerName && !(controllerName in ['assets', 'feed'])) {
            //unauthenticated request to an action
            if(grailsApplication.config.rundeck?.authorization?.anonymous){
                def subject = new Subject();
                subject.principals << new Username(grailsApplication.config.rundeck?.authorization?.anonymousUser?:'anonymous')

//                roles.each { role ->
                    subject.principals << new Group(grailsApplication.config.rundeck?.authorization?.anonymousRole?:'none');
//                }

                request.subject = subject
                session.subject = subject
                return true
            }
            response.status = 403
            request.errorCode = 'request.authentication.required'
            render(view: '/common/error.gsp')
            return false
        }
        true
    }

    boolean after() {
        true
    }

    void afterView() {
        // no-op
        if (request?.authenticatedToken && session && session?.user) {
            session.user = null
            request.subject = null
            session.subject = null
        }
    }
}
