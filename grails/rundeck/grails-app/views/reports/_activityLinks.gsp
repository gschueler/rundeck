%{--
  Copyright 2013 SimplifyOps Inc, <http://simplifyops.com>

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  --}%

<g:set var="projParams" value="${[project: project ?: params.project ?: request.project]}"/>
<g:set var="linkParams" value="${filter?filter+projParams:projParams}"/>
<g:set var="runningParams" value="${filter ? filter + projParams : projParams}"/>
<g:if test="${scheduledExecution}">
    <g:set var="linkParams" value="${[jobIdFilter: scheduledExecution.id]+projParams}"/>
    <g:set var="runningParams" value="${[jobIdFilter: scheduledExecution.extid]+projParams}"/>
</g:if>
<ul class="nav nav-tabs activity_links">
    %{--<g:unless test="${hideNowRunning}">--}%
    <li data-bind="css: { disabled: !nowRunningEnabled() }">
        <g:link controller="reports" action="index" class="running_link"
                title="All activity for this job"
                data-auto-refresh="2"
                params="${runningParams + [runningFilter: 'running']}">
            <i class="glyphicon glyphicon-play-circle"></i>
            running
        </g:link>
    </li>
    %{--</g:unless>--}%
    <li>
        <g:link controller="reports" action="index" class="activity_link"
                title="All activity for this job"
                params="${linkParams}">
            <i class="glyphicon glyphicon-time"></i>
            recent
        </g:link>
    </li>

    <li>
        <g:link controller="reports" action="index" class="activity_link"
                title="Failed executions"
                params="${linkParams+[ statFilter: 'fail']}">
            <i class="glyphicon glyphicon-minus-sign"></i>
            failed
        </g:link>
    </li>

    <g:if test="${!execution || execution.user != session.user}">
        <li>
            <g:link controller="reports" action="index" class="activity_link"
                    title="Executions by you"
                    params="${linkParams+[ userFilter: session.user]}">
                <i class="glyphicon glyphicon-user"></i>
                by you
            </g:link>
        </li>
    </g:if>

    <g:if test="${execution}">
        <li>
            <g:link controller="reports" action="index" class="activity_link"
                    title="Executions by ${execution.user.encodeAsHTML()}"
                    params="${linkParams+[ userFilter: execution.user]}">
                <i class="glyphicon glyphicon-user"></i>
                by <g:username user="${execution.user}"/>
            </g:link>
        </li>
    </g:if>

</ul>
<g:if test="${knockoutBinding}">

