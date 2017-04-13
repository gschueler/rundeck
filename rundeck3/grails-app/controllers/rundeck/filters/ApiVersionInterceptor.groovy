package rundeck.filters

import com.codahale.metrics.MetricRegistry
import grails.converters.JSON
import grails.converters.XML

import javax.servlet.http.HttpServletResponse


class ApiVersionInterceptor {

    def MetricRegistry metricRegistry
    def messageSource
    def apiService
    ApiVersionInterceptor() {
        match(uri: '/api/**')
    }

    private com.codahale.metrics.Timer.Context timer() {
        metricRegistry.timer(MetricRegistry.name('rundeck.api.requests', 'requestTimer')).time()
    }
    boolean before() {
        request[ApiRequestFiltersUtil.REQUEST_TIME]=System.currentTimeMillis()
        request[ApiRequestFiltersUtil.METRIC_TIMER]= timer()
        if (request.remoteUser && null != session.api_access_allowed && !session.api_access_allowed) {
            log.debug("Api access request disallowed for ${request.forwardURI}")
            response.sendError(HttpServletResponse.SC_NOT_FOUND)
            return false
        } else if (null == session.api_access_allowed) {
            session.api_access_allowed = true
        }
        if (controllerName == 'api' && allowed_actions.contains(actionName) || request.api_version) {
            request.is_allowed_api_request = true
            return true
        }

        if (!params.api_version) {
            flash.errorCode = 'api.error.api-version.required'
//                    AA_TimerFiltersDisabled.afterRequest(request, response, session)
            ApiRequestFiltersUtil.logDetail(request, params.toString(), actionName, controllerName, 'api.error.api-version.required')
            apiService.renderErrorFormat(response, [code: 'api.error.api-version.required'])
            return false
        }
        def unsupported = !(ApiRequestFiltersUtil.VersionMap.containsKey(params.api_version))
        if (unsupported) {
//                    AA_TimerFiltersDisabled.afterRequest(request, response, session)
            ApiRequestFiltersUtil.logDetail(request, params.toString(), actionName, controllerName, 'api.error.api-version.unsupported')
            apiService.renderErrorFormat(response,
                                         [
                                                 status: HttpServletResponse.SC_BAD_REQUEST,
                                                 code  : 'api.error.api-version.unsupported',
                                                 args  : [params.api_version, request.forwardURI, "Current version: " +
                                                         ApiRequestFiltersUtil.API_CURRENT_VERSION]
                                         ]
            )
            return false;
        }
        request.api_version = ApiRequestFiltersUtil.VersionMap[params.api_version]
        request['ApiRequestFilters.request.parameters.project'] = params.project ?: request.project ?: ''
        XML.use('v' + request.api_version)
        JSON.use('v' + request.api_version)
        return true
    }

    boolean after() {
        ApiRequestFiltersUtil.logDetail(request, request['ApiRequestFilters.request.parameters.project']?:'', actionName, controllerName)
        true
    }

    void afterView() {
        // no-op
    }
}
