<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants; rundeck.Execution" %>
<g:javascript>
/** START history
         *
         */


    function init(){
        <g:if test="${!(grailsApplication.config.rundeck?.gui?.enableJobHoverInfo in ['false', false])}">
        $$('.obs_bubblepopup').each(function(e) {
            new BubbleController(e,null,{offx:-14,offy:null}).startObserving();
        });
        </g:if>
    }
    Event.observe(window,'load',init);

</g:javascript>

<div class="row">
    <g:render template="/scheduledExecution/showHead"
              model="[scheduledExecution: scheduledExecution, followparams: [mode: followmode, lastlines: params.lastlines]]"/>
</div>
<g:set var="runAccess" value="${auth.jobAllowedTest(job: scheduledExecution, action: AuthConstants.ACTION_RUN)}"/>
<div class="row">
    <div class="col-sm-12">
        <ul class="nav nav-tabs">
            <g:if test="${runAccess}">
                <li class="active"><a href="#runjob" data-toggle="tab"><g:message
                        code="scheduledExecution.show.run.tab.name"/></a></li>
            </g:if>
            <g:else>
                <li class="disabled">
                    <a href="#"
                       title="${message(code:'unauthorized.job.run')}"
                       class="has_tooltip"
                       data-placement="bottom">
                        <g:message code="scheduledExecution.show.run.tab.name"/>
                    </a>
                </li>
            </g:else>
            <li class="${runAccess ? '' : 'active'}"><a href="#schedExDetails${scheduledExecution?.id}"
                                                        data-toggle="tab"><g:message code="definition"/></a></li>
        </ul>

        <div class="tab-content">
            <g:if test="${runAccess}">
                <div class="tab-pane active" id="runjob">
                    <tmpl:execOptionsForm
                            model="${[scheduledExecution: scheduledExecution, crontab: crontab, authorized: authorized]}"
                            hideHead="${true}"
                            hideCancel="${true}"
                            defaultFollow="${true}"/>
                </div>
            </g:if>
            <div id="schedExDetails${scheduledExecution?.id}"
                 class="tab-pane panel panel-default panel-tab-content  ${runAccess ? '' : 'active'}">
                <div class="panel-body">
                    <g:render template="showDetail"
                              model="[scheduledExecution: scheduledExecution, showEdit: true, hideOptions: true]"/>

                </div>
            </div>
        </div>
    </div>
</div>


<div class="row">
    <div class="col-sm-12 ">
        <h4 class="text-muted">Statistics</h4>

        <g:render template="/scheduledExecution/renderJobStats" model="${[scheduledExecution: scheduledExecution]}"/>
    </div>
</div>

<div class="row" id="activity_section">
    <div class="col-sm-12">
        <h4 class="text-muted"><g:message code="page.section.Activity.for.this.job" /></h4>

        <g:render template="/reports/activityLinks" model="[scheduledExecution: scheduledExecution, knockoutBinding:true]"/>
    </div>
</div>

<!--[if (gt IE 8)|!(IE)]><!--> <g:javascript library="ace/ace"/><!--<![endif]-->
<g:javascript>
    fireWhenReady('schedExecPage', function (z) {
        $$('.apply_ace').each(function (t) {
            _applyAce(t,'400px');
        })
    });
</g:javascript>
