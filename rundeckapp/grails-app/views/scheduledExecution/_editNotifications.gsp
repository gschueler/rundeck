<g:set var="notifications" value="${scheduledExecution.notifications}"/>
<g:set var="defSuccess" value="${scheduledExecution.findNotification('onsuccess', 'email')}"/>
<g:set var="isSuccess" value="${params.notifySuccessRecipients && 'true' == params.notifyOnsuccess || defSuccess}"/>
<g:set var="defSuccessUrl" value="${scheduledExecution.findNotification('onsuccess', 'url')}"/>
<g:set var="isSuccessUrl" value="${params.notifySuccessUrl && 'true' == params.notifyOnsuccessUrl || defSuccessUrl}"/>

<g:set var="defFailure" value="${scheduledExecution.findNotification('onfailure', 'email')}"/>
<g:set var="isFailure" value="${params.notifyFailureRecipients && 'true' == params.notifyOnfailure || defFailure}"/>
<g:set var="defFailureUrl" value="${scheduledExecution.findNotification('onfailure', 'url')}"/>
<g:set var="isFailureUrl" value="${params.notifyFailureUrl && 'true' == params.notifyOnfailureUrl || defFailureUrl}"/>

<g:set var="defStart" value="${scheduledExecution.findNotification('onstart', 'email')}"/>
<g:set var="isStart" value="${ defStart}"/>
<g:set var="defStartUrl" value="${scheduledExecution.findNotification('onstart', 'url')}"/>
<g:set var="isStartUrl" value="${ defStartUrl}"/>
<tr>
    <td>
        Send Notification?
    </td>
    <td>
        <label>
            <g:radio value="false" name="notified"
                     checked="${!(notifications || params.notified=='true')}"
                     id="notifiedFalse"/>
            No
        </label>

        <label>
            <g:radio name="notified" value="true"
                     checked="${notifications || params.notified == 'true'}"
                     id="notifiedTrue"/>
            Yes
        </label>

        <g:javascript>
            <wdgt:eventHandlerJS for="notifiedTrue" state="unempty">
                <wdgt:action visible="true" targetSelector="tr.notifyFields"/>
            </wdgt:eventHandlerJS>
            <wdgt:eventHandlerJS for="notifiedFalse" state="unempty">
                <wdgt:action visible="false" targetSelector="tr.notifyFields"/>

                <wdgt:action check="false" target="notifyOnsuccess"/>
                <wdgt:action visible="false" target="notifSuccessholder"/>
                <wdgt:action check="false" target="notifyOnfailure"/>
                <wdgt:action visible="false" target="notifFailureholder"/>
            </wdgt:eventHandlerJS>
        </g:javascript>
    </td>
</tr>
<g:render template="/scheduledExecution/editNotificationsTrigger"
    model="${[
            isVisible:( notifications ),
            trigger:'success',
            isEmail:isSuccess,
            isUrl:isSuccessUrl,
            definedNotifications: scheduledExecution.notifications?.findAll{it.eventTrigger=='onsuccess'}
    ]}"
    />
<g:render template="/scheduledExecution/editNotificationsTrigger"
          model="${[
                  isVisible: (notifications),
                  trigger: 'failure',
                  isEmail: isFailure,
                  isUrl: isFailureUrl,
                  definedNotifications: scheduledExecution.notifications?.findAll { it.eventTrigger == 'onfailure' }
          ]}"/>

<g:render template="/scheduledExecution/editNotificationsTrigger"
          model="${[
                  isVisible: (notifications),
                  trigger: 'start',
                  isEmail: isStart,
                  isUrl: isStartUrl,
                  definedNotifications: scheduledExecution.notifications?.findAll { it.eventTrigger == 'onstart' }
          ]}"/>


%{--<tr class="notifyFields" style="${wdgt.styleVisible(if: isFailure || isSuccess || isSuccessUrl || isFailureUrl)}">--}%

    %{--<!-- onfailure-->--}%
    %{--<td>--}%
        %{--<label for="notifyOnfailure"--}%
               %{--class=" ${hasErrors(bean: scheduledExecution, field: 'notifyFailureRecipients', 'fieldError')}">--}%
            %{--<g:message code="notification.event.onfailure"/>--}%
        %{--</label>--}%
    %{--</td>--}%
    %{--<td>--}%
        %{--<div>--}%
            %{--<span>--}%
                %{--<g:checkBox name="notifyOnfailure" value="true" checked="${isFailure ? true : false}"/>--}%
                %{--<label for="notifyOnfailure">Send Email</label>--}%
            %{--</span>--}%
            %{--<span id="notifFailureholder" style="${wdgt.styleVisible(if: isFailure)}">--}%
                %{--<label>to: <g:textField name="notifyFailureRecipients" cols="70" rows="3"--}%
                                        %{--value="${defFailure ? defFailure.content : params.notifyFailureRecipients}"--}%
                                        %{--size="60"/></label>--}%

                %{--<div class="info note">comma-separated email addresses</div>--}%
                %{--<g:hasErrors bean="${scheduledExecution}" field="notifyFailureRecipients">--}%
                    %{--<div class="fieldError">--}%
                        %{--<g:renderErrors bean="${scheduledExecution}" as="list" field="notifyFailureRecipients"/>--}%
                    %{--</div>--}%
                %{--</g:hasErrors>--}%
            %{--</span>--}%
            %{--<wdgt:eventHandler for="notifyOnfailure" state="checked" target="notifFailureholder" visible="true"/>--}%
        %{--</div>--}%

        %{--<div>--}%
            %{--<span>--}%
                %{--<g:checkBox name="notifyOnfailureUrl" value="true" checked="${isFailureUrl ? true : false}"/>--}%
                %{--<label for="notifyOnfailureUrl">Webhook</label>--}%
            %{--</span>--}%
            %{--<span id="notifFailureholder2" style="${wdgt.styleVisible(if: isFailureUrl)}">--}%
                %{--<label>POST to URLs:--}%
                    %{--<g:set var="notiffailureurlcontent"--}%
                           %{--value="${defFailureUrl ? defFailureUrl.content : params.notifyFailureUrl}"/>--}%
                    %{--<g:if test="${notiffailureurlcontent && notiffailureurlcontent.length() > 30}">--}%
                        %{--<textarea name="notifyFailureUrl"--}%
                                  %{--style="vertical-align:top;"--}%
                                  %{--rows="6" cols="40">${notiffailureurlcontent?.encodeAsHTML()}</textarea>--}%
                    %{--</g:if>--}%
                    %{--<g:else>--}%
                        %{--<g:textField name="notifyFailureUrl" cols="70" rows="3"--}%
                                     %{--value="${notiffailureurlcontent?.encodeAsHTML()}" size="60"/>--}%
                    %{--</g:else>--}%
                %{--</label>--}%

                %{--<div class="info note">comma-separated URLs</div>--}%
                %{--<g:hasErrors bean="${scheduledExecution}" field="notifyFailureUrl">--}%
                    %{--<div class="fieldError">--}%
                        %{--<g:renderErrors bean="${scheduledExecution}" as="list" field="notifyFailureUrl"/>--}%
                    %{--</div>--}%
                %{--</g:hasErrors>--}%
            %{--</span>--}%
            %{--<wdgt:eventHandler for="notifyOnfailureUrl" state="checked" target="notifFailureholder2" visible="true"/>--}%
        %{--</div>--}%
    %{--</td>--}%
%{--</tr>--}%
