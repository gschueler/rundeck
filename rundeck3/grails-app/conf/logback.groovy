import grails.util.BuildSettings
import grails.util.Environment
import org.springframework.boot.logging.logback.ColorConverter
import org.springframework.boot.logging.logback.WhitespaceThrowableProxyConverter

import java.nio.charset.Charset

conversionRule 'clr', ColorConverter
conversionRule 'wex', WhitespaceThrowableProxyConverter

// See http://logback.qos.ch/manual/groovy.html for details on configuration
appender('STDOUT', ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        charset = Charset.forName('UTF-8')

        pattern =
                '%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} ' + // Date
                        '%clr(%5p) ' + // Log level
                        '%clr(---){faint} %clr([%15.15t]){faint} ' + // Thread
                        '%clr(%-40.40logger{39}){cyan} %clr(:){faint} ' + // Logger
                        '%m%n%wex' // Message
    }
}

def targetDir = BuildSettings.TARGET_DIR
if (Environment.isDevelopmentMode() && targetDir != null) {
    appender("FULL_STACKTRACE", FileAppender) {
        file = "${targetDir}/stacktrace.log"
        append = true
        encoder(PatternLayoutEncoder) {
            pattern = "%level %logger - %msg%n"
        }
    }
    logger("StackTrace", ERROR, ['FULL_STACKTRACE'], false)
}
root(ERROR, ['STDOUT'])
logger 'grails.artefact.Interceptor', DEBUG, ['STDOUT'], false

//old logging config:

// log4j={
//     // Example of changing the log pattern for the default console
//     // appender:
//     //
//     //appenders {
//     //    console name:'stdout', layout:pattern(conversionPattern: '%c{2} %m%n')
//     //}
//     root {
//         error()
//         additivity = true
//     }


//     error  'org.codehaus.groovy.grails.web.servlet',  //  controllers
//                'org.codehaus.groovy.grails.web.pages', //  GSP
//                'org.codehaus.groovy.grails.web.sitemesh', //  layouts
//                'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
//                'org.codehaus.groovy.grails.web.mapping', // URL mapping
//                'org.codehaus.groovy.grails.commons', // core / classloading
//                'org.codehaus.groovy.grails.plugins', // plugins
//                'org.codehaus.groovy.grails.orm.hibernate', // hibernate integration
//                'org.springframework',
//                'org.hibernate',
//            'net.sf.ehcache.hibernate'

//     warn 'org.mortbay.log'
//     warn 'grails.app.filters.AuthorizationFilters'
//     info 'grails.app.conf'
// //    info 'com.dtolabs.rundeck.core.authorization.providers.SAREAuthorization'

//     appenders {
//         environments {
//             development {
//                 console name: "access", layout: pattern(conversionPattern: "[%d{ISO8601}] \"%X{method} %X{uri}\" %X{duration} %X{remoteHost} %X{secure} %X{remoteUser} %X{authToken} %X{project} [%X{contentType}] (%X{userAgent})%n")
//             }
//         }
//         if (System.properties['rundeck.grails.stacktrace.enabled']=='true'
//                 && System.properties['rundeck.grails.stacktrace.dir']) {
//             String logDir = System.properties['rundeck.grails.stacktrace.dir']
//             rollingFile name: 'stacktrace',
//                     maximumFileSize: 10 * 1024 * 1024,
//                     file: "$logDir/stacktrace.log",
//                     layout: pattern(conversionPattern: '%d [%t] %-5p %c{2} %x - %m%n'),
//                     maxBackupIndex: 10
//         } else {
//             delegate.'null'( name: 'stacktrace')
//         }
//     }
//     environments {
//         development {
//             info 'org.rundeck.api.requests'
// //            info 'org.rundeck.web.requests'
// //            debug 'org.rundeck.web.infosec'
//             debug 'org.apache.commons.httpclient'
//             info 'grails.app.services.rundeck.services.ProjectManagerService'
//             //off 'h2database'
//             //info 'grails.app.utils.rundeck.codecs.SanitizedHTMLCodec'
//         }
//     }
// }