import java.util.Collections;

import javax.security.auth.Subject

import com.dtolabs.rundeck.core.authentication.Username;
import com.dtolabs.rundeck.core.authentication.Group;
import com.dtolabs.rundeck.core.authorization.Attribute;
import com.dtolabs.rundeck.core.authorization.Authorization;

class AuthTagLib {
    def static namespace="auth"
    def userService
    def FrameworkService frameworkService
    static returnObjectForTags = ['jobAllowedTest','adhocAllowedTest', 'resourceAllowedTest']
    
    /**
     * Render an enclosed body if user authorization matches the assertion.  Attributes:
     *  'auth'= name of auth to check, 'has'= true/false [optional], 'altText'=failed assertion message [optional]
     *
     * if has is 'true' then the body is rendered if the user has the specified role.
     * if has is 'false' then the body is rendered if the user DOES NOT have the specified role.
     *
     * otherwise if altText is set, it is rendered.
     */
    def jobAllowed ={attrs,body->
        if(!attrs.action && !attrs.name){
            throw new Exception("action attribute required: " + attrs.action + ": " + attrs.name)
        }
        
        def action = attrs.action
        if(!action) {
            action = attrs.name
        }
        
        
        if(!attrs.job) {
            throw new Exception("job required for action: " + action);
        }
        
        
        boolean has=(!attrs.has || attrs.has == "true")
        
        def framework = frameworkService.getFrameworkFromUserSession(request.session, request)
        def Authorization authr = framework.getAuthorizationMgr()
        
        def resource = ["job": attrs.job?.jobName, "group": (attrs.job?.groupPath ?: ""), type: 'job']

        def env = Collections.singleton(new Attribute(URI.create("http://dtolabs.com/rundeck/env/project"), session.project))

        def decision = authr.evaluate(resource, request.subject, action, env)
        
        if(has && decision.authorized){
            out<<body()
        }else if(!has && !decision.authorized){
            out<<body()
        }else if(attrs.altText){
            out<<attrs.altText
        }
    }
    /**
     * Render an enclosed body if user authorization matches the assertion.  Attributes:
     *  'auth'= name of auth to check, 'has'= true/false [optional], 'altText'=failed assertion message [optional]
     *
     * if has is 'true' then the body is rendered if the user has the specified role.
     * if has is 'false' then the body is rendered if the user DOES NOT have the specified role.
     *
     * otherwise if altText is set, it is rendered.
     */
    def adhocAllowed ={attrs,body->
        if(!attrs.action ){
            throw new Exception("action attribute required: " + attrs.action + ": " + attrs.name)
        }

        def action = attrs.action

        boolean has=(!attrs.has || attrs.has == "true")

        def framework = frameworkService.getFrameworkFromUserSession(request.session, request)
        def Authorization authr = framework.getAuthorizationMgr()

        def resource = [ type: 'adhoc']

        def env = Collections.singleton(new Attribute(URI.create("http://dtolabs.com/rundeck/env/project"), session.project))

        def decision = authr.evaluate(resource, request.subject, action, env)

        if(has && decision.authorized){
            out<<body()
        }else if(!has && !decision.authorized){
            out<<body()
        }else if(attrs.altText){
            out<<attrs.altText
        }
    }
    /**
     * Render an enclosed body if user authorization matches the assertion.  Attributes:
     *  'auth'= name of auth to check, 'has'= true/false [optional], 'altText'=failed assertion message [optional]
     *
     * if has is 'true' then the body is rendered if the user has the specified role.
     * if has is 'false' then the body is rendered if the user DOES NOT have the specified role.
     *
     * otherwise if altText is set, it is rendered.
     */
    def resourceAllowed = {attrs, body ->
        if (!attrs.action) {
            throw new Exception("action attribute required: " + attrs.action + ": " + attrs.name)
        }

        def action = attrs.action

        boolean has = (!attrs.has || attrs.has == "true")

        def framework = frameworkService.getFrameworkFromUserSession(request.session, request)
        def Authorization authr = framework.getAuthorizationMgr()

        def env
        if ('application'==attrs.context){
            env=Collections.singleton(new Attribute(URI.create("http://dtolabs.com/rundeck/env/application"), 'rundeck'))
        }else{
            env=Collections.singleton(new Attribute(URI.create("http://dtolabs.com/rundeck/env/project"), session.project))
        }
        def resource = [type: attrs.type?:'resource']
        def tagattrs=[:]
        tagattrs.putAll(attrs)
        tagattrs.remove('type')
        tagattrs.remove('action')
        tagattrs.remove('has')
        tagattrs.remove('context')
        def attributes = attrs.attributes?:tagattrs
        if(attributes){
            resource.putAll(attributes)
        }

        def decision = authr.evaluate(resource, request.subject, action, env)

        if (has && decision.authorized) {
            out << body()
        } else if (!has && !decision.authorized) {
            out << body()
        } else if (attrs.altText) {
            out << attrs.altText
        }
    }

