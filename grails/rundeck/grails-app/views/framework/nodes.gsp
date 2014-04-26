<%@ page import="grails.util.Environment; rundeck.User; com.dtolabs.rundeck.server.authorization.AuthConstants" %>
<html>
<head>
    <g:set var="ukey" value="${g.rkey()}" />
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="nodes"/>
    <title><g:message code="gui.menu.Nodes"/> - ${(params.project ?: request.project).encodeAsHTML()}</title>
    <g:javascript library="yellowfade"/>
    <asset:javascript src="nodeFiltersKO.js"/>
    <asset:javascript src="nodeRemoteEdit.js"/>
    <script type="text/javascript">
        function showError(message) {
            $("error").innerHTML += message;
            $("error").show();
        }

        //method called by _nodeFilterInputs
        function _matchNodes(){
            expandResultNodes()
        }
        function _submitNodeFilters(){
            _matchNodes();
            return false;
        }

        var nodeFilter;
        /**
         * node filter link action
         * @param e
         */
        function selectNodeFilterLink(e) {
            var filterName = jQuery(e).data('node-filter-name');
            var filterString = jQuery(e).data('node-filter');
            var filterAll = jQuery(e).data('node-filter-all')?true:false;
            //additive filter string
            if(filterString && !filterName && nodeFilter.filter() && !nodeFilter.filterAll() && !filterAll){
                nodeFilter.filter(nodeFilter.filter()+' '+filterString);
                filterString=nodeFilter.filter();
            }
            loadNodeFilter(filterName, filterString,filterAll);
        }

        var nodespage=0;
        var pagingMax=20;
        /**
        * Expand paging results
        * @param page
        * @param elem
         */
        function expandResultNodes(page,elem){
            loadNodeFilter(null,nodeFilter.filter(),nodeFilter.filterAll(),elem,page);
        }
        /**
         * load either filter string or saved filter
         * @param filterName name of saved filter
         * @param filterString string filter
         * @param filterAll if true, "all nodes" was selected
         * @param elem target element
         * @param page number to load
         */
        function loadNodeFilter(filterName, filterString,filterAll,elem,page) {
            jQuery('.nodefilterlink').removeClass('active');
            if (!page) {
                page = 0;
            }
            if (!elem) {
                elem = 'nodelist';
            }
            if(!filterName&&!filterString&&null==filterAll){
                filterName=nodeFilter.filterName();
                filterString=nodeFilter.filter();
                filterAll=nodeFilter.filterAll();
            }
            if(!filterName && !filterString){
                //if blank input and no filtername selected, do nothing
                return;
            }
            nodespage = page;
            var view = page == 0 ? 'table' : 'tableContent';
            var data = filterName? {filterName: filterName} : {filter: filterString};
            if(filterName){
                jQuery('a[data-node-filter-name=\''+filterName+'\']').addClass('active');
                jQuery('.hiddenNodeFilter').val(filterString);
                jQuery('.hiddenNodeFilterName').val(filterName);
            }else{
                jQuery('.hiddenNodeFilter').val(filterString );
                jQuery('.hiddenNodeFilterName').val('');
            }
            nodeFilter.filterAll(filterAll);
            nodeFilter.filterName(filterName);
            nodeFilter.filter(filterString);
            _updateMatchedNodes(data,elem,'${params.project?:request.project}',false,{view:view,expanddetail:true,inlinepaging:true,
                page:page,max:pagingMax},function(xht){
            });
        }
        function _loadNextNodesPageTable(max,total,tbl,elem){
            if(!nodespage){
                nodespage=0;
            }
            var next=nodespage+1;
            if(total<0 || max*next<total){
                //create sibling of elem
                var div= new Element('tbody');
                $(tbl).insert({bottom:div});
                //total < 0 means load all remaining, so invert next page
                expandResultNodes(next* (total<0?-1:1),Element.identify(div));
            }
//            console.log("next: "+(max*(next+1))+", total: "+total);
            var loadCount = max*(next+1);
            if(loadCount>=total || total<0){
                //hide pager button area
                $(elem).hide();
            }else{
                //update moreCount
                $('moreCount').innerHTML=total-loadCount;
                if(total-loadCount<max){
                    $('nextPageButton').hide();
                }
            }
        }


        /**
         * Handle embedded content updates
         */
        function _updateBoxInfo(name,data){
            if(name=='nodetable'){
                if(null !=data.total && typeof(nodeFilter)!='undefined'){
                    nodeFilter.total(data.total);
                }
                if(null!=data.allcount){
                    if(typeof(nodeFilter) != 'undefined'){
                        nodeFilter.allcount(data.allcount);
                    }
                }
                if(null!=data.filter){
                    if (typeof(nodeFilter) != 'undefined') {
                        nodeFilter.filter(data.filter);
                    }
                }
            }
        }



        /**
         * START page init
         */
        function init() {
            var filterParams =${[filterName:params.filterName,filter:query?.filter,filterAll:params.showall in ['true',true]].encodeAsJSON()};
            nodeFilter = new NodeFilters(
                    appLinks.frameworkAdhoc,
                    appLinks.scheduledExecutionCreate,
                    appLinks.frameworkNodes,
                    Object.extend(filterParams,{
                        nodesTitleSingular:"${g.message(code:'Node',default:'Node')}",
                        nodesTitlePlural:"${g.message(code:'Node.plural',default:'Nodes')}"
                    }));
            ko.applyBindings(nodeFilter);
            jQuery('#nodesContent').on('click', '.nodefilterlink', function (evt) {
                evt.preventDefault();
                selectNodeFilterLink(this);
            });
            jQuery('#searchForm').submit(_submitNodeFilters);
            loadNodeFilter();
        }
        jQuery(document).ready(init);

    </script>

