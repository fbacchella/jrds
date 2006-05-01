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
		<title>Graph</title>
		<style type="text/css"><!--
A:visited { color: rgb(0, 0, 238); }
.tree {float: left; height:100%; width: 33%}
.graph {display: block}
--></style>
	</head>

	<body bgcolor="#ffffff">
		<div align="center">
			<jsp:setProperty name="period" property="scale" value="7" /> 
			<% jrdsBean.getGraphList(out, request, period); %>
			<jsp:setProperty name="period" property="scale" value="9" /> 
			<% jrdsBean.getGraphList(out, request, period); %>
			<jsp:setProperty name="period" property="scale" value="11" /> 
			<% jrdsBean.getGraphList(out, request, period); %>
			<jsp:setProperty name="period" property="scale" value="16" /> 
			<% jrdsBean.getGraphList(out, request, period); %>
		</div>
	</body>
</html>

