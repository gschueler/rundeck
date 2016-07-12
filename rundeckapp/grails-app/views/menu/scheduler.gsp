<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: 7/8/16
  Time: 10:45 AM
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html><head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="configure"/>
    <title><g:message code="gui.menu.Scheduler" default="Scheduler"/></title>
    <asset:javascript src="menu/scheduler"/>

    <g:javascript>

        SchedulerInfo.init = function (opts) {
            "use strict";
            var viewvm = new SchedulerInfo(opts);
            jQuery(function () {
                "use strict";
                ko.applyBindings(viewvm);
                viewvm.load();
            });
            viewvm.update();
            return viewvm;
        };
        var schedulerInfo = SchedulerInfo.init({url:
        "${createLink(action: 'listSchedules')}",
        urlOpts:{
        // project:"${params.project}"
        }
        });
        jQuery(function(){
        "use strict";

            jQuery('table').on('click','.act_job_action_dropdown',function(){
                var id=jQuery(this).data('jobId');
                var el=jQuery(this).parent().find('.dropdown-menu');
                el.load(
                    _genUrl(appLinks.scheduledExecutionActionMenuFragment,{id:id})
                );
            });
        });
    </g:javascript>
</head>

<body>
<div class="row">
    <div class="col-sm-3">
        <g:render template="configNav" model="[selected: 'scheduler']"/>
    </div>

    <div class="col-sm-9">
        <h1>Scheduler</h1>
        <g:ifServletContextAttributeExists attribute="SERVER_UUID">
            <div class="well well-sm  well-embed">
                Scheduled Jobs for this cluster node

                <div class=" rundeck-server-uuid"
                     data-server-uuid="${enc(attr: servletContextAttribute(attribute: 'SERVER_UUID'))}"
                     data-server-name="${enc(attr: servletContextAttribute(attribute: 'FRAMEWORK_NODE'))}">
                </div>
                ${servletContextAttribute(attribute: 'SERVER_UUID')}
            </div>
        </g:ifServletContextAttributeExists>

        <table class="table table-bordered table-condensed">
            <thead>
            <tr>
                <th class="th-muted text-small">Project</th>
                <th class="th-muted text-small">Job</th>
                <th class="th-muted text-small">Schedule</th>
                <th class="th-muted text-small">Cron</th>
            </tr>
            </thead>
            <tbody data-bind="foreach: scheduledJobs">
            <tr>
                <td>
                    <span data-bind="text: project"></span>
                </td>
                <td>
                    <span data-bind="if: executionEnabled(), bootstrapTooltip: true"
                          class="text-success"
                          title="Execution is enabled">
                        <g:icon name="play-circle"/>
                    </span>
                    <span data-bind="if: !executionEnabled(), bootstrapTooltip: true"
                          class="text-muted"
                          title="Execution is disabled">
                        <g:icon name="ban-circle"/>
                    </span>
                    <a href="${g.createLink(
                            controller: 'scheduledExecution',
                            action: 'show',
                            params: [project: '<$>', id: '<$>']
                    )}"
                       data-bind="urlPathParam: [ project(), jobId() ]">
                        <g:icon name="book"/>
                        <span data-bind="text: jobTitle"></span>
                    </a>

                    <div class="btn-group">
                        <button type="button"
                                class="btn btn-default btn-sm btn-link dropdown-toggle act_job_action_dropdown"
                                title="${g.message(code: 'click.for.job.actions')}"
                                data-bind="attr: {'data-job-id': jobId }"
                                data-toggle="dropdown"
                                aria-expanded="false">
                            <span class="caret"></span>
                        </button>
                        <ul class="dropdown-menu" role="menu">
                            <li role="presentation" class="dropdown-header"><g:message code="loading.text"/></li>
                        </ul>
                    </div>
                </td>
                <td data-bind="css: {'text-muted': !scheduleEnabled() }">

                    <span data-bind="if: scheduleEnabled(), bootstrapTooltip: true, css: { 'text-success': scheduleEnabled()&&executionEnabled() } "
                          title="Schedule is enabled">
                        <g:icon name="time"/>
                    </span>
                    <span data-bind="if: !scheduleEnabled(), bootstrapTooltip: true"
                          title="Schedule is disabled">
                        <g:icon name="ban-circle"/>
                    </span>
                    <span data-bind="if: nextExecution" >
                        <span data-bind="text: nextExecDate, attr: {title: nextExecDateFormat}"></span>
                        <span data-bind="text: '('+relativeNextExecDate()+')', attr: {title: nextExecDateFormat}"></span>
                    </span>
                </td>
                <td>
                    <code>
                        <span data-bind="text: crontab"></span>
                    </code>
                </td>
            </tr>
            </tbody>
        </table>

    </div>
</div>

</body>
</html>