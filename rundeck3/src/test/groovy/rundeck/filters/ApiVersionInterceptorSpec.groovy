package rundeck.filters


import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(ApiVersionInterceptor)
class ApiVersionInterceptorSpec extends Specification {

    def setup() {
    }

    def cleanup() {

    }

    void "Test apiVersion interceptor matching"() {
        when:"A request matches the interceptor"
            withRequest(controller:"apiVersion")

        then:"The interceptor does match"
            interceptor.doesMatch()
    }
}