</head>
<body>

<g:if test="${session.user && User.findByLogin(session.user)?.nodefilters}">
    <g:set var="filterset" value="${User.findByLogin(session.user)?.nodefilters}"/>
</g:if>
<div id="nodesContent">
    <g:set var="run_authorized" value="${auth.adhocAllowedTest( action:AuthConstants.ACTION_RUN,project: params.project ?: request.project)}"/>
    <g:set var="job_create_authorized" value="${auth.resourceAllowedTest(kind:'job', action: AuthConstants.ACTION_CREATE,project: params.project ?: request.project)}"/>
    <g:render template="/common/messages"/>

<div class="row ">
<div  class="col-sm-9">
    <g:set var="wasfiltered" value="${ paginateParams?.keySet().grep(~/(?!proj).*Filter|groupPath|project$/)||(query && !query.nodeFilterIsEmpty() && !summaryOnly)}"/>
    <g:set var="filtersOpen" value="${summaryOnly || showFilter||params.createFilters||params.editFilters||params.saveFilter || filterErrors?true:false}"/>

    <div id="${ukey}filter">
        <g:form action="nodes" controller="framework" class="form form-inline" name="searchForm">
            <g:hiddenField name="max" value="${max}"/>
            <g:hiddenField name="offset" value="${offset}"/>
            <g:hiddenField name="formInput" value="true"/>
            <g:set var="filtvalue"
                   value="${query?.('filter')?.encodeAsHTML()}"/>

            <div class="form-group">
                <span class="input-group" >
                    <g:render template="nodeFilterInputGroup" model="[filterset: filterset, filtvalue:filtvalue,filterName:filterName]"/>
                </span>
            </div>




        </g:form>

    <div class=" collapse" id="queryFilterHelp">
        <div class="help-block">
            <g:render template="/common/nodefilterStringHelp"/>
        </div>
    </div>
    </div>



                <g:set var="adminauth"
                       value="${auth.resourceAllowedTest(kind:'node',action:[AuthConstants.ACTION_REFRESH],project: params.project ?: request.project)}"/>
                <g:if test="${adminauth}">
                    <g:if test="${selectedProject && selectedProject.shouldUpdateNodesResourceFile()}">
                        <span class="floatr"><g:link action="reloadNodes" params="${[project:selectedProject.name]}" class="btn btn-sm btn-default" title="Click to update the resources.xml file from the source URL, for project ${selectedProject.name}" onclick="\$(this.parentNode).loading();">Update Nodes for project ${selectedProject.name}</g:link></span>
                    </g:if>
                </g:if>
        </div>

    <div class="col-sm-3" data-bind="visible: filterName() || filterWithoutAll()">
        <div class="well well-sm inline ">
            <div data-bind="visible: filterName()">

                Selected Filter:  <strong data-bind="text: filterName()">${filterName.encodeAsHTML()}</strong>
                        <span data-bind="visible: filterName()">
                            <a href="#"
                                class="textbtn textbtn-danger"
                               data-toggle="modal"
                               data-target="#deleteFilterModal">
                                <i class="glyphicon glyphicon-remove"></i>
                                delete &hellip;
                            </a>
                        </span>

            </div>
            <div data-bind="visible: !filterName() && filterWithoutAll()">
                    <a href="#"
                        class="textbtn textbtn-success"
                       data-toggle="modal"
                       data-target="#saveFilterModal">
                        <i class="glyphicon glyphicon-plus"></i>
                        Save this filter&hellip;
                    </a>
            </div>
        </div>
    </div>

    %{--Form for saving/deleting node filters--}%
    <g:form class="form form-horizontal">
        <g:hiddenField name="project" value="${params.project}"/>
        <g:render template="nodeFiltersHidden"/>
        <g:render template="/common/queryFilterManagerModal"
                  model="${[rkey: ukey, filterName: filterName, filterset: filterset,
                          filterLinks: true,
                          formId: "${ukey}filter",
                          ko: true,
                          deleteActionSubmit: 'deleteNodeFilter', storeActionSubmit: 'storeNodeFilter']}"/>
    </g:form>
