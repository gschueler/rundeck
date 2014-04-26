/*
 * The following allows grails to leverage a different url setting for maven central. This would
 * typically be passed along as a -D parameter to grails, ie: grails -Dmaven.central.url=http://...
 */
def mavenCentralUrl = "http://repo1.maven.org/maven2/"
if (System.properties["maven.central.url"]) {
    mavenCentralUrl = System.properties["maven.central.url"]
}
println "Maven Central: ${mavenCentralUrl}"

Boolean mavenCredsDefined = false
def mavenRealm
def mavenHost
def mavenUser
def mavenPassword

// TODO: System.env["mavenRealm"] is a hack.  See comments below.
if (System.env["mavenRealm"] && System.properties["maven.host"] && System.properties["maven.user"] && System.properties["maven.password"]) {
    mavenCredsDefined = true

    /*
     * There's a bug in grails 1.3.7 where system properties (e.g. -Dmaven.realm="Sonatype Nexus Repository Manager") 
     * are truncated at the first space (e.g. System.properties["maven.realm"] is "Sonatype")
     */
    // mavenRealm = System.properties["maven.realm"]

    /*
     * Fortunately, the bug doesn't affect reading environment variables.
     * TODO: This is a hack until grails is fixed
     */
    mavenRealm = System.env["mavenRealm"]

    mavenHost = System.properties["maven.host"]
    mavenUser = System.properties["maven.user"]
    mavenPassword = System.properties["maven.password"]

    println "Maven credentials:\n\tRealm: ${mavenRealm}\n\tHost: ${mavenHost}\n\tUser: ${mavenUser}"
}

def grailsLocalRepo = "grails-app/plugins"
if (System.properties["grails.local.repo"]) {
        grailsLocalRepo = System.properties["grails.local.repo"]
}
println "Grails Local Repo: ${grailsLocalRepo}"

grails.project.dependency.resolution = {
    pom true
    inherits "global" // inherit Grails' default dependencies
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    repositories {
        useOrigin true
        mavenLocal()
        flatDir name:'grailsLocalRepo', dirs:"${grailsLocalRepo}"
        grailsHome()
        grailsPlugins()
        mavenRepo mavenCentralUrl
        grailsCentral()
    }

    if (mavenCredsDefined) {
        credentials {
            realm = mavenRealm
            host = mavenHost
            username = mavenUser
            password = mavenPassword
        }
    }


    rundeckVersion = System.getProperty("RUNDECK_VERSION", appVersion)
    println "Application Version: ${rundeckVersion}"
    plugins {
    }
    dependencies {

    }
}
grails.war.resources = { stagingDir, args ->
    delete(file: "${stagingDir}/WEB-INF/lib/jetty-all-7.6.0.v20120127.jar")
    delete(file: "${stagingDir}/WEB-INF/lib/rundeck-jetty-server-${rundeckVersion}.jar")
    delete(file: "${stagingDir}/WEB-INF/lib/servlet-api-2.5.jar")
}
