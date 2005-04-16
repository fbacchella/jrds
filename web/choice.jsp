<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<%@ page contentType="text/html; charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page session="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<jsp:useBean id="jrdsBean" class="jrds.ChoiceJspBean" />

<html>

	<head>
		<meta http-equiv="content-type" content="text/html;charset=utf-8">
		<title>Liste des choix</title>
		<style type="text/css"><!--
.logo    { position: absolute; top: 20%; float: left }
--></style>
		<script type="text/javascript" src="lib/date.js"></script>
		<script type="text/javascript" src="lib/popupwindow.js"></script>
		<script type="text/javascript" src="lib/anchorposition.js"></script>
		<script type="text/javascript" src="lib/calendarpopup.js"></script>
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
	</head>

	<body bgcolor="#ffffff">
		<div align="left"></div>
		<div align="center">
			<h4>Welcome to RDS, Java version</h4>
		</div>
		<div align="left">
			<!--.AGL.wrap(Fabrice Bacchella ;2005.01.12 17:31;;)<img class="logo" src="img/aollogo.gif" alt="AOL" height="25" width="96" border="0">-->
			<form id="FormName" action="tree.jsp" method="POST" name="FormName" target="tree">
				<div align="center">
					<table border="0" cellspacing="2" cellpadding="0">
						<tr>
							<td width="160">Choose a sort</td>
							<td><select name="sort">
									<option value="1">by server</option>
									<option value="2">by view</option>
								</select></td>
						</tr>
						<tr>
							<td width="160">Choose a group</td>
							<td><select name="group">
									<c:forEach items="${jrdsBean.groupList}" var="i">
										<c:out value="${i}" escapeXml="false" />
									</c:forEach>
								</select>
							</td>
						</tr>
						<tr>
							<td width="160">Choose a time scale</td>
							<td><select name="scale">
									<option value="1">Daily</option>
									<option value="2">Hebdomadaire</option>
									<option value="3">Weekly</option>
									<option value="4">Annually</option>
								</select></td>
						</tr>
						<tr>
							<td width="160">Choose the end date</td>
							<td><input type="text" name="date" value=<%= jrdsBean.getNow() %> size="9" /><a name="anchor" id="anchor" title="Sélection de la date" onclick="cal.select(document.forms[0].date,'anchor','dd/MMM/yyyy'); return false;" name="anchor" href="#"><img src="img/cal.gif" alt="calendrier" height="16" width="16" border="0"></a></td>
						</tr>
					</table>
					<input type="submit" name="submitButtonName"></div>
			</form>
		</div>
	</body>

</html>
