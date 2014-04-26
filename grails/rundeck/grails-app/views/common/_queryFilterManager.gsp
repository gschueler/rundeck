<%--
 Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 --%>
 <%--
    _queryFilterManager.gsp
    
    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: Apr 16, 2010 9:50:54 AM
    $Id$
 --%>
<g:if test="${!rkey}">
    <g:set var="rkey" value="${g.rkey()}" />
</g:if>
<g:if test="${!filterName}">
    <span class="textbtn textbtn-success floatr" style="${wdgt.styleVisible(unless: params.saveFilter)}" onclick="['${rkey}fsave','${rkey}fsavebtn'].each(Element.toggle);" id="${rkey}fsavebtn" title="Click to save this filter with a name">
        save this filter&hellip;
    </span>
    <div id="${rkey}fsave" style="${params.saveFilter ? '' : 'display:none;'} " class="filterdef clear">
        <p class="prompt">Save Filter</p>
        <div class="form-group">
            <label for="existsFilterName">Filter:</label>
            <g:select class="form-control input-sm"
                name="existsFilterName" optionKey="name" optionValue="name" from="${filterset?filterset.sort({a,b->a.name.compareTo(b.name)}):filterset}" value="${filterName}" noSelection="['':'-New-']"
                onchange="if(this.value){\$('newFilterInput').hide();}else{\$('newFilterInput').show();}"/>
        </div>
        <div id="newFilterInput" class="form-group">
            <label for="newFilterName">Name:</label>
            <g:textField name="newFilterName" class="form-control input-sm"/>
        </div>

        <input type="button" onclick="['${rkey}fsave','${rkey}fsavebtn'].each(Element.toggle); return false;" value="Cancel" class="btn btn-default btn-sm"/>
        %{--<g:actionSubmit value="Save Filter" name="nodesForm" url="[controller:'framework',action:'storeNodeFilter',params:[fragment:true]]" update="${rkey}nodeForm"/>--}%
        
        <g:if test="${storeActionSubmit}">
            <g:actionSubmit value="Save Filter" action="${storeActionSubmit}" class="btn btn-primary btn-sm" />
        </g:if>
        <g:elseif test="${storeActionSubmitRemote}">
            <g:submitToRemote value="Save Filter" url="${storeActionSubmitRemote}" update="${update}" class="btn btn-primary btn-sm"/>
        </g:elseif>
    </div>
</g:if>
<g:if test="${filterName}">
    <div class="filterdef saved clear">
        <span class="prompt">${filterName.encodeAsHTML()}</span>
        <span class="textbtn textbtn-danger pull-right" onclick="['${rkey}fdel','${rkey}fdelbtn'].each(Element.toggle);" id="${rkey}fdelbtn" title="Click to delete this saved filter">
            <b class="glyphicon glyphicon-remove"></b>
            delete&hellip;
        </span>
        <div id="${rkey}fdel" style="display:none">
            <g:hiddenField name="delFilterName" value="${filterName}"/>
            <span class="confirmMessage">Are you sure you want to delete this filter?</span>
            <input type="button" onclick="['${rkey}fdel','${rkey}fdelbtn'].each(Element.toggle);
            return false;" value="No" class="btn btn-default btn-sm"/>
            <g:if test="${deleteActionSubmit}">
                <g:actionSubmit  action="${deleteActionSubmit}" value="Yes" class="btn btn-danger btn-sm"/>
            </g:if>
            <g:elseif test="${deleteActionSubmitRemote}">
                <g:submitToRemote value="Yes" url="${deleteActionSubmitRemote}" update="${update}" class="btn btn-danger btn-sm"/>
            </g:elseif>
        </div>
    </div>
</g:if>
