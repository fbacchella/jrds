<!DOCTYPE html 
     PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
     "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page session="false" %>
<%@ page import="jrds.HostsList" %>
<%@ page import="jrds.GraphNode" %>
<%@ page isELIgnored="false" %>
<jsp:useBean id="jrdsBean" class="jrds.webapp.TreeJspBean" />
<jsp:useBean id="period" class="jrds.webapp.ParamsBean"/>

<%period.parseReq(request);%>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

	<head>
		<meta http-equiv="content-type" content="text/html;charset=utf-8"/>
		<title>JRDS</title>
<link href="lib/calendar-win2k-1.css" rel="stylesheet" />
			<script type="text/javascript" src="lib/ua.js"> </script>
			<script type="text/javascript" src="lib/ftiens4.js"> </script>
			<script type="text/javascript" src="lib/calendar.js"></script> 
			<script type="text/javascript" src="lib/calendar-en.js"></script> 
			<script type="text/javascript" src="lib/calendar-setup.js"></script> 
			<script type="text/javascript" src="lib/querystring.js"></script>

<script type="text/javascript" >
periodUrl="<jsp:getPropertyname="period" property="periodUrl" />";
</script>

<link rel="stylesheet" type="text/css" href="lib/jrds.css" />
<script type="text/javascript" src="lib/jrds.js" ></script>

	</head>

<body bgcolor="#ffffff">
	<div id="content">
	<div id="treeframe" >
			<form id="hostSelect" action="index.jsp" method="get" name="hostForm">
			<a id="toroot" href="index.jsp"><img src="img/back_to_album.gif" alt="" height="16" width="20" border="0"></a>
				<input name="begin" type="hidden" value="<jsp:getPropertyname="period" property="stringBegin" />"/>
				<input name="end" type="hidden" value="<jsp:getPropertyname="period" property="stringEnd" />"/>
				<input name="scale" type="hidden" value="<jsp:getPropertyname="period" property="scale" />"/>
				<input name="max" type="hidden" value="<jsp:getPropertyname="period" property="maxStr" />"/>
				<input name="min" type="hidden" value="<jsp:getPropertyname="period" property="minStr" />"/>
				<label>Only host</label> <input type="text" name="host" size="24">
			</form>
		<div id="tree">
			<div id="treenotice"><table border="0"><tr><td><font size="-2"><a style="font-size:7pt;text-decoration:none;color:silver" href="http://www.treemenu.net/" target="_blank">JavaScript Tree Menu</a></font></td></tr></table></div>
<script type="text/javascript"><!--
USETEXTLINKS = 1  
HIGHLIGHT = 1
GLOBALTARGET = 'S'
ICONPATH="img/"
USEFRAMES = 0
PRESERVESTATE = 1
STARTALLOPEN = 0
<%jrdsBean.ManageTree(out, request, response);%>
<%jrdsBean.getJavascriptTree(out, period);%>
 //-->
</script>
		</div>
	</div>
	<div id="graphframe">
			<div id="form">
			<input class="btnlist" onclick="refresh_onClick();" type="button" name="refreshButton" value="Refresh" tabindex="0">
			<input class="btnlist" onclick="sort_onClick();" type="button" name="sortButton" value="Sort by graph name" tabindex="1">
			<form  id="dateForm" name="dateForm" action="index.jsp" method="get">
				<input name="id" type="hidden" value="" class="hiddeninput" />
				<input name="filter" type="hidden" value="" class="hiddeninput" />
				<input name="host" type="hidden" value="" class="hiddeninput" />
				<p class="formblock" id="period">
                	<span class="linelabel">Time scale</span>
					<label for="scale">Predefined</label>
					<select name="scale" onchange="submitScale(this);">
<c:set value="0" var="opt" />
<c:forEach var="i" items="${period.periodNames}"><option value="<c:out value="${opt}" />"><c:out value="${i}"/></option>
<c:set value="${opt + 1}" var="opt" /></c:forEach>
					</select>
					<label for="begin">From</label>
					<input size="14" type="text" name="begin" id="begin" value=""  /><img  align="absmiddle" class="calendar" id="dateBeginTrigger" src="img/cal.gif" alt="calendrier" height="16" width="16" border="0" style="cursor: pointer" />
					<label for="end">To</label>
					<input size="14" type="text" name="end" id="end" value="" /><img  align="absmiddle" class="calendar" id="dateEndTrigger" src="img/cal.gif" alt="calendrier" height="16" width="16" border="0" style="cursor: pointer" />
				</p>
				<p class="formblock">
					<span class="linelabel">Vertical scale</span>
					<input class="btnlist" onclick="autoscale_onClick();" type="button" name="autoscaleButton" value="Auto" tabindex="3">
					<label for="min">min</label>
					<input size="14" type="text" name="min" id="min" value="" />
					<label for="max">max</label>
					<input size="14" type="text" name="max" id="max" value="" />
                </p>
                <p class="formblock" id="submit">
					<input type="submit" />
				</p>
			</form>
<script type="text/javascript">
	document.dateForm.end.value = "<jsp:getPropertyname="period" property="stringEnd" />"; 
	document.dateForm.begin.value = "<jsp:getPropertyname="period" property="stringBegin" />"; 
	document.dateForm.max.value = "<jsp:getPropertyname="period" property="maxStr" />"; 
	document.dateForm.min.value = "<jsp:getPropertyname="period" property="minStr" />"; 
    document.dateForm.scale.selectedIndex = <jsp:getPropertyname="period" property="scale" />;
    document.dateForm.id.value = qs.get("id", 0);
    document.dateForm.filter.value = qs.get("filter", "");
   	document.dateForm.host.value = qs.get("host", "");
   	document.hostForm.host.value = qs.get("host", "");
    startCal("begin", "dateBeginTrigger", calCloseBegin);
    startCal("end", "dateEndTrigger", calCloseEnd);
 </script>
 			</div>
	 		<div id="graphs">
<% jrdsBean.getGraphList(out, request, period); %>
			</div>
	</div>
	</div>
</body>

</html>
