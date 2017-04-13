package rundeck.filters


class ApiAccessInterceptor {

    ApiAccessInterceptor() {
        matchAll().excludes(uri: '/api/**')
    }

    boolean before() {

        if (ApiRequestFiltersUtil.allowed_pre_api_reqs[controllerName] && (actionName in ApiRequestFiltersUtil.allowed_pre_api_reqs[controllerName])) {
            return true
        }
        if (null == session.api_access_allowed) {
            log.debug("Disallowing API access, blocked due to request for ${controllerName}/${actionName}")
            session.api_access_allowed = false
        }
        true
    }

    boolean after() { true }

    void afterView() {
        // no-op
    }
}
