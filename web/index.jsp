<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page session="false" %>
<%@ page import="jrds.HostsList" %>
<%@ page import="jrds.RdsGraph" %>
<jsp:useBean id="jrdsBean" class="jrds.webapp.TreeJspBean" />
<jsp:useBean id="period" class="jrds.webapp.PeriodBean"/>
<jsp:setProperty name="period" property="*" /> 
<html>

	<head>
		<meta http-equiv="content-type" content="text/html;charset=utf-8">
		<title>JRDS</title>
		<style type="text/css"><!--
A:visited { color: rgb(0, 0, 238); }
.tree {float: left; height:100%; 
width: 300px; background-color:  #DDD; 
margin: 5px;
border: 1px solid black;
padding: 5px;
overflow: scroll;
}
#graphs {padding-left: 325px}
.graph {display: block}
--></style>
<link href="lib/calendar-win2k-1.css" rel="stylesheet">
			<script type="text/javascript" src="lib/ua.js"> </script>
			<script type="text/javascript" src="lib/ftiens4.js"> </script>
			<script type="text/javascript" src="lib/calendar.js"></script> 
			<script type="text/javascript" src="lib/calendar-en.js"></script> 
			<script type="text/javascript" src="lib/calendar-setup.js"></script> 
			<script type="text/javascript" src="lib/jrdsdate.js"></script> 
			<script type="text/javascript" src="lib/querystring.js"></script> 
<script type="text/javascript">
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
	var historyWin = window.open("history.jsp" + document.location.search, "history.jsp" + document.location.search, "menubar=no,status=no,resizable=yes");
}

function keep_onClick()
{
	var historyWin = window.open(window.location, qs.get("id", 0).replace("-","_"), "menubar=no,status=no,resizable=yes");
}

function download_onClick()
{
	var historyWin = window.open("download" + document.location.search, "download" + document.location.search, "menubar=no,status=no");
}
//-->
</script>

			<script type="text/javascript">
</script>
	</head>

	<body bgcolor="#ffffff">
	<div class="tree" >
<div style="position:absolute; top:0; left:0; "><table border=0><tr><td><font size=-2><a style="font-size:7pt;text-decoration:none;color:silver" href="http://www.treemenu.net/" target="_blank">JavaScript Tree Menu</a></font></td></tr></table></div>
		<p>
<script type="text/javascript"><!--
USETEXTLINKS = 1  
STARTALLOPEN = 0
HIGHLIGHT = 1
PRESERVESTATE = 1
GLOBALTARGET = 'S'
ICONPATH="img/"
USEFRAMES = 0
<%
	jrdsBean.getJavascriptTree(1, "hostTree_0", out, request, period);
	jrdsBean.getJavascriptTree(2, "viewTree_0", out, request, period); 
%>

foldersTree = gFld("<i>Graph List</i>");
foldersTree.addChildren([hostTree_0, viewTree_0]);
initializeDocument();
 //-->
</script>
</div>
		<div id="content">
			<input class="btnlist" onclick="refresh_onClick();" type="button" name="refreshButton" value="Refresh" tabindex="0">
			<input class="btnlist" onclick="keep_onClick();" type="button" name="keepButton" value="Keep" tabindex="1">
			<input class="btnlist" onclick="history_onClick();" type="button" name="HistoryButton" value="History" tabindex="2">
			<input class="btnlist" onclick="download_onClick();" type="button" name="DownloadButton" value="Download values" tabindex="3">
			<form  id="select" name="dateForm" action="index.jsp" method="GET">
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
 </script>
 			<div id="graphs">
			<% jrdsBean.getGraphList(out, request, period); %>
			</div>
		</div>
	</body>

</html>
