<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<%@ page contentType="text/html; charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page session="false" %>
<%@taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<jsp:useBean id="jrdsBean" class="jrds.webapp.ChoiceJspBean" />
<jsp:useBean id="period" class="jrds.webapp.PeriodBean"/>
<jsp:setProperty name="period" property="*" /> 

<html>

	<head>
		<meta http-equiv="content-type" content="text/html;charset=utf-8">
		<title>Liste des choix</title>
		<link href="lib/rds.css" rel="stylesheet" type="text/css" media="all">
		<script type="text/javascript" src="lib/calendar.js"></script> 
<script type="text/javascript" src="lib/calendar-en.js"></script> 
<script type="text/javascript" src="lib/calendar-setup.js"></script> 
<script type="text/javascript" src="lib/querystring.js"></script> 
<script type="text/javascript" src="lib/jrdsdate.js"></script> 
	</head>

	<body bgcolor="#ffffff">
		<div align="center">
			<h4>Welcome to RDS, Java version</h4>
		</div>
		<div align="center">
			<form id="dateForm" action="tree.jsp" method="GET" name="dateForm" target="tree">
				<div id="period">
					<div id="scale">
							Choose a time scale<select name="scale" onchange="submitScale(this);">
							<c:set value="0" var="opt" />
							<c:forEach var="i" items="${period.periodNames}">
								<option value="<c:out value="${opt}" />">

								<c:out value="${i}"/>
								</option>
								<c:set value="${opt + 1}" var="opt" />
							</c:forEach>
						</select></div>
					<table id="period" border="0" cellspacing="2" cellpadding="0">
						<tr>
							<td><span class="dateChoiceText">Choose the begin date</span></td>
							<td><input size="14" type="text" name="begin" id="begin" value="" size="9" /></td>
							<td><img id="dateBeginTrigger" src="img/cal.gif" alt="calendrier" height="16" width="16" border="0" style="cursor: pointer"></td>
						</tr>
						<tr>
							<td><span class="dateChoiceText">Choose the end date</span></td>
							<td><input size="14" type="text" name="end" id="end" value="" size="9" /></td>
							<td><img id="dateEndTrigger" src="img/cal.gif" alt="calendrier" height="16" width="16" border="0" style="cursor: pointer"></td>
						</tr>
					</table>
				</div>
				<div align="center">
					<input type="submit"></div>
			</form>
			<script type="text/javascript">
	document.dateForm.end.value = "<jsp:getPropertyname="period" property="end" />"; 
	document.dateForm.begin.value = "<jsp:getPropertyname="period" property="begin" />"; 
    document.dateForm.scale.selectedIndex = "<jsp:getPropertyname="period" property="scale" />";
    beginCal = startCal("begin", "dateBeginTrigger");
    endCal = startCal("end", "dateEndTrigger");
</script>
		</div>
	</body>

</html>