<div data-bind="visible: selected()"  class="panel panel-default panel-tab-content" style="display: none;">
    <table class=" table table-hover table-condensed events-table"
           style="width:100%; display: none"
           data-bind="visible: results().length > 0">
        <tbody ></tbody>
        <tbody data-bind=" foreach: results ">
        <tr class="link activity_row"
            data-bind="css: { 'succeed': status()=='succeed', 'fail': status()=='fail', 'highlight': $root.highlightExecutionId()==executionId() } "
            onclick="$(this).down('a._defaultAction').click();"
            >
            <td style="width:12px;" class="eventicon">
                <i class="exec-status icon"
                   data-bind="css: { 'succeed': status()=='succeed', 'fail': status()=='fail', 'warn': status()=='cancel', 'running': status()=='running' } "
                ></i>
            </td>
            <td class="eventtitle" data-bind="css: { job: jobId(), adhoc: !jobId() }">
                <a href="#" data-bind="text: '#'+executionId(), attr: { href: executionHref() }" class="_defaultAction"></a>
                <g:if test="${showTitle}">
                    <span data-bind="text: jobId()?jobName():executionString()"></span>
                </g:if>
            </td>
            <td class="eventargs" >
                <div class="argstring-scrollable">
                <span data-bind="if: execution().jobArguments">
                    <span data-bind="foreachprop: execution().jobArguments">
                        <span data-bind="text: key"></span>:
                        <span data-bind="text: value" class="optvalue"></span>
                    </span>
                </span>
                <!-- ko if: !execution().jobArguments -->
                <span data-bind="text: execution().argString"></span>
                <!-- /ko -->
                </div>
            </td>
            <td style="white-space:nowrap" class="right date">
                <span data-bind="if: dateCompleted()">
                    <span class="timeabs" data-bind="text: endTimeFormat('${g.message(code:'jobslist.date.format.ko')}')">

                    </span>
                    <span title="">
                        <span class="text-muted">in</span>
                        <span class="duration" data-bind="text: durationHumanize()"></span>
                    </span>
                </span>
                <span data-bind="if: !dateCompleted()">
                    <div data-bind="if: !jobId() || jobAverageDuration()==0">
                    <g:render template="/common/progressBar" model="${[
                            indefinite: true, title: 'Running', innerContent: 'Running', width: 120,
                            progressClass: 'rd-progress-exec progress-striped active indefinite progress-embed',
                            progressBarClass: 'progress-bar-info',
                    ]}"/>
                    </div>
                    <div data-bind="if: jobId() && jobAverageDuration()>0">
                        <g:render template="/common/progressBar"
                                  model="[completePercent: 0,
                                          progressClass: 'rd-progress-exec progress-embed',
                                          progressBarClass: '',
                                          containerId: 'progressContainer2',
                                          innerContent: '',
                                          showpercent: true,
                                          progressId: 'progressBar',
                                          bind: 'jobPercentageFixed()',
                                          bindText: '(jobPercentageFixed()  < 105 ? jobPercentageFixed() + \'%\' : \'+\' + jobOverrunDuration()) + \' of average \' + MomentUtil.formatDurationHumanize(jobAverageDuration())',
                                          progressBind: ', css: { \'progress-bar-info\': jobPercentageFixed() < 105 ,  \'progress-bar-warning\': jobPercentageFixed() &gt; 104  }',
                                  ]"/>
                    </div>
                </span>
            </td>

            <td class="  user text-right" style="white-space: nowrap;">
                <em>by</em>
                <span data-bind="text: user"></span>
            </td>


        </tr>
        </tbody>
    </table>


    <div data-bind="visible: selected() && results().length < 1 " class="panel-body" style="display: none;">
        <span class="text-muted" data-bind="if: !showReports()">No running executions found</span>
        <span class="text-muted" data-bind="if: showReports()">No matching activity found</span>
    </div>

    <div data-bind="visible: selected()" class="panel-footer" style="display: none">
            <ul class="pagination pagination-sm pagination-embed" data-bind="foreach: pageCount() > 1 ? pages() : []">
                <li data-bind="css: { active: $data.currentPage, disabled: $data.disabled } ">
                    <a data-bind="attr: { href: ($data.skipped||$data.disabled||$data.currentPage)?'#':$data.url },
                    click: function(){$data.skipped||$data.disabled||$data.currentPage?null:$root.visitPage($data);}">
                        <span data-bind="if: $data.nextPage">
                            <i class="glyphicon glyphicon-arrow-right"></i>
                        </span>
                        <span data-bind="if: $data.prevPage">
                            <i class="glyphicon glyphicon-arrow-left"></i>
                        </span>
                        <span data-bind="if: $data.skipped">
                            …
                        </span>
                        <span data-bind="if: $data.normal">
                            <span data-bind="text: $data.page">

                            </span>
                        </span>
                    </a>
                </li>
            </ul>
        <span data-bind="if: max() > 0" class="text-info">
            showing
            <span data-bind="text: results().length + ' of ' + total()"></span>
        </span>
        <a href="#" class="textbtn textbtn-default" data-bind="attr: { href: href() } ">
            Filter activity…
            <i class="glyphicon glyphicon-search"></i>
        </a>

    </div>
</div>

</g:if>
