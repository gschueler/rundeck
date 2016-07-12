<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: 3/25/15
  Time: 4:16 PM
--%>

<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="tabpage" content="jobs"/>
    <meta name="layout" content="base"/>
    <title><g:appTitle/> - <g:message
            code="ScheduledExecution.page.delete.title"/></title>

</head>

<body>
<g:form controller="scheduledExecution" useToken="true"
        action="${flipSchedule ? 'flipScheduleEnabled' : 'flipExecutionEnabled'}" method="post"
        params="[project: scheduledExecution.project, id: scheduledExecution.extid]"
        class="form form-horizontal">
    <div class="panel panel-primary">
        <div class="panel-heading">
            <span class="h3">

                <g:if test="${flipSchedule}">
                    <g:if test="${scheduledExecution.scheduleEnabled}">
                        <g:message code="job.bulk.disable.schedule.confirm.message"/>
                    </g:if>
                    <g:else>
                        <g:message code="job.bulk.enable.schedule.confirm"/>
                    </g:else>
                </g:if>
                <g:else>
                    <g:message code="job.bulk.${scheduledExecution.executionEnabled ? 'dis' :
                            'en'}able.execution.confirm"/>
                </g:else>
            </span>
        </div>

        <div class="panel-body">
            <div class="form-group">
                <label class="col-sm-2 control-label">
                    <g:message code="scheduledExecution.jobName.label"/>
                </label>

                <div class="col-sm-10">
                    <p class="form-control-static text-info">
                        ${scheduledExecution.generateFullName()}
                    </p>
                </div>
            </div>

        </div>

        <div class="panel-footer">
            <g:if test="${flipSchedule}">
                <g:hiddenField name="scheduleEnabled" value="${!scheduledExecution.scheduleEnabled}"/>
            </g:if>
            <g:else>
                <g:hiddenField name="executionEnabled" value="${!scheduledExecution.executionEnabled}"/>
            </g:else>
            <g:hiddenField name="returnToScheduler" value="true"/>
            <g:actionSubmit value="Cancel" action="cancel" class="btn btn-default "/>
            %{--<input type="submit" value="Enable Schedule" class="btn btn-danger btn-sm"/>--}%
            <g:submitButton name="submit"
                            value="${message(code:
                                                     flipSchedule ?
                                                             scheduledExecution.scheduleEnabled ?
                                                                     'job.bulk.disable.schedule.button' :
                                                                     'job.bulk.enable.schedule.button' :
                                                             scheduledExecution.executionEnabled ?
                                                                     'scheduledExecution.action.disable.execution.button.label' :
                                                                     'scheduledExecution.action.enable.execution.button.label'
                            )}"
                            class="btn btn-danger"/>
        </div>
    </div>
</g:form>
</body>
</html>