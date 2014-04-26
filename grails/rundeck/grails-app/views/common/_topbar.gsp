<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" %>
<g:set var="selectParams" value="${[:]}"/>
<g:if test="${pageScope._metaTabPage}">
    <g:set var="selectParams" value="${[page: _metaTabPage,project:params.project?:request.project]}"/>
</g:if>
<nav class="navbar navbar-default navbar-static-top" role="navigation">

    <div class="navbar-header">
        <button type="button" class="navbar-toggle" data-toggle="collapse" data-target="#bs-example-navbar-collapse-1">
            <span class="sr-only">Toggle navigation</span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
        </button>
    <a href="${grailsApplication.config.rundeck.gui.titleLink ? grailsApplication.config.rundeck.gui.titleLink : g.resource(dir: '/')}"
       title="Home" class="navbar-brand">
        <g:set var="appTitle"
               value="${grailsApplication.config.rundeck.gui.title ? grailsApplication.config.rundeck.gui.title : g.message(code: 'main.app.name')}"/>
        <i class="rdicon app-logo"></i>
        ${appTitle}
    </a>
    </div>

    <div class="container">
    <div class="collapse navbar-collapse" id="bs-example-navbar-collapse-1">
    <ul class="nav navbar-nav">
<g:if test="${session?.user && request.subject }">
    <g:set var="homeselected" value="${false}"/>
    <g:ifPageProperty name='meta.tabpage'>
        <g:ifPageProperty name='meta.tabpage' equals='home'>
            <g:set var="homeselected" value="${true}"/>
        </g:ifPageProperty>
    </g:ifPageProperty>
    <g:if test="${! homeselected}">

    <g:if test="${params.project ?: request.project || session?.projects}">
        <g:if test="${session.frameworkProjects}">
            <li class="dropdown" id="projectSelect">
                <g:render template="/framework/projectSelect"
                          model="${[projects: session.frameworkProjects, project: params.project ?: request.project, selectParams: selectParams]}"/>
            </li>
        </g:if>
        <g:else>
            <li id="projectSelect" class="dropdown">
                <span class="action textbtn button" onclick="loadProjectSelect();"
                      title="Select project...">${params.project ?: request.project?: 'Select project&hellip;'}
                </span>
            </li>
        </g:else>
    </g:if>

        <g:set var="selectedclass" value="active"/>

        <g:set var="wfselected" value=""/>
        <g:ifPageProperty name='meta.tabpage' >
        <g:ifPageProperty name='meta.tabpage' equals='jobs'>
           <g:set var="wfselected" value="${selectedclass}"/>
        </g:ifPageProperty>
        </g:ifPageProperty>
        <g:set var="resselected" value=""/>
        <g:ifPageProperty name='meta.tabpage'>
            <g:ifPageProperty name='meta.tabpage' equals='nodes'>
                <g:set var="resselected" value="${selectedclass}"/>
            </g:ifPageProperty>
        </g:ifPageProperty>
        <g:set var="adhocselected" value=""/>
        <g:ifPageProperty name='meta.tabpage'>
            <g:ifPageProperty name='meta.tabpage' equals='adhoc'>
                <g:set var="adhocselected" value="${selectedclass}"/>
            </g:ifPageProperty>
        </g:ifPageProperty>
        <g:set var="eventsselected" value=""/>
        <g:ifPageProperty name='meta.tabpage'>
            <g:ifPageProperty name='meta.tabpage' equals='events'>
                <g:set var="eventsselected" value="${selectedclass}"/>
            </g:ifPageProperty>
        </g:ifPageProperty>
        <g:if test="${params.project?:request.project}">
        <li class="${wfselected}"><g:link controller="menu" action="jobs" class=" toptab ${wfselected}"
                                          params="[project: params.project ?: request.project]">
           <g:message code="gui.menu.Workflows"/>
        </g:link></li><!--
        --><li class="${resselected}"><g:link controller="framework" action="nodes" class=" toptab ${resselected}"
                                              params="[project: params.project ?: request.project]" >
           <g:message code="gui.menu.Nodes"/>
       </g:link></li><!--
        --><g:if
                test="${auth.adhocAllowedTest(action:AuthConstants.ACTION_RUN,project: params.project?:request.project)}"><li
                    class="${adhocselected}"><g:link
                controller="framework" action="adhoc"
                                                  class=" toptab ${adhocselected}"
                                                params="[project: params.project ?: request.project]">
           <g:message code="gui.menu.Adhoc"/>
       </g:link></li></g:if><!--
        --><li class="${eventsselected}"><g:link controller="reports"  action="index" class=" toptab ${eventsselected}"
                                                 params="[project: params.project ?: request.project]" >
            <g:message code="gui.menu.Events"/>
        </g:link></li>
        </g:if>

    <g:unless test="${session.frameworkProjects}">
        <g:javascript>
            jQuery(window).load(function(){
                jQuery('#projectSelect').load('${createLink(controller: 'framework', action: 'projectSelect', params: selectParams)}');
            });
        </g:javascript>
    </g:unless>
    </g:if>
</g:if>
</ul>
  <ul class="nav navbar-nav navbar-right">
    <g:set var="helpLinkUrl" value="${g.helpLinkUrl()}"/>
    <g:if test="${session?.user && request.subject}">
        <li class="headright">
            <g:set var="adminauth" value="${false}"/>
            <g:if test="${params.project ?: request.project}">
            <g:set var="adminauth" value="${auth.resourceAllowedTest(type:'project',name: (params.project ?: request.project),action:[AuthConstants.ACTION_ADMIN,AuthConstants.ACTION_READ],context:'application')}"/>
            <g:ifPageProperty name='meta.tabpage'>
                <g:ifPageProperty name='meta.tabpage' equals='configure'>
                    <g:set var="cfgselected" value="active"/>
                </g:ifPageProperty>
            </g:ifPageProperty>
            <g:if test="${adminauth}">
            <li class="${cfgselected ?: ''}">
                <g:link controller="menu" action="admin" title="${g.message(code:'gui.menu.Admin')}"
                        params="[project: params.project?:request.project]">
                    <i class="glyphicon glyphicon-cog"></i>
                </g:link>
            </li>
                <!-- --></g:if><!--
        --></g:if><!--
            -->
        <li class="dropdown">
            <g:link controller="user" action="profile" class="dropdown-toggle" data-toggle="dropdown" data-target="#" id="userLabel"
                    role="button">
                ${session.user.encodeAsHTML()} <span class="caret"></span>
            </g:link>
            <ul class="dropdown-menu" role="menu" aria-labelledby="userLabel">
                <li><g:link controller="user" action="profile">
                        <i class="glyphicon glyphicon-user"></i>
                        Profile
                    </g:link>
                </li>
                <li class="divider"></li>
                <li><g:link action="logout" controller="user" title="Logout user: ${session.user}">
                    <i class="glyphicon glyphicon-remove"></i>
                    Logout
                </g:link>
                </li>
            </ul>
        </li>
        <li>
            <a href="${helpLinkUrl.encodeAsHTML()}" class="help ">
                help <b class="glyphicon glyphicon-question-sign"></b>
            </a>
        </li>
    </g:if>
    <g:else>
        <li >
            <a href="${helpLinkUrl.encodeAsHTML()}" class="help ">
                help <b class="glyphicon glyphicon-question-sign"></b>
            </a>
        </li>
    </g:else>
    </ul>
    </div>
    </div>
</nav>
