<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants; rundeck.Execution" %>
<g:javascript>
/** START history
         *
         */
        function loadHistory(){
            new Ajax.Updater('histcontent',"${createLink(controller: 'reports', action: 'eventsFragment')}",{
                parameters:{compact:true,nofilters:true,jobIdFilter:'${scheduledExecution.id}'},
                evalScripts:true,
                onComplete: function(transport) {
                    if (transport.request.success()) {
                        Element.show('histcontent');
                    }
                }
            });
        }

    function init(){
        <g:if test="${!(grailsApplication.config.rundeck?.gui?.enableJobHoverInfo in ['false', false])}">
        $$('.obs_bubblepopup').each(function(e) {
            new BubbleController(e,null,{offx:-14,offy:null}).startObserving();
        });
        </g:if>
    }
    Event.observe(window,'load',init);

</g:javascript>

<div class="pageTop jobHead extra">
    <g:render template="/scheduledExecution/showHead" model="[scheduledExecution:scheduledExecution,execution:execution,followparams:[mode:followmode,lastlines:params.lastlines]]"/>

    <g:if test="${scheduledExecution.scheduled}">
        <div >
            Next Execution:
    <g:if test="${nextExecution}">
        <img src="${resource(dir: 'images', file: 'icon-clock-small.png')}" alt="" width="16px" height="16px"/>
        <span title="<g:relativeDate atDate='${nextExecution}'/>">
            <g:relativeDate elapsed="${nextExecution}" untilClass="timeuntil"/>
        </span>
    </g:if>
    <g:elseif test="${scheduledExecution.scheduled && !nextExecution}">
        <img src="${resource(dir: 'images', file: 'icon-clock-small.png')}" alt="" width="16px" height="16px"/>
        <span class="warn note" title="Job schedule will never fire">Never</span>
    </g:elseif>
        </div>
    </g:if>
</div>

<div class="pageBody" id="schedExecPage">

    <div style="width: 200px;" class="toolbar ">
        <g:render template="/scheduledExecution/actionButtons"
                  model="${[scheduledExecution: scheduledExecution, objexists: objexists, jobAuthorized: jobAuthorized]}"/>
    </div>
    <div class="clear"></div>

    <g:if test="${flash.message}">
        <div class="message">${flash.message}</div>
    </g:if>
    <g:if test="${message}">
        <div class="message">${message}</div>
    </g:if>
    <div class="pageMessage" id="showPageMessage" style="display: none;"></div>
    <g:render template="/common/messages"/>

</div>

<g:if test="${!jobauthorizations}">
    <%-- evaluate auth for the job on demand --%>
    <g:set var="jobauthorizations" value="${[:]}"/>
    <%
    [AuthConstants.ACTION_DELETE, AuthConstants.ACTION_RUN, AuthConstants.ACTION_READ, AuthConstants.ACTION_UPDATE].each { action ->
    jobauthorizations[action] = auth.jobAllowedTest(job: scheduledExecution, action: action) ? [scheduledExecution.id.toString()] : []
    }
    jobauthorizations[AuthConstants.ACTION_CREATE] = auth.resourceAllowedTest(kind: 'job', action: AuthConstants.ACTION_CREATE)
    %>
</g:if>

<g:set var="jobAuths" value="${jobauthorizations}"/>
<g:if test="${jobAuths[AuthConstants.ACTION_RUN]?.contains(scheduledExecution.id.toString())}">
    <tmpl:execOptionsForm model="[scheduledExecution: scheduledExecution, crontab: crontab, authorized: authorized]"/>

</g:if>

<div class="runbox">
    <g:set var="successcount"
           value="${scheduledExecution.id ? Execution.countByScheduledExecutionAndStatus(scheduledExecution, 'true') : 0}"/>
    <g:set var="execCount"
           value="${scheduledExecution.id ? Execution.countByScheduledExecution(scheduledExecution) : 0}"/>
    <g:set var="successrate" value="${execCount > 0 ? (successcount / execCount) : 0}"/>

    ${execCount} Executions.

    <g:set var="avgDuration" value="${scheduledExecution.execCount > 0 ? scheduledExecution.totalTime / scheduledExecution.execCount : 0}"/>
    <g:if test="${avgDuration>0}">
        Average duration: <g:timeDuration time="${avgDuration}"/>
    </g:if>

</div>

<div class="pageBody">
    <g:each in="${executions}" var="execution">
        <li>
            <g:link controller="execution" action="show" id="${execution.id}" title="View execution output">
                <img
                        src="${resource(dir: 'images', file: 'icon-tiny-' + (execution?.status == 'true' ? 'ok' : 'warn') + '.png')}"
                        alt="" width="12px" height="12px"/>
                %{--${execution.status=='true'?'Success': execution.aborted?'Aborted':'Failed'}--}%
                <g:relativeDate atDate="${execution.dateCompleted}"/>
                <span class="when">
                    (<g:relativeDate elapsed="${execution.dateCompleted}"/>)
                </span>
                by
                <g:if test="${execution.user==session.user}">
                    you
                </g:if>
                <g:else>
                ${execution.user}
                </g:else>
                &raquo;
            </g:link>
        </li>
    </g:each>

</div>
