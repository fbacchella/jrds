<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<%@ page contentType="text/html; charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page session="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<jsp:useBean id="jrdsBean" class="jrds.ChoiceJspBean" />

<html>

	<head>
		<meta http-equiv="content-type" content="text/html;charset=utf-8">
		<title>Liste des choix</title>
		<link href="lib/choice.css" type="text/css" rel="stylesheet" media="all">
	</head>

	<body bgcolor="#ffffff">
		<script type="text/javascript" src="date.js"></script>
		<script type="text/javascript" src="popupwindow.js"></script>
		<script type="text/javascript" src="anchorposition.js"></script>
		<script type="text/javascript" src="calendarpopup.js"></script>
		<div align="left"></div>
		<div align="center">
			<h4>Bienvenue sur RDS version AOL</h4>
		</div>
		<div align="left">
			<img style="float: left" src="img/aollogo.gif" alt="AOL" height="25" width="96" border="0">
			<form id="FormName" action="tree.jsp" method="POST" name="FormName" target="tree">
					<p>Choisissez un groupe<span class="value">
							<select  display="inline" name="group">
							<c:forEach items="${jrdsBean.groupList}" var="i">
								<c:out value="${i}" escapeXml="false" />
							</c:forEach>
						</select></span></p>
				<div align="center">
					
							
					<table width="531" border="0" cellspacing="2" cellpadding="0">
						<tr>
							<td width="160">Choisissez une tri</td>
							<td><select name="sort">
									<option value="1">par serveur</option>
									<option value="2">par vue</option>
								</select></td>
						</tr>
						<tr>
							<td width="160"></td>
							<td></td>
						</tr>
						<tr>
							<td width="160">Choisissez une échelle</td>
							<td><select name="scale">
									<option value="1">Quotidienne</option>
									<option value="2">Hebdomadaire</option>
									<option value="3">Mensuelle</option>
									<option value="4">Annuelle</option>
								</select></td>
						</tr>
						<tr>
							<td width="160">Choisissez la date de fin</td>
							<td>
								<script type="text/javascript">
var cal = new CalendarPopup();
cal.setMonthNames('Janvier','Fevrier','Mars','Avril','Mai','Juin','Juillet','Aout','Septembre','Octobre','Novembre','Decembre');
cal.setDayHeaders('D','L','M','M','J','V','S');
cal.setWeekStartDay(1);
cal.setTodayText("Aujourd hui");
cal.setReturnFunction("localReturn");
cal.showYearNavigation();
function localReturn(y,m,d) {
	document.forms[0].date.value=d+"/"+m+"/"+y;
}
</script>
								<input type="text" name="date" value="<%= jrdsBean.getNow() %>" /><a id="anchor" title="cal.select(document.forms[0].date,'anchor','dd/MM/yyyy'); return false;" onclick="cal.select(document.forms[0].date,'anchor','dd/MMM/yyyy'); return false;" name="anchor" href="#"><img src="img/cal.gif" alt="calendrier" height="16" width="16" border="0"></a></td>
						</tr>
					</table>
					<input type="submit" name="submitButtonName"></div>
			</form>
		</div>
	</body>

</html>
