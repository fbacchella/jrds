<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<%@ page contentType="text/html; charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page session="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<jsp:useBean id="jrdsBean" class="jrds.SimpleGraphJspBean" />
<jsp:setProperty name="jrdsBean" property="*" />

<html>

	<head>
		<meta http-equiv="content-type" content="text/html;charset=utf-8">
		<title>Graph</title>
		<script type="text/javascript" src="lib/popupwindow.js"></script>
		<script type="text/javascript"><!--
function history_onClick()
{
	newurl = document.URL.replace("/[^/]\?/","keep.jsp\?");
}

function keep_onClick()
{
}
//-->
</script>
	</head>

	<body bgcolor="#ffffff">
		<img src="graph" alt="" height="32" width="32" border="0">
		<form id="FormName" action="" method="get" name="popupForm" target="_blank">
			<input type="hidden" name="begin" value="hiddenValue">
			<input type="hidden" name="end" value="hiddenValue">
			<input type="hidden" name="id" value="hiddenValue">
			<div align="center">
				<table width="180" border="1" cellspacing="2" cellpadding="0">
					<tr>
						<td width="33%"><input onclick="keep_onClik();" type="submit" name="keepButton" value="garder" tabindex="0"></td>
						<td width="100"></td>
						<td width="33%"><input onclick="history_onClick();" type="submit" name="HistoryButton" value="Historique" tabindex="1"></td>
					</tr>
				</table>
		  </div>
		</form>
	</body>
</html>

