<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page session="false" %>
<%@ page import="jrds.HostsList" %>
<%@ page import="jrds.RdsGraph" %>

<jsp:useBean id="period" class="jrds.webapp.PeriodBean"/>
<jsp:setProperty name="period" property="*" /> 

<%
		final HostsList hl = HostsList.getRootGroup();
		String rrdId = request.getParameter("id");
		RdsGraph graph = hl.getGraphById(Integer.parseInt(rrdId));
%>

<html>

	<head>
		<meta http-equiv="content-type" content="text/html;charset=utf-8">
		<title>Graph</title>
<link href="lib/calendar-win2k-1.css" rel="stylesheet">
<script type="text/javascript" src="lib/calendar.js"></script> 
<script type="text/javascript" src="lib/calendar-en.js"></script> 
<script type="text/javascript" src="lib/calendar-setup.js"></script> 
<script type="text/javascript" src="lib/jrdsdate.js"></script> 
<script type="text/javascript" src="lib/querystring.js"></script> 
<script type="text/javascript" src="lib/infobulle.js"></script> 
<script type="text/javascript">
//We preload the image
var oImage = new Image;
oImage.src =  "graph" + document.location.search;
//the query string analyzer
qs = new Querystring();

</script>
		
		<script type="text/javascript"><!--
function refresh_onClick()
{
	window.location.reload( false );
}

function history_onClick()
{
	var historyWin = window.open("history.html" + document.location.search, "history.html" + document.location.search, "menubar=no,status=no");
}

function keep_onClick()
{
	var historyWin = window.open("graph" + document.location.search, "graph" + document.location.search, "menubar=no,status=no");
}

function download_onClick()
{
	var historyWin = window.open("download" + document.location.search, "download" + document.location.search, "menubar=no,status=no");
}
//-->
</script>
	</head>

	<body bgcolor="#ffffff">
		<div align="center">
			<table width="180" border="0" cellspacing="2" cellpadding="0">
				<tr>
					<td><input onclick="refresh_onClick();" type="button" name="refreshButton" value="Refresh" tabindex="0"></td>
					<td width="100"></td>
					<td><input onclick="keep_onClick();" type="button" name="keepButton" value="Keep" tabindex="1"></td>
					<td width="100"></td>
					<td><input onclick="history_onClick();" type="button" name="HistoryButton" value="History" tabindex="2"></td>
					<td width="100"></td>
					<td><input onclick="download_onClick();" type="button" name="DownloadButton" value="Download values" tabindex="3"></td>
				</tr>
			</table>
			<form  name="dateForm" action="simplegraph.jsp" method="GET">
				<input name="id" type="hidden" value=""/>
				<div align="center">
					<table border="0" cellspacing="2" cellpadding="0">
						<tr>
							<td width="160">Choose a time scale</td>
							<td><select name="scale" onchange="submitScale(this);">
<c:set value="0" var="opt" />
<c:forEach var="i" items="${period.periodNames}"><option value="<c:out value="${opt}" />"><c:out value="${i}"/></option>
<c:set value="${opt + 1}" var="opt" /></c:forEach>
							</select></td>
						</tr>
						<tr>
							<td width="160">Choose the begin date</td>
							<td><input size="14" type="text" name="begin" id="begin" value="" size="9" /><img id="dateBeginTrigger" src="img/cal.gif" alt="calendrier" height="16" width="16" border="0" style="cursor: pointer"></td>
						</tr>
						<tr>
							<td width="160">Choose the end date</td>
							<td><input size="14" type="text" name="end" id="end" value="" size="9" /><img id="dateEndTrigger" src="img/cal.gif" alt="calendrier" height="16" width="16" border="0" style="cursor: pointer"></td>
						</tr>
					</table>
					<input type="submit">
				</div>
			</form>
<script type="text/javascript">
	document.dateForm.end.value = "<jsp:getPropertyname="period" property="end" />"; 
	document.dateForm.begin.value = "<jsp:getPropertyname="period" property="begin" />"; 
    document.dateForm.scale.selectedIndex = <jsp:getPropertyname="period" property="scale" />;
    document.dateForm.id.value = qs.get("id", 0);
    beginCal = startCal("begin", "dateBeginTrigger");
    endCal = startCal("end", "dateEndTrigger");
    InitBulle("navy","#FFCC66","orange",1);
</script>
			<img onMouseOver="this.T_DELAY=3000;return escape('Graph name: <%=graph.getQualifieName()%><br>Probe Name: <%=graph.getProbe().getQualifieName()%>')" onclick="history_onClick();" id="graphImg" src="img/aollogo.gif" alt="" name="graphImg" border="0">
			<script type="text/javascript">document.graphImg.src="graph" + window.location.search;</script>

		</div>
		<script language="JavaScript" type="text/javascript" src="lib/wz_tooltip.js"></script>
	</body>
</html>