</div>
    <div class="row row-space">
        <div class="col-sm-12">
            <span class="h4">
                <g:if test="${summaryOnly}">
                    <span data-bind="text: allcount">${total}</span>
                    <span data-bind="text: nodesTitle()">Node${1 != total ? 's' : ''}</span>
                </g:if>
                <g:else>
                    <span data-bind="text: allcount">${total}</span>
                    <span data-bind="text: nodesTitle()">Node${1 != total ? 's' : ''}</span> matching filter
                </g:else>
            </span>
            <g:if test="${tagsummary}">
                <g:render template="tagsummary"
                          model="${[hidetop:!summaryOnly,tagsummary: tagsummary, link: [action: 'nodes', controller: 'framework', param: 'nodeIncludeTags']]}"/>
            </g:if>
            <g:elseif test="${tagsummary?.size() == 0}">
            %{--<span class="text-muted">no tags</span>--}%
            </g:elseif>
            <div class=" btn-group pull-right ">
                <button class="btn btn-default dropdown-toggle" data-toggle="dropdown">
                   Node Actions <span class="caret"></span>
                </button>
                <ul class="dropdown-menu" role="menu">
                    <li data-bind="visible: hasNodes()" class="${run_authorized?'':'disabled'}">
                        <a href="#" data-bind="${run_authorized?'click: runCommand':''}"
                           title="${run_authorized ? '' : 'Not authorized'}"
                           class="${run_authorized ? '' : 'has_tooltip'}"
                           data-placement="left"
                        >
                            <i class="glyphicon glyphicon-play"></i>
                            Run a command on <span data-bind="text: allcount">${total}</span>
                            <span data-bind="text: nodesTitle()">Node${1 != total ? 's' : ''}</span> …
                        </a>
                    </li>

                    <li class="${job_create_authorized?'':'disabled'}">
                        <a href="#" data-bind="${job_create_authorized?'click: saveJob':''}"
                            title="${job_create_authorized?'':'Not authorized'}"
                            class="${job_create_authorized?'':'has_tooltip'}"
                            data-placement="left"
                        >
                            <i class="glyphicon glyphicon-plus"></i>
                            Create a job for <span data-bind="text: allcount">${total}</span>
                            <span data-bind="text: nodesTitle()">Node${1 != total ? 's' : ''}</span> …
                        </a>
                    </li>
                </ul>
            </div>
        </div>
        <g:form class="form form-inline" action="adhoc" controller="framework" method="get" name="runform">
            <g:hiddenField name="project" value="${params.project ?: request.project}"/>
            <g:render template="nodeFiltersHidden" model="${[params: params, query: query]}"/>
        </g:form>

    </div>

    <div class="row row-space">
        <div class="col-sm-12">

            <div class=" clear matchednodes " id="nodelist" >
                <g:if test="${!summaryOnly}">
                    <g:render template="allnodes" model="${[nodeview:'table', expanddetail: true,allnodes: allnodes, totalexecs: totalexecs, jobs: jobs, params: params, total: total, allcount: allcount, page: page, max: max, nodeauthrun: nodeauthrun, tagsummary: null]}" />
                </g:if>
            </div>

        </div>
    </div>


</div>
</body>
</html>
