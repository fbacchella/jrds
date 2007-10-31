<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page session="false" %>

<jsp:useBean id="jrdsBean" class="jrds.webapp.TreeJspBean" />
<jsp:useBean id="period" class="jrds.webapp.ParamsBean"/>
<%period.parseReq(request);%>
<html>

	<head>
		<meta http-equiv="content-type" content="text/html;charset=utf-8">
		<title>Graph</title>
<link rel="stylesheet" type="text/css" href="lib/jrds.css" />
<script type="text/javascript" src="lib/querystring.js"></script>
<script type="text/javascript" src="lib/jrds.js" ></script>

<script type="text/javascript" >
periodUrl="<jsp:getPropertyname="period" property="periodUrl" />";
</script>

	</head>

	<body bgcolor="#ffffff">
			<jsp:setProperty name="period" property="scale" value="7" /> 
			<% jrdsBean.getGraphList(out, request, period); %>
			<jsp:setProperty name="period" property="scale" value="9" /> 
			<% jrdsBean.getGraphList(out, request, period); %>
			<jsp:setProperty name="period" property="scale" value="11" /> 
			<% jrdsBean.getGraphList(out, request, period); %>
			<jsp:setProperty name="period" property="scale" value="16" /> 
			<% jrdsBean.getGraphList(out, request, period); %>
	</body>
</html>

