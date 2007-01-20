<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page session="false" %>
<%@ page import="jrds.HostsList" %>
<%@ page import="jrds.RdsGraph" %>
<jsp:useBean id="jrdsBean" class="jrds.webapp.TreeJspBean" />
<jsp:useBean id="period" class="jrds.webapp.ParamsBean"/>
<jsp:useBean id="graph" class="jrds.webapp.GraphBean"/>

<%period.parseReq(request);
graph.config(request, period);
%>
<html>

	<head>
		<meta http-equiv="content-type" content="text/html;charset=utf-8">
		<title>JRDS</title>
<link href="lib/calendar-win2k-1.css" rel="stylesheet" />
			<script type="text/javascript" src="lib/ua.js"> </script>
			<script type="text/javascript" src="lib/ftiens4.js"> </script>
			<script type="text/javascript" src="lib/calendar.js"></script> 
			<script type="text/javascript" src="lib/calendar-en.js"></script> 
			<script type="text/javascript" src="lib/calendar-setup.js"></script> 
			<script type="text/javascript" src="lib/jrdsdate.js"></script> 
			<script type="text/javascript" src="lib/querystring.js"></script> 

<link rel="stylesheet" type="text/css" href="lib/jrds.css" />
<script type="text/javascript" src="lib/jrds.js" ></script>

<script type="text/javascript" >
periodUrl="<jsp:getPropertyname="period" property="periodUrl" />";
</script>


	</head>

	<body bgcolor="#ffffff">
<% jrdsBean.getGraphList(out, request, period); %>
	</body>
</html>