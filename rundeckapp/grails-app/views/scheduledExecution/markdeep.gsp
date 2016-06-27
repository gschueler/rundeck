<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: 6/27/16
  Time: 12:53 PM
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="base"/>
    <title></title>
</head>

<body>
<g:form controller="scheduledExecution" action="markdeep">
    <g:textArea name="content" rows="15" cols="100" value="${content?:''}"/>
    <g:submitButton name="Submit" class="btn"/>
</g:form>
<g:if test="${result}">
${raw(result)}
</g:if>
</body>
</html>