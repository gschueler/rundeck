package rundeck.filters


import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(ApiIncubatorInterceptor)
class ApiIncubatorInterceptorSpec extends Specification {

    def setup() {
    }

    def cleanup() {

    }

    void "Test apiIncubator interceptor matching"() {
        when:"A request matches the interceptor"
            withRequest(controller:"apiIncubator")

        then:"The interceptor does match"
            interceptor.doesMatch()
    }
}
