<%@ page import="rundeck.Execution; com.dtolabs.rundeck.server.authorization.AuthConstants" %>
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="tabpage" content="jobs"/>
    <meta name="layout" content="base" />
    <title><g:message code="main.app.name"/> - <g:if test="${null==execution?.dateCompleted}">Now Running - </g:if><g:if test="${scheduledExecution}">${scheduledExecution?.jobName.encodeAsHTML()} :  </g:if><g:else>Transient <g:message code="domain.ScheduledExecution.title"/> : </g:else> Execution at <g:relativeDate atDate="${execution.dateStarted}" /> by ${execution.user}</title>
    <g:set var="followmode" value="${params.mode in ['browse','tail','node']?params.mode:null==execution?.dateCompleted?'tail':'browse'}"/>
      <g:set var="authKeys" value="${[AuthConstants.ACTION_KILL, AuthConstants.ACTION_READ,AuthConstants.ACTION_CREATE,AuthConstants.ACTION_RUN]}"/>
      <g:set var="authChecks" value="${[:]}"/>
      <g:each in="${authKeys}" var="actionName">
      <g:if test="${execution.scheduledExecution}">
          <%-- set auth values --%>
          %{
              authChecks[actionName]=auth.jobAllowedTest(job:execution.scheduledExecution,action: actionName)
          }%
      </g:if>
      <g:else>
          %{
              authChecks[actionName] = auth.adhocAllowedTest(action: actionName)
          }%
      </g:else>
      </g:each>
      <g:set var="adhocRunAllowed" value="${auth.adhocAllowedTest(action: AuthConstants.ACTION_RUN)}"/>

      <g:set var="defaultLastLines" value="${grailsApplication.config.rundeck.gui.execution.tail.lines.default}"/>
      <g:set var="maxLastLines" value="${grailsApplication.config.rundeck.gui.execution.tail.lines.max}"/>
      <g:javascript src="executionControl.js?v=${grailsApplication.metadata['app.version']}"/>
      <g:javascript library="prototype/effects"/>
      <g:javascript>
        <g:if test="${scheduledExecution}">
        /** START history
         *
         */
        function loadHistory(){
            new Ajax.Updater('histcontent',"${createLink(controller:'reports',action:'eventsFragment')}",{
                parameters:{compact:true,nofilters:true,jobIdFilter:'${scheduledExecution.id}'},
                evalScripts:true,
                onComplete: function(transport) {
                    if (transport.request.success()) {
                        Element.show('histcontent');
                    }
                }
            });
        }
        </g:if>
        var followControl = new FollowControl('${execution?.id}','commandPerform',{
            appLinks:appLinks,
            iconUrl: "${resource(dir: 'images', file: 'icon')}",
            extraParams:"<%="true" == params.disableMarkdown ? '&disableMarkdown=true' : ''%>&markdown=${params.markdown}",
            lastlines: ${params.lastlines ? params.lastlines : defaultLastLines},
            maxLastLines: ${maxLastLines},
            collapseCtx: {value:${null == execution?.dateCompleted },changed:false},

            tailmode: ${followmode == 'tail'},
            browsemode: ${followmode == 'browse'},
            nodemode: ${followmode == 'node'},
            execData: {node:"${session.Framework.getFrameworkNodeHostname()}"},
            <g:if test="${authChecks[AuthConstants.ACTION_KILL]}">
            killjobhtml: '<span class="action button textbtn" onclick="followControl.docancel();">Kill <g:message code="domain.ScheduledExecution.title"/> <img src="${resource(dir: 'images', file: 'icon-tiny-removex.png')}" alt="Kill" width="12px" height="12px"/></span>',
            </g:if>
            <g:if test="${!authChecks[AuthConstants.ACTION_KILL]}">
            killjobhtml: "",
            </g:if>
            totalDuration : 0 + ${scheduledExecution?.totalTime ? scheduledExecution.totalTime : -1},
            totalCount: 0 + ${scheduledExecution?.execCount ? scheduledExecution.execCount : -1}
            <g:if test="${scheduledExecution}">
            , onComplete:loadHistory
            </g:if>
        });


        function init() {
            followControl.beginFollowingOutput('${execution?.id}');
            <g:if test="${!(grailsApplication.config.rundeck?.gui?.enableJobHoverInfo in ['false', false])}">
            $$('.obs_bubblepopup').each(function(e) {
                new BubbleController(e,null,{offx:-14,offy:null}).startObserving();
            });
            </g:if>
          $$('select.obs_followModeSelect').each(function(e){
            Event.observe(e,'change',function(el){
                window.location='${g.createLinkTo(controller: "execution", action: "show", id:execution.id)}'+'?mode='+$F(e);
            });
          });
          $$('.obs_modeSelect').each(function(e){
            Event.observe(e,'click',function(el){
                $(e).hide();
                $$('.show_modeSelect').each(Element.show);
            });
          });
        }

        Event.observe(window, 'load', init);

      </g:javascript>
      <style type="text/css">

        #log{
            margin-bottom:20px;
        }
      </style>
  </head>

  <body>
  <g:if test="${scheduledExecution}">
    <div class="pageTop extra jobHead">
            <g:render template="/scheduledExecution/showHead" model="[scheduledExecution:scheduledExecution,execution:execution,followparams:[mode:followmode,lastlines:params.lastlines]]"/>

        %{--<g:if test="${scheduledExecution}">--}%
            %{--<div style="vertical-align:top;" class="toolbar small">--}%
                %{--<g:render template="/scheduledExecution/actionButtons"--}%
                          %{--model="${[scheduledExecution: scheduledExecution, objexists: objexists, jobAuthorized: jobAuthorized, execPage: true]}"/>--}%
                %{--<g:set var="lastrun"--}%
                       %{--value="${scheduledExecution.id ? Execution.findByScheduledExecutionAndDateCompletedIsNotNull(scheduledExecution, [max: 1, sort: 'dateStarted', order: 'desc']) : null}"/>--}%
                %{--<g:set var="successcount"--}%
                       %{--value="${scheduledExecution.id ? Execution.countByScheduledExecutionAndStatus(scheduledExecution, 'true') : 0}"/>--}%
                %{--<g:set var="execCount"--}%
                       %{--value="${scheduledExecution.id ? Execution.countByScheduledExecution(scheduledExecution) : 0}"/>--}%
                %{--<g:set var="successrate" value="${execCount > 0 ? (successcount / execCount) : 0}"/>--}%
                %{--<g:render template="/scheduledExecution/showStats"--}%
                          %{--model="[scheduledExecution: scheduledExecution, lastrun: lastrun ? lastrun : null, successrate: successrate]"/>--}%
            %{--</div>--}%
        %{--</g:if>--}%
    </div>
  </g:if>
    <div class="pageTop extra execHead ">
        <div class="exec">
            <span class="partContent">
                <g:link
                        title="Permalink for execution ID ${execution?.id}"
                        controller="execution"
                        action="show"
                        id="${execution.id}"
                        absolute="${absolute ? 'true' : 'false'}"
                        params="${followparams?.findAll { it.value }}">
                    Execution
                    <g:if test="${null != execution.dateCompleted}">

                        <span class="${execution.status == 'true' ? 'succeed' : 'fail'}">
                            <g:if test="${execution.status == 'true'}">
                                Successful
                            </g:if>
                            <g:elseif test="${execution.cancelled}">
                                Killed<g:if
                                    test="${execution.abortedby}">by: ${execution.abortedby.encodeAsHTML()}</g:if>
                            </g:elseif>
                            <g:else>
                                Failed
                            </g:else>
                        </span>

                        <g:relativeDate elapsed="${execution.dateCompleted}" agoClass="timeago"/>
                        (<g:relativeDate start="${execution.dateStarted}" end="${execution.dateCompleted}"/>)
                    </g:if>
                    <g:else>
                        <span id="runstatus">
                            <span class="nowrunning">
                                <img src="${resource(dir: 'images', file: 'icon-tiny-disclosure-waiting.gif')}"
                                     alt="Spinner"/>
                                Now Running&hellip;
                            </span>
                        </span>

                        <g:if test="${authChecks[AuthConstants.ACTION_KILL]}">
                            <span id="cancelresult" style="margin-left:10px">
                                <span class="action button textbtn" onclick="followControl.docancel();">Kill <g:message
                                        code="domain.ScheduledExecution.title"/> <img
                                        src="${resource(dir: 'images', file: 'icon-tiny-removex.png')}" alt="Kill"
                                        width="12px" height="12px"/></span>
                            </span>
                        </g:if>

                    </g:else>
                    by <span class="username">${execution.user}</span></g:link>
            </span>



        %{--Download output button--}%
        <span style="${execution.dateCompleted ? '' : 'display:none'}" class="sepL" id="viewoptionscomplete">
            <g:link
                    title="View raw output"
                    controller="execution" action="downloadOutput" id="${execution.id}" params="${[view:'inline',formatted:'false']}">
                raw
            </g:link>
            <g:link class="sepL"
                    title="Download entire output file ${filesize ? filesize + ' bytes' : ''}"
                    controller="execution" action="downloadOutput" id="${execution.id}" >
                &darr;
                %{--<span id="outfilesize"></span> --}%
                download</g:link>
        </span>

            %{--
            Workflow details expander
            --}%

            <g:expander key="schedExDetails${scheduledExecution?.id ? scheduledExecution?.id : ''}"
                        imgfirst="true">Workflow Details</g:expander>


            %{--
            Execution retry buttons
            --}%

            <span id="execRerun" style="${wdgt.styleVisible(if: null != execution.dateCompleted)}">
                <g:if test="${scheduledExecution}">
                    <g:if test="${authChecks[AuthConstants.ACTION_RUN]}">
                        &nbsp;
                        <g:link controller="scheduledExecution"
                                action="execute"
                                id="${scheduledExecution.extid}"
                                params="${[retryExecId: execution.id]}"
                                class="action button"
                                title="Run this Job Again with the same options">
                            <g:img file="icon-small-run.png" alt="run" width="16px" height="16px"/>
                            Run Again &hellip;
                        </g:link>
                    </g:if>
                </g:if>
                <g:else>
                    <g:if test="${auth.resourceAllowedTest(kind: 'job', action: [AuthConstants.ACTION_CREATE]) || adhocRunAllowed}">
                        <g:if test="${!scheduledExecution || scheduledExecution && authChecks[AuthConstants.ACTION_READ]}">
                            <g:link
                                    controller="scheduledExecution"
                                    action="createFromExecution"
                                    params="${[executionId: execution.id]}"
                                    class="action button"
                                    title="Save these Execution parameters as a Job, or run again...">
                                <g:img file="icon-small-run.png" alt="run" width="16px" height="16px"/>
                                Run Again or Save &hellip;
                            </g:link>
                        </g:if>
                    </g:if>
                </g:else>
            </span>
            <span id="execRetry"
                  style="${wdgt.styleVisible(if: null != execution.dateCompleted && null != execution.failedNodeList)}; margin-right:10px;">
                <g:if test="${scheduledExecution}">
                    <g:if test="${authChecks[AuthConstants.ACTION_RUN]}">
                        <g:link controller="scheduledExecution" action="execute" id="${scheduledExecution.extid}"
                                params="${[retryFailedExecId: execution.id]}" title="Run Job on the failed nodes"
                                class="action button" style="margin-left:10px">
                            <img src="${resource(dir: 'images', file: 'icon-small-run.png')}" alt="run" width="16px"
                                 height="16px"/>
                            Retry Failed Nodes  &hellip;
                        </g:link>
                    </g:if>
                </g:if>
                <g:else>
                    <g:if test="${auth.resourceAllowedTest(kind: 'job', action: [AuthConstants.ACTION_CREATE]) || adhocRunAllowed}">
                        <g:link controller="scheduledExecution" action="createFromExecution"
                                params="${[executionId: execution.id, failedNodes: true]}" class="action button"
                                title="Retry on the failed nodes&hellip;" style="margin-left:10px">
                            <img src="${resource(dir: 'images', file: 'icon-small-run.png')}" alt="run" width="16px"
                                 height="16px"/>
                            Retry Failed Nodes &hellip;
                        </g:link>
                    </g:if>
                </g:else>
            </span>



            %{--Workflow Details section--}%
              <div class="presentation" style="display:none" id="schedExDetails${scheduledExecution?.id}">
                  <g:render template="execDetails" model="[execdata: execution]"/>

              </div>



        </div>
        <div class="clear"></div>
    </div>

  <div id="progressContainer" class="progressContainer boxy">
      <div class="progressBar" id="progressBar"
           title="Progress is an estimate based on average execution time for this ${g.message(code: 'domain.ScheduledExecution.title')}.">0%</div>
  </div>

    <div id="commandPerformOpts" class="outputdisplayopts" style=" ;">
        <form action="#" id="outputappendform">

        <table width="100%">
            <tr>
                <td style="text-align: right;">
                %{--
                Follow output controls expander
                --}%
                %{--Follow output controls section--}%

                <span class="presentation show_modeSelect" style="display:none" id="outputControl">
                    <span id="fullviewopts" style="${followmode != 'browse' ? 'display:none' : ''}">

                        &nbsp;
                        <span class="action textbtn"
                              title="Click to change"
                              id="ctxshowlastlineoption"
                              style="${wdgt.styleVisible(if: null == execution?.dateCompleted)}"
                              onclick="followControl.setShowFinalLine($('ctxshowlastline').checked);">
                            <input
                                    type="checkbox"
                                    name="ctxshowlastline"
                                    id="ctxshowlastline"
                                    value="true"
                                    checked="CHECKED"
                                    style=""/>
                            <label for="ctxshowlastline">Show final line</label>
                        </span>
                        <span
                                class="action textbtn"
                                title="Click to change"
                                id="ctxcollapseLabel"
                                onclick="followControl.setCollapseCtx($('ctxcollapse').checked);">
                            <input
                                    type="checkbox"
                                    name="ctxcollapse"
                                    id="ctxcollapse"
                                    value="true"
                                ${followmode == 'tail' ? '' : null == execution?.dateCompleted ? 'checked="CHECKED"' : ''}
                                    style=""/>
                            <label for="ctxcollapse">Collapse</label>
                        </span>
                    </span>
                    <g:if test="${followmode == 'tail'}">

                        Show the last
                        <span class="action textbtn "
                              title="Click to reduce"
                              onmousedown="followControl.modifyLastlines(-5);
                              return false;">-</span>
                        <input
                                type="text"
                                name="lastlines"
                                id="lastlinesvalue"
                                value="${params.lastlines ? params.lastlines : defaultLastLines}"
                                size="3"
                                onchange="updateLastlines(this.value)"
                                onkeypress="var x = noenter();
                                if (!x) {
                                    this.blur();
                                }
                                ;
                                return x;"
                                style=""/>
                        <span class="action textbtn "
                              title="Click to increase"
                              onmousedown="followControl.modifyLastlines(5);
                              return false;">+</span>

                        lines<span id="taildelaycontrol" style="${execution.dateCompleted ? 'display:none' : ''}">,
                    and update every


                        <span class="action textbtn "
                              title="Click to reduce"
                              onmousedown="followControl.modifyTaildelay(-1);
                              return false;">-</span>
                        <input
                                type="text"
                                name="taildelay"
                                id="taildelayvalue"
                                value="1"
                                size="2"
                                onchange="updateTaildelay(this.value)"
                                onkeypress="var x = noenter();
                                if (!x) {
                                    this.blur();
                                }
                                ;
                                return x;"
                                style=""/>
                        <span class="action textbtn "
                              title="Click to increase"
                              onmousedown="followControl.modifyTaildelay(1);
                              return false;">+</span>

                        seconds
                    </span>

                    </g:if>
                </span>

                %{--<g:if test="${followmode != 'node'}">--}%

                    %{--<g:expander key="outputControl"--}%
                                %{-->Output </g:expander>--}%
                %{--</g:if>--}%

                <span class="action textbtn obs_modeSelect"><g:message code="execution.show.mode.${followmode}.label"/></span>
                <select name="mode" class="obs_followModeSelect show_modeSelect" style="display: none;">
                    <option value="tail" ${followmode == 'tail' ? 'selected' : ''}><g:message code="execution.show.mode.tail.label"/></option>
                    <option value="browse" ${followmode == 'browse' ? 'selected' : ''}><g:message code="execution.show.mode.browse.label"/></option>
                    <option value="node" ${followmode == 'node' ? 'selected' : ''}><g:message code="execution.show.mode.node.label"/></option>
                </select>
                </td>
            </tr>
            <tr >
                <td style="text-align: right;">

                </td>
            </tr>
            </table>
        </form>
    </div>
    <div id="fileload2" style="display:none;" class="outputdisplayopts"><img src="${resource(dir:'images',file:'icon-tiny-disclosure-waiting.gif')}" alt="Spinner"/> Loading Output... <span id="fileloadpercent"></span></div>
    <div
        id="commandPerform"
        style="display:none; ; "></div>
    <div id="fileload" style="display:none;" class="outputdisplayopts"><img src="${resource(dir:'images',file:'icon-tiny-disclosure-waiting.gif')}" alt="Spinner"/> Loading Output... <span id="fileload2percent"></span></div>
    <div id="log"></div>

  </body>
</html>


