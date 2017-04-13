package rundeck.filters

import grails.core.GrailsApplication
import rundeck.services.ApiService

import javax.servlet.http.HttpServletResponse


class ApiIncubatorInterceptor {
    ApiService apiService
    GrailsApplication grailsApplication

    ApiIncubatorInterceptor() {
        match(uri: "/api/**")
    }

    boolean before() {
        def path = request.forwardURI.split('/')
        def feature = path.length > 4 && path[3] == 'incubator' ? path[4] : null
        def featurePresent = {
            def splat = grailsApplication.config.feature?.incubator?.getAt('*') in ['true', true]
            splat || (grailsApplication.config?.feature?.incubator?.getAt(it) in ['true', true])
        }
        if (feature && !(featurePresent(feature))) {
            apiService.renderErrorFormat(response,
                                         [
                                                 status: HttpServletResponse.SC_NOT_FOUND,
                                                 code  : 'api.error.invalid.request',
                                                 args  : [request.forwardURI]
                                         ]
            )
            return false;
        }
        true
    }

    boolean after() { true }

    void afterView() {
        // no-op
    }
}
