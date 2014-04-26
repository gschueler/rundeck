<%@ page import="grails.util.Environment; rundeck.Execution; com.dtolabs.rundeck.server.authorization.AuthConstants" %>
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="tabpage" content="events"/>
    <meta name="layout" content="base" />
    <title><g:message code="main.app.name"/> - <g:if test="${null==execution?.dateCompleted}">Now Running - </g:if><g:if test="${scheduledExecution}">${scheduledExecution?.jobName.encodeAsHTML()} :  </g:if><g:else><g:message code="execution.type.adhoc.title" /></g:else> Execution at <g:relativeDate atDate="${execution.dateStarted}" /> by ${execution.user}</title>
    <g:set var="followmode" value="${params.mode in ['browse','tail','node']?params.mode:'tail'}"/>
    <g:set var="execState" value="${execution.dateCompleted == null ? 'RUNNING' : execution.status == 'true' ? 'SUCCEEDED' : execution.cancelled ? 'ABORTED' : 'FAILED'}"/>
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
              authChecks[actionName] = auth.adhocAllowedTest(action: actionName,project:execution.project)
          }%
      </g:else>
      </g:each>
      <g:set var="adhocRunAllowed" value="${auth.adhocAllowedTest(action: AuthConstants.ACTION_RUN,project:execution.project)}"/>

      <g:set var="defaultLastLines" value="${grailsApplication.config.rundeck.gui.execution.tail.lines.default}"/>
      <g:set var="maxLastLines" value="${grailsApplication.config.rundeck.gui.execution.tail.lines.max}"/>
      <g:javascript src="executionControl.js"/>
      <g:javascript src="workflow.js"/>
      <g:javascript src="executionState.js"/>
      <asset:javascript src="executionState_HistoryKO.js"/>
      <g:javascript library="prototype/effects"/>
      <g:javascript>

        var workflowData=${execution.workflow.commands*.toMap().encodeAsJSON()};
        var workflow = new RDWorkflow(workflowData,{
            nodeSteppluginDescriptions:${stepPluginDescriptions.node.collectEntries { [(it.key): [title: it.value.title]] }.encodeAsJSON()},
            wfSteppluginDescriptions:${stepPluginDescriptions.workflow.collectEntries { [(it.key): [title: it.value.title]] }.encodeAsJSON()}
        });

        var followControl = new FollowControl('${execution?.id}','outputappendform',{
            parentElement:'commandPerform',
            fileloadId:'fileload',
            fileloadPctId:'fileloadpercent',
            fileloadProgressId:'fileloadprogress',
            viewoptionsCompleteId:'viewoptionscomplete',
            cmdOutputErrorId:'cmdoutputerror',
            outfileSizeId:'outfilesize',
            workflow:workflow,
            appLinks:appLinks,
            iconUrl: "${resource(dir: 'images', file: 'icon-small')}",
            smallIconUrl: "${resource(dir: 'images', file: 'icon-small')}",
            extraParams:"<%="true" == params.disableMarkdown ? '&disableMarkdown=true' : ''%>&markdown=${params.markdown}",
            lastlines: ${params.lastlines ? params.lastlines : defaultLastLines},
            maxLastLines: ${params.maxlines ? params.maxlines : maxLastLines},
            collapseCtx: {value:${null == execution?.dateCompleted },changed:false},
            showFinalLine: {value:false,changed:false},
            tailmode: ${followmode == 'tail'},
            browsemode: ${followmode == 'browse'},
            nodemode: ${followmode == 'node'},
            execData: {},
            groupOutput:{value:${followmode == 'browse'}},
            updatepagetitle:${null == execution?.dateCompleted},
            <g:if test="${authChecks[AuthConstants.ACTION_KILL]}">
            killjobhtml: '<span class="btn btn-danger btn-sm textbtn" onclick="followControl.docancel();">Kill <g:message code="domain.ScheduledExecution.title"/> <i class="glyphicon glyphicon-remove"></i></span>',
            </g:if>
            <g:if test="${!authChecks[AuthConstants.ACTION_KILL]}">
            killjobhtml: "",
            </g:if>
            totalDuration : 0 + ${scheduledExecution?.totalTime ? scheduledExecution.totalTime : -1},
            totalCount: 0 + ${scheduledExecution?.execCount ? scheduledExecution.execCount : -1}
        });
        var flowState = new FlowState('${execution?.id}','flowstate',{
            workflow:workflow,
            loadUrl: "${g.createLink(controller: 'execution', action: 'ajaxExecState', id: execution.id)}",
            outputUrl:"${g.createLink(controller: 'execution', action: 'tailExecutionOutput', id: execution.id)}.json",
            selectedOutputStatusId:'selectedoutputview',
            reloadInterval:1500
         });
         var stepState= new StepFlow(flowState,'flowstate');

         var nodeflowvm=new NodeFlowViewModel(workflow,"${g.createLink(controller: 'execution', action: 'tailExecutionOutput', id: execution.id)}.json");
         function followOutput(){
            followControl.beginFollowingOutput('${execution?.id}');
         }
         function followState(){
            try{
                flowState.beginFollowing();
            }catch(e){
                nodeflowvm.errorMessage('Could not load flow state: '+e);
                nodeflowvm.stateLoaded(false);
            }
         }
         function showTab(id){
            jQuery('#'+id+' a').tab('show');
         }
        function init() {
//            flowState.addUpdater(stepState);
            flowState.addUpdater({
            updateError:function(error,data){
                nodeflowvm.stateLoaded(false);
                if(error!='pending'){
                    nodeflowvm.errorMessage(data.state.errorMessage?data.state.errorMessage:error);
                }else{
                    nodeflowvm.statusMessage(data.state.errorMessage?data.state.errorMessage:error);
                }
                ko.mapping.fromJS({
                    executionState:data.executionState,
                    completed:data.completed,
                    execDuration:data.execDuration,
                    jobAverageDuration:data.jobAverageDuration,
                    startTime:data.startTime? data.startTime : data.state ? data.state.startTime: null,
                    endTime:data.endTime ? data.endTime : data.state ? data.state.endTime : null
                },{},nodeflowvm);
            },
            updateState:function(data){
                ko.mapping.fromJS({
                    executionState:data.executionState,
                    completed:data.completed,
                    execDuration:data.execDuration,
                    jobAverageDuration:data.jobAverageDuration,
                    startTime:data.startTime? data.startTime : data.state ? data.state.startTime: null,
                    endTime:data.endTime ? data.endTime : data.state ? data.state.endTime : null
                },{},nodeflowvm);

                nodeflowvm.updateNodes(data.state);
            }});
            ko.mapping.fromJS({
                completed:${execution.dateCompleted!=null},
                startTime:'${execution.dateStarted.encodeAsJavaScript()}',
                endTime:'${execution.dateCompleted?.encodeAsJavaScript()}',
                executionState:'${execState}'
            },{},nodeflowvm);
            ko.applyBindings(nodeflowvm,jQuery('#execution_main')[0]);

            <g:if test="${!(grailsApplication.config.rundeck?.gui?.enableJobHoverInfo in ['false', false])}">
            $$('.obs_bubblepopup').each(function(e) {
                new BubbleController(e,null,{offx:-14,offy:null}).startObserving();
            });
            </g:if>

            //link flow and output tabs to initialize following
            //by default show state
            followState();
            jQuery('#tab_link_summary').on('show.bs.tab',function(e){
                followState();
            });
            jQuery('#tab_link_flow').on('show.bs.tab',function(e){
                followState();
            });
            jQuery('#tab_link_output').on('show.bs.tab',function(e){
                followOutput();
            });
        }
        var activity;
        jQuery(document).ready(function(){
            init();
            if(document.getElementById('activity_section')){
                activity = new History(appLinks.reportsEventsAjax, appLinks.menuNowrunningAjax);
                activity.nowRunningEnabled(${null != execution?.dateCompleted});
                //enable now running activity tab once execution completes
                activity.highlightExecutionId("${execution.id}");
                nodeflowvm.completed.subscribe(activity.nowRunningEnabled);
                ko.applyBindings(activity, document.getElementById('activity_section'));
                setupActivityLinks('activity_section', activity);
           }
        });
      </g:javascript>
      <style type="text/css">

        #log{
            margin-bottom:20px;
        }

        .inline_only {
            display: none;
        }

        .execstate.isnode[data-execstate=RUNNING],.execstate.isnode[data-execstate=RUNNING_HANDLER] {
            background-image: url(${g.resource(dir: 'images',file: 'icon-tiny-disclosure-waiting.gif')});
            padding-right: 16px;
            background-repeat: no-repeat;
            background-position: right 2px;
        }



        .errmsg {
            color: gray;
        }
      </style>
  </head>

