<%@ page import="grails.converters.deep.JSON; grails.util.Environment" %>
<%--
used by _editOptions.gsp template
--%>
<g:set var="rkey" value="${g.rkey()}"/>
<g:if test="${ optionSelections}">
    <g:set var="usePrefix" value="${paramsPrefix?paramsPrefix:''}"/>
    <g:set var="showDTFormat" value="${false}"/>
    <g:hiddenField name="${usePrefix+'argString'}" value=""/>
    <%
        def optsmap=[:]
        if(optionSelections){
            optionSelections.each{
                optsmap[it.name]=[selopt:it]
            }
        }
    %>
    <div id="_commandOptions">
        <g:each var="optName" in="${optionordering?:optsmap.keySet().sort()}" status="oindex">
            <g:set var="optionSelect" value="${optsmap[optName].selopt }"/>
            <g:set var="optRequired" value="${optionSelect.required}"/>
            <g:set var="optDescription" value="${optionSelect.description}"/>
            <g:set var="fieldName" value="${usePrefix+'option.'+optName}"/>
            <g:set var="optionHasValue" value="${!selectedoptsmap && optionSelect.defaultValue || selectedoptsmap &&
                    selectedoptsmap[optName] || !optionSelect.defaultValue && optRequired && optionSelect.enforced &&
                    optionSelect.values }"/>
            <g:set var="hasError" value="${jobexecOptionErrors?jobexecOptionErrors[optName]:null}"/>
            <g:set var="fieldNamekey" value="${rkey+'_'+optName+'_label'}"/>
            <g:set var="fieldhiddenid" value="${rkey+'_'+optName+'_h'}"/>
            <g:set var="hasRemote" value="${optionSelect.realValuesUrl != null}"/>
            <div class="form-group ${hasError ? 'has-warning' : ''} ${hasRemote?'remote':''}" >
              <label class="remoteoptionfield col-sm-2 control-label" for="${fieldhiddenid}" id="${fieldNamekey}">
                  <span style="display:none;" class="remotestatus"></span>
                  ${optName.encodeAsHTML()}
                  <g:if test="${Environment.current == Environment.DEVELOPMENT && grailsApplication.config.rundeck?.debug}">
                      (${optiondependencies ? optiondependencies[optName] : '-'})(${dependentoptions ? dependentoptions[optName] : '-'})
                  </g:if>
              </label>
                %{--determine if option has all dependencies met--}%
                <g:set var="optionDepsMet" value="${!optiondependencies[optName] || selectedoptsmap && optiondependencies[optName].every {selectedoptsmap[it]}}" />

                    <g:if test="${optionSelect.realValuesUrl !=null}">
                        <div class=" col-sm-9">
                        <g:set var="holder" value="${rkey+'_'+optName+'_hold'}"/>
                        <span id="${holder}" >
                            <g:if test="${!optionDepsMet}">
                                <span class="info note">
                                    <g:message code="option.remote.dependency.emptyresult"/>
                                </span>
                            </g:if>
                                <g:hiddenField name="${fieldName}" value="${selectedoptsmap?selectedoptsmap[optName]:''}" id="${fieldhiddenid}"/>
                            <span class="loading"></span>
                        </span>
                        <g:if test="${Environment.current == Environment.DEVELOPMENT && grailsApplication.config.rundeck?.debug}">
                        <a onclick="_remoteOptionControl('_commandOptions').loadRemoteOptionValues('${optName.encodeAsJavaScript()}');return false;" href="#">${optName.encodeAsHTML()} reload</a>
                        </g:if>
                        </div>
                    </g:if>
                    <g:else>
                        <div class=" col-sm-9">
                        <g:render template="/framework/optionValuesSelect"
                            model="${[rkey: rkey+'_'+oindex,elemTarget:rkey+'_'+optName,optionSelect:optionSelect, fieldPrefix:usePrefix,fieldName:'option.'+optName,selectedoptsmap:selectedoptsmap,fieldkey: fieldhiddenid]}"/>
                        </div>
                    </g:else>
                <div class="col-sm-1">
                    <span id="${optName.encodeAsHTML()+'_state'}">
                        <g:if test="${ optRequired }">
                            <span class="reqwarning has_tooltip"
                                    title="${hasError?.contains('required')?hasError:g.message(code:'option.value.required')}"
                                    data-toggle="tooltip"
                                  style="${wdgt.styleVisible(unless:optionHasValue)}">
                                <i class="glyphicon glyphicon-warning-sign"></i>
                            </span>
                        </g:if>
                    </span>
                </div>

                <div class="col-sm-10 col-sm-offset-2">
                    <span class="help-block">${optDescription?.encodeAsHTML()}</span>
                </div>
                <g:if test="${hasError}">
                    <div class="col-sm-10 col-sm-offset-2">
                        <p class="text-warning">${hasError.encodeAsHTML()}</p>
                    </div>
                </g:if>
            </div>
        </g:each>
    </div>
        <%--
        Javascript for configuring remote option cascading/dependencies
        --%>
        <g:javascript>
            fireWhenReady('_commandOptions', function(){
                var remoteOptions = _remoteOptionControl('_commandOptions');
                <g:if test="${optionsDependenciesCyclic}">
                    remoteOptions.cyclic=true;
                </g:if>
        <g:each var="optName" in="${optionordering ?: optsmap.keySet().sort()}">
            <g:set var="optionSelect" value="${optsmap[optName].selopt}"/>
            <g:set var="fieldName" value="${usePrefix + 'option.' + optName}"/>
            <g:set var="fieldNamekey" value="${rkey + '_' + optName + '_label'}"/>
            <g:set var="holder" value="${rkey + '_' + optName + '_hold'}"/>
            <g:set var="fieldhiddenid" value="${rkey + '_' + optName + '_h'}"/>
            <g:set var="optionDepsMet"
                   value="${!optiondependencies[optName] || selectedoptsmap && optiondependencies[optName].every {selectedoptsmap[it]}}"/>
            <g:if test="${optiondependencies[optName]}">
                remoteOptions.addOptionDependencies("${optName.encodeAsJavaScript()}", ${optiondependencies[optName] as JSON});
            </g:if>
            <g:if test="${dependentoptions[optName]}">
                <%-- If option has dependents, register them to refresh when this option value changes --%>
                remoteOptions.addOptionDeps("${optName.encodeAsJavaScript()}", ${dependentoptions[optName] as JSON});


                <g:if test="${optionSelect.enforced || selectedoptsmap && selectedoptsmap[optName]}">
                <%-- Will be a drop down list, so trigger change automatically. --%>
                    remoteOptions.setOptionAutoReload("${optName.encodeAsJavaScript()}",true);
                </g:if>
            </g:if>
            <g:if test="${optionSelect.realValuesUrl != null}">
                <%-- If option has a remote URL, register data used for ajax reload --%>
                remoteOptions.addOption("${optName.encodeAsJavaScript()}","${holder.encodeAsJavaScript()}",'${scheduledExecutionId.encodeAsJavaScript()}','${optName.encodeAsJavaScript()}','${usePrefix.encodeAsJavaScript()}','${selectedoptsmap ? selectedoptsmap[optName]?.encodeAsJavaScript() : ''}','${fieldNamekey.encodeAsJavaScript()}',true);

                <g:if test="${!optiondependencies[optName] || optionsDependenciesCyclic}">
                    remoteOptions.loadonstart["${optName.encodeAsJavaScript()}"]=true;
                </g:if>
                <g:else>
                    remoteOptions.setOptionAutoReload("${optName.encodeAsJavaScript()}",true);
                </g:else>
                <g:if test="${optionSelect.multivalued}">
                    remoteOptions.setFieldMultiId('${optName.encodeAsJavaScript()}','${fieldhiddenid.encodeAsJavaScript()}');
                </g:if>
                <g:else>
                    remoteOptions.setFieldId('${optName.encodeAsJavaScript()}','${fieldhiddenid.encodeAsJavaScript()}');
                </g:else>
            </g:if>
            <g:else>
                    remoteOptions.addLocalOption("${optName.encodeAsJavaScript()}");
            </g:else>
        </g:each>
        <%-- register observers for field value changes --%>
                remoteOptions.observeChanges();
                if(typeof(_registerJobExecUnloadHandler)=='function'){
                    _registerJobExecUnloadHandler(remoteOptions.unload.bind(remoteOptions));
                }
            });
        </g:javascript>
        <g:if test="${optionsDependenciesCyclic}">
            <g:message code="remote.options.warning.cyclicDependencies" />
        </g:if>
   <g:if test="${showDTFormat}">
     <div class="info note help">

        <g:expander key="argStringDateFormatHelp">datestamp format</g:expander>

        <table id="argStringDateFormatHelp" style="display:none">
            <thead>
                <tr >
                    <th colspan="4">
                        <span style="color:green">$<!-- -->{DATE:<em>XXYYZZ</em>}</span> can be used with these formatting characters:
                    </th>
                </tr>
            </thead>
            <tr>
                <td>y</td> 	<td>Year</td>
                <td>M</td> 	<td>Month in year</td>
            </tr>
            <tr>
                <td>w</td> 	<td>Week in year</td>
                <td>W</td> 	<td>Week in month</td>
            </tr>
            <tr>
                <td>D</td> 	<td>Day in year</td>
                <td>d</td> 	<td>Day in month</td>
            </tr>
            <tr>
                <td>a</td> 	<td>Am/pm marker</td>
                <td>H</td> 	<td>Hour in day (0-23)</td>
            </tr>
            <tr>
                <td>k</td> 	<td>Hour in day (1-24)</td>
                <td>K</td> 	<td>Hour in am/pm (0-11)</td>
            </tr>
            <tr>
                <td>h</td> 	<td>Hour in am/pm (1-12)</td>
                <td>m</td> 	<td>Minute in hour</td>
            </tr>
            <tr>
                <td>s</td> 	<td>Second in minute</td>
                <td>S</td> 	<td>Millisecond</td>
            </tr>
            <!--<tr><td>Z</td> 	<td>Time zone</td></tr>-->
        </table>
    </div>
    </g:if>

</g:if>
<g:elseif test="${!authorized}">
    <div class="info note">Not authorized to execute chosen job.</div>
    <g:if test="${selectedargstring}"><div>Old value: ${selectedargstring.encodeAsHTML()}</div></g:if>
</g:elseif>
<g:else>
    <div class="form-group">
        <div class="col-sm-2 control-label text-form-label">
            <g:message code="input.options" />
        </div>
        <div class="col-sm-10">
            <p class="form-control-static text-muted"><g:message code="no.input.options.for.this.job" /></p>
        </div>
    </div>
</g:else>