    /**
     * return true if user authorization matches the assertion.  Attributes:
     *  'name'= name of auth to check, 'has'= true/false [optional], 'altText'=failed assertion message [optional]
     *  'name' can also be a list of auth names, and all of them must match
     *
     * if has is 'true' then the body is rendered if the user has the specified authorization.
     * if has is 'false' then the body is rendered if the user DOES NOT have the specified authorization.
     *
     */
    def resourceAllowedTest = {attrs, body ->
        boolean has = (null == attrs.has || attrs.has == "true")
        boolean auth = false
        if (!attrs.action) {
            throw new Exception("action attribute required: " + attrs.action + ": " + attrs.name)
        }

        def action = attrs.action

        def Set tests = []
        if (action instanceof String) {
            tests.add(action)
        } else if (action instanceof Collection) {
            tests.addAll(action)
        }

        def framework = frameworkService.getFrameworkFromUserSession(request.session, request)
        def Authorization authr = framework.getAuthorizationMgr()
        def env
        if ('application' == attrs.context) {
            env = Collections.singleton(new Attribute(URI.create("http://dtolabs.com/rundeck/env/application"), 'rundeck'))
        } else {
            env = Collections.singleton(new Attribute(URI.create("http://dtolabs.com/rundeck/env/project"), session.project))
        }
        def resource = [type: attrs.type ?: 'resource']
        def tagattrs = [:]
        tagattrs.putAll(attrs)
        tagattrs.remove('type')
        tagattrs.remove('action')
        tagattrs.remove('has')
        tagattrs.remove('context')
        def attributes = attrs.attributes ?: tagattrs
        if (attributes) {
            resource.putAll(attributes)
        }
        def Set resources = [resource]

        authr.evaluate(resources, request.subject, tests, env).each { def decision ->
            // has == true, authorized == true => auth = true
            // has == true, authorized == false => auth = false
            // has == false, authorized == true => auth = false
            // has == false, authorized == false => auth = true
            auth = !(has ^ decision.isAuthorized()) // inverse xor
            if (auth)
                return;
        }
        return auth;
    }
    /**
     * return true if user authorization matches the assertion.  Attributes:
     *  'name'= name of auth to check, 'has'= true/false [optional], 'altText'=failed assertion message [optional]
     *  'name' can also be a list of auth names, and all of them must match
     *
     * if has is 'true' then the body is rendered if the user has the specified authorization.
     * if has is 'false' then the body is rendered if the user DOES NOT have the specified authorization.
     *
     */
    def adhocAllowedTest = {attrs, body ->
        boolean has = (null == attrs.has || attrs.has == "true")
        boolean auth = false
        if (!attrs.action ) {
            throw new Exception("action attribute required: " + attrs.action + ": " + attrs.name)
        }

        def action = attrs.action

        def Set tests = []
        if (action instanceof String) {
            tests.add(action)
        } else if (action instanceof Collection) {
            tests.addAll(action)
        }

        def framework = frameworkService.getFrameworkFromUserSession(request.session, request)
        def Authorization authr = framework.getAuthorizationMgr()

        def Set resource = [[type: 'adhoc']]

        def env = Collections.singleton(new Attribute(URI.create("http://dtolabs.com/rundeck/env/project"), session.project))

        authr.evaluate(resource, request.subject, tests, env).each { def decision ->
            // has == true, authorized == true => auth = true
            // has == true, authorized == false => auth = false
            // has == false, authorized == true => auth = false
            // has == false, authorized == false => auth = true
            auth = !(has ^ decision.isAuthorized()) // inverse xor
            if (auth)
                return;
        }
        return auth;
    }
    /**
     * return true if user authorization matches the assertion.  Attributes:
     *  'name'= name of auth to check, 'has'= true/false [optional], 'altText'=failed assertion message [optional]
     *  'name' can also be a list of auth names, and all of them must match
     *
     * if has is 'true' then the body is rendered if the user has the specified authorization.
     * if has is 'false' then the body is rendered if the user DOES NOT have the specified authorization.
     *
     */
    def jobAllowedTest ={attrs,body->
        boolean has=(null==attrs.has || attrs.has == "true")
        boolean auth=false
        if(!attrs.action && !attrs.name){
            throw new Exception("action attribute required: " + attrs.action + ": " + attrs.name)
        }
        
        def action = attrs.action
        if(!action) {
            action = attrs.name
        }
        
        def Set tests=[]
        if(action instanceof String) {
            tests.add(action)
        } else if(action instanceof Collection){
            tests.addAll(action)
        }
        
        if(!attrs.job) {
            throw new Exception("job required for action: " + tests);
        }
        
                
//            def authorized = userService.userHasAuthorization(request.remoteUser,name)
        
        def framework = frameworkService.getFrameworkFromUserSession(request.session, request)
        def Authorization authr = framework.getAuthorizationMgr()
        
        def Set resource = [ ["job": attrs.job?.jobName, "group": (attrs.job?.groupPath ?: ""), type:'job'] ]

        def env = Collections.singleton(new Attribute(URI.create("http://dtolabs.com/rundeck/env/project"), session.project))
        
        authr.evaluate(resource, request.subject, tests, env).each{ def decision ->
            // has == true, authorized == true => auth = true
            // has == true, authorized == false => auth = false
            // has == false, authorized == true => auth = false
            // has == false, authorized == false => auth = true
            auth = !(has ^ decision.isAuthorized()) // inverse xor
            if(auth)
                return;
        }
        return auth;
    }
}