<g:set var="isAdhoc" value="${!scheduledExecution && execution.workflow.commands.size() == 1}"/>
  <body id="executionShowPage">
    <div id="execution_main">
        <div class="executionshow_wrap" data-affix="wrap">
        <div class="executionshow" data-affix="top" data-affix-padding-top="21">
            <div class="row">
                    %{--permalink--}%
                    <div class="col-sm-2">
                        <span class="jobInfo" id="jobInfo_${execution.id}">
                            <span class="h3">
                                <g:render template="/scheduledExecution/showExecutionLink"
                                     model="[scheduledExecution: scheduledExecution, noimgs:true, execution: execution, followparams: [mode: followmode, lastlines: params.lastlines]]"/>
                            </span>
                        </span>
                        <g:if test="${eprev || enext}">
                            <div class="affixed-hidden">

                                <ul class="pager pager-embed pager-left pager-sm">
                                    <g:if test="${eprev}">
                                        <li>
                                            <g:link action="show" controller="execution" id="${eprev.id}"
                                                params="[project: eprev.project]"
                                                    title="Previous Execution #${eprev.id}">
                                                <i class="glyphicon glyphicon-arrow-left"></i>
                                                <g:message
                                                        code="${scheduledExecution ? 'job' : 'adhoc'}.previous.execution"
                                                        args="${[eprev.id]}"/>
                                            </g:link>
                                        </li>
                                    </g:if>
                                    <g:else>
                                        <li class="disabled">
                                            <span><g:message code="no.previous.executions"/></span>
                                        </li>
                                    </g:else>
                                    <g:if test="${enext}">
                                        <li>
                                            <g:link action="show" controller="execution"
                                                    title="Next Execution #${enext.id}"
                                                    params="[project: enext.project]"
                                                    id="${enext.id}">
                                                <g:message code="${scheduledExecution ? 'job' : 'adhoc'}.next.execution"
                                                           args="${[enext.id]}"/>
                                                <i class="glyphicon glyphicon-arrow-right"></i>
                                            </g:link>
                                        </li>
                                    </g:if>
                                    <g:else>
                                        <li class="disabled">
                                            <span><g:message code="no.more.executions"/></span>
                                        </li>
                                    </g:else>
                                </ul>

                            </div>
                        </g:if>
                    </div>

                    %{--job or adhoc title--}%
                    <div class="col-sm-6">

                                <g:if test="${scheduledExecution}">
                                    <div class="row">
                                        <g:render template="/scheduledExecution/showHead"
                                              model="${[scheduledExecution: scheduledExecution, ]}"/>
                                    </div>
                                </g:if>
                                <g:if test="${execution.argString}">
                                    <div class="row">
                                        <div class="col-sm-12" >
                                            <div class="argstring-scrollable">
                                            <span class="text-muted">Options:</span>
                                            <g:render template="/execution/execArgString" model="[argString:execution.argString]"/>
                                            </div>
                                        </div>
                                    </div>
                                </g:if>
                                <g:if test="${isAdhoc}">
                                    <div class="row">
                                        <div class="col-sm-12">
                                            <g:render template="wfItemView" model="[
                                                    item:execution.workflow.commands[0],
                                                    icon:'icon-med',
                                                    iwidth:'24px',
                                                    iheight:'24px',
                                            ]"/>
                                        </div>
                                    </div>
                                </g:if>

                            </div>

                    %{--buttons--}%
                            <div class="col-sm-4">

                                <g:if test="${null == execution.dateCompleted}">
                                    <g:if test="${authChecks[AuthConstants.ACTION_KILL]}">
                                        <div class="pull-right">
                                        <span id="cancelresult" style="margin-left:10px"
                                              data-bind="visible: !completed()">
                                            <span class="btn btn-danger btn-sm"
                                                  onclick="followControl.docancel();">Kill <g:message
                                                    code="domain.ScheduledExecution.title"/>
                                                <i class="glyphicon glyphicon-remove"></i>
                                            </span>
                                        </span>
                                        </div>
                                    </g:if>
                                </g:if>
                                %{--adhoc--}%
                                <g:if test="${!scheduledExecution}">
                                <div class="btn-group pull-right">
                                    %{--save as job link--}%
                                    <g:if test="${auth.resourceAllowedTest(kind: 'job', action: [AuthConstants.ACTION_CREATE],project:execution.project)}">
                                        <g:link
                                                controller="scheduledExecution"
                                                action="createFromExecution"
                                                params="${[executionId: execution.id,project:execution.project]}"
                                                class=" btn btn-primary btn-sm header execRerun execRetry"
                                                title="${g.message(code: 'execution.action.saveAsJob')}"
                                                style="${wdgt.styleVisible(if: null != execution.dateCompleted)}"
                                                data-bind="visible: completed()"
                                        >
                                            <g:message code="execution.action.saveAsJob"
                                                       default="Save as Job"/>&hellip;
                                        </g:link>
                                    </g:if>
                                    %{--run again links--}%
                                    <g:if test="${adhocRunAllowed}">
                                        %{--run again only--}%
                                    <g:link
                                            controller="framework"
                                            action="adhoc"
                                            params="${[fromExecId: execution.id, project: execution.project]}"
                                            title="${g.message(code: 'execution.action.runAgain')}"
                                            class="btn btn-default btn-sm force-last-child execRerun"
                                            style="${wdgt.styleVisible(if: null != execution.dateCompleted && null == execution.failedNodeList)}"
                                            data-bind="visible: completed() && !failed()"
                                    >

                                        <b class="glyphicon glyphicon-play"></b>
                                        <g:message code="execution.action.runAgain"/>&hellip;
                                    </g:link>
                                        %{--run again and retry failed --}%
                                    <div class="btn-group execRetry"
                                         style="${wdgt.styleVisible(if: null != execution.dateCompleted && null!=execution.failedNodeList )}"
                                         data-bind="visible: failed()"
                                    >
                                        <button class="btn btn-default btn-sm dropdown-toggle force-last-child" data-target="#"
                                                data-toggle="dropdown">
                                            Run Again
                                            <i class="caret"></i>
                                        </button>
                                        <ul class="dropdown-menu pull-right" role="menu">
                                            <li >
                                                    <g:link
                                                            controller="framework"
                                                            action="adhoc"
                                                            params="${[fromExecId: execution.id, project: execution.project]}"
                                                            title="${g.message(code: 'execution.action.runAgain')}">

                                                        <b class="glyphicon glyphicon-play"></b>
                                                        <g:message code="execution.action.runAgain"/>&hellip;
                                                    </g:link>
                                            </li>
                                            <li class="divider  ">

                                            </li>
                                            <li>
                                                    <g:link
                                                            controller="framework"
                                                            action="adhoc"
                                                            params="${[retryFailedExecId: execution.id, project: execution.project]}"
                                                            title="${g.message(code: 'retry.failed.nodes.description')}">

                                                        <b class="glyphicon glyphicon-play"></b>
                                                        <g:message code="retry.failed.nodes"/>&hellip;
                                                    </g:link>
                                            </li>
                                        </ul>
                                    </div>
                                    </g:if>

                                </div>
                                </g:if>
                                <g:else>
                                %{--job--}%
                                <div class="pull-right">
                                    <g:if test="${!groupOnly && auth.jobAllowedTest(job: scheduledExecution, action: AuthConstants.ACTION_UPDATE)}">
                                        <g:link controller="scheduledExecution" title="Edit Job" action="edit"
                                                       id="${scheduledExecution.extid}"
                                                       params="[project:scheduledExecution.project]"
                                                       class="btn btn-info btn-sm">
                                            <i class="glyphicon glyphicon-edit"></i>
                                            edit Job
                                        </g:link>
                                    </g:if>
                                    <g:if test="${authChecks[AuthConstants.ACTION_RUN]}">
                                        %{--Run again link--}%
                                        <g:link controller="scheduledExecution"
                                                action="execute"
                                                id="${scheduledExecution.extid}"
                                                class="btn btn-default btn-sm execRerun"
                                                params="${[retryExecId: execution.id, project: execution.project]}"
                                                title="${g.message(code: 'execution.job.action.runAgain')}"
                                                style="${wdgt.styleVisible(if: null != execution.dateCompleted && null == execution.failedNodeList)};"
                                            data-bind="visible: completed() && !failed()"
                                        >
                                            <b class="glyphicon glyphicon-play"></b>
                                            <g:message code="execution.action.runAgain"/>&hellip;
                                        </g:link>
                                        %{--Run again and retry failed links in a dropdown --}%
                                        <div class="btn-group execRetry"
                                             style="${wdgt.styleVisible(if: null != execution.dateCompleted && null != execution.failedNodeList)};"
                                             data-bind="visible: failed()"
                                        >
                                            <button class="btn btn-default btn-sm dropdown-toggle"
                                                    data-target="#"
                                                    data-toggle="dropdown">
                                                Run Again
                                                <i class="caret"></i>
                                            </button>
                                            <ul class="dropdown-menu pull-right" role="menu">
                                                <li class="retrybuttons">
                                                    <g:link controller="scheduledExecution"
                                                            action="execute"
                                                            id="${scheduledExecution.extid}"
                                                            params="${[retryExecId: execution.id, project: execution.project]}"
                                                            title="${g.message(code: 'execution.job.action.runAgain')}"
                                                            data-bind="visible: completed()"
                                                    >
                                                        <b class="glyphicon glyphicon-play"></b>
                                                        <g:message code="execution.action.runAgain"/>&hellip;
                                                    </g:link>
                                                </li>
                                                <li class="divider">

                                                </li>
                                                <li class="retrybuttons">
                                                    <g:link controller="scheduledExecution" action="execute"
                                                            id="${scheduledExecution.extid}"
                                                            params="${[retryFailedExecId: execution.id, project: execution.project]}"
                                                            title="${g.message(code: 'retry.job.failed.nodes')}">
                                                        <b class="glyphicon glyphicon-play"></b>
                                                        <g:message code="retry.failed.nodes"/>&hellip;
                                                    </g:link>
                                                </li>
                                            </ul>
                                        </div>

                                    </g:if>

                                </div>
                                </g:else>
                                <div class="affixed-shown pull-right">
                                    <a class="textbtn textbtn-default textbtn-on-hover btn-xs" href="#top">
                                        <g:message code="scroll.to.top" />
                                        <i class="glyphicon glyphicon-arrow-up"></i>
                                    </a>
                                </div>
                            </div>

            </div>

        <div >


            <div class="row row-space" data-bind="if: !completed()">
                <div class="col-sm-2 text-right">
                    Elapsed Time:
                </div>

                <div class="col-sm-6">
                    <section class="runstatus " data-bind="if: !completed() && jobAverageDuration()>0">
                        <g:render template="/common/progressBar"
                                  model="[completePercent: execution.dateCompleted ? 100 : 0,
                                          progressClass: 'rd-progress-exec progress-embed',
                                          progressBarClass: '',
                                          containerId: 'progressContainer2',
                                          innerContent: '',
                                          showpercent: true,
                                          progressId: 'progressBar',
                                          bind: 'jobPercentageFixed()',
                                          bindText: '(jobPercentageFixed()  < 105 ? jobPercentageFixed() + \'%\' : \'+\' + jobOverrunDuration()) + \' of average \' + formatDurationHumanize(jobAverageDuration())',
                                          progressBind: ', css: { \'progress-bar-info\': jobPercentageFixed() < 105 ,  \'progress-bar-warning\': jobPercentageFixed() &gt; 104  }',
                                  ]"/>
                    </section>
                    <div data-bind="if: completed() || jobAverageDuration() <= 0 ">
                        <span data-bind="text: execDurationSimple()"></span>
                    </div>
                </div>
            </div>
        </div>

        </div>
        </div>
            <div class="row row-space clearfix">
                <div class="col-sm-12">

                    <ul class="nav nav-tabs">
                        <li id="tab_link_summary" class="active">
                            <a href="#summary" data-toggle="tab">Summary</a>
                        </li>
                        <li id="tab_link_flow">
                            <a href="#state" data-toggle="tab" data-bind="text: completed()?'Report':'Monitor' ">
                                <g:if test="${execution.dateCompleted==null}">
                                    Monitor
                                </g:if>
                                <g:else>
                                    Report
                                </g:else>
                            </a>
                        </li>
                        <li id="tab_link_output">
                            <a href="#output" data-toggle="tab">Log Output</a>
                        </li>
                        <li>
                            <a href="#schedExDetails${scheduledExecution?.id}" data-toggle="tab">Definition</a>
                        </li>
                    </ul>
                </div>
            </div>


    <div class="row">
        <div class="col-sm-12">
            <div class="tab-content">
                <div class="tab-pane active" id="summary">
                    <g:render template="wfstateSummaryDisplay" bean="${workflowState}" var="workflowState"/>
                </div>
                <div class="tab-pane" id="state">
                    <div class="flowstate" id="nodeflowstate">
                       <g:render template="wfstateNodeModelDisplay" bean="${workflowState}" var="workflowState"/>
                    </div>
                </div>
                <div class="tab-pane " id="output">
                    <g:render template="/execution/showFragment"
                              model="[execution: execution, scheduledExecution: scheduledExecution, inlineView: false, followmode: followmode]"/>
                </div>
                <div class="tab-pane" id="schedExDetails${scheduledExecution?.id}">
                    <div class="presentation" >
                        <g:render template="execDetails"
                                  model="[execdata: execution, showArgString: false, hideAdhoc: isAdhoc]"/>
                    </div>
                </div>
            </div>
        </div>
    </div>
    </div>


    <g:if test="${scheduledExecution}">
        <div class="row row-space" id="activity_section">
            <div class="col-sm-12 ">
                <h4 class="text-muted "><g:message code="page.section.Activity.for.this.job"/></h4>
                <g:render template="/reports/activityLinks" model="[hideNowRunning:!execution.dateCompleted,execution:execution,scheduledExecution: scheduledExecution, knockoutBinding: true]"/>
            </div>
        </div>
    </g:if>

  <!--[if (gt IE 8)|!(IE)]><!--> <g:javascript library="ace/ace"/><!--<![endif]-->
    <g:javascript>
      fireWhenReady('executionShowPage', function (z) {
          $$('.apply_ace').each(function (t) {
              _applyAce(t);
          })
      });
        fireWhenReady('outputappendform',function(z){
            followControl.bindActions('outputappendform');
        });
    </g:javascript>

  </body>
</html>


