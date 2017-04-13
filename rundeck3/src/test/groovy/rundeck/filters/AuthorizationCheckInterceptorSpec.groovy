package rundeck.filters


import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(AuthorizationCheckInterceptor)
class AuthorizationCheckInterceptorSpec extends Specification {

    def setup() {
    }

    def cleanup() {

    }

    void "Test authorizationCheck interceptor matching"() {
        when:"A request matches the interceptor"
            withRequest(controller:"authorizationCheck")

        then:"The interceptor does match"
            interceptor.doesMatch()
    }
}
