%{--
  - Copyright 2011 DTO Labs, Inc. (http://dtolabs.com)
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -        http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  --}%
 <%--
    _contexthelp.gsp
    
    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: 6/22/11 3:02 PM
 --%>

<%@ page contentType="text/html;charset=UTF-8" %>
<g:set var="rkey" value="${g.rkey()}"/>
<span class="obs_tooltip" id="help_${rkey}"><g:img file="icon-small-help.png" width="16px" height="16px"/></span>
<span class="popout tooltipcontent simplehelp" id="help_${rkey}_tooltip"
      style="display:none; background:white; position:absolute;">
    <g:if test="${code}">
        <g:message code="${code}"/>
    </g:if>
    <g:if test="${text}">
        ${text}
    </g:if>
</span>