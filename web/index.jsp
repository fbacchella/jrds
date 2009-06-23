<html >
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page session="false" %>
<%@ page import="jrds.HostsList" %>
<%@ page import="jrds.GraphNode" %>
<%@ page isELIgnored="false" %>
<jsp:useBean id="jrdsBean" class="jrds.webapp.TreeJspBean" />
<jsp:useBean id="period" class="jrds.webapp.ParamsBean"/>
<%period.parseReq(request);%>

<head>
	<title>JRDS</title>
<link href="lib/dijit/themes/nihilo/nihilo.css" rel="stylesheet" type="text/css" />
<link href="lib/dojox/form/resources/DropDownSelect.css" rel="stylesheet" type="text/css" />
<link href="lib/jrds.css" rel="stylesheet" type="text/css" />
<script type="text/javascript" src="lib/dojo/dojo.js.uncompressed.js" djConfig="parseOnLoad:true, isDebug: true, locale:'en-us'"></script>
<script type="text/javascript" src="lib/jrds.js"></script>
<script type="text/javascript">
function initQuery() {
	var queryParams = dojo.queryToObject(window.location.search.slice(1));
	
	queryParams.end = "<jsp:getPropertyname="period" property="stringEnd" />";
	queryParams.begin = "<jsp:getPropertyname="period" property="stringBegin" />";
	queryParams.autoperiod = "<jsp:getPropertyname="period" property="scale" />";	

	queryParams.max = "<jsp:getPropertyname="period" property="maxStr" />";
	queryParams.min = "<jsp:getPropertyname="period" property="minStr" />";

	return queryParams;
}

var queryParams=initQuery();
dojo.addOnLoad(function(){
	setupCalendar();
	fileForms();
	getTree();
	});

</script>
</head>
<body class="nihilo">
<div id="mainDiv" dojoType="dijit.layout.BorderContainer" >
 	<div id="treePane" dojoType="dijit.layout.ContentPane" region="leading" >
    	<form dojoType="dijit.form.Form" id="hostForm" action="index.jsp" method="get" name="hostForm">
			<a id="toroot" href="index.jsp"><img src="img/back_to_album.gif" alt="" height="16" width="20" border="0" /></a>
      		<label for="host">Only host</label>
      		<input type="text" name="host" size="24"  dojoType="dijit.form.TextBox" trim="true" />
    	</form>
    	<div id="treeOne" > </div>
	</div>
  <div dojoType="dijit.layout.ContentPane" region="center">
    <div dojoType="dijit.layout.ContentPane" id="formPane" >
      <form  id="dateForm" name="dateForm" action="index.jsp" method="get">
       <div  dojoType="dijit.layout.ContentPane" class="formblock" > <span class="linelabel">Time scale</span>
       <select id="autoperiod" name="autoperiod">
<c:set value="0" var="opt" />
<c:forEach var="i" items="${period.periodNames}"><option value="<c:out value="${opt}" />"><c:out value="${i}"/></option>
<c:set value="${opt + 1}" var="opt" /></c:forEach>
          </select>

          <label for="begin">Begin</label>
          <input type="text" name="begin" id="begin" class="field" />
          <label for="end">End</label>
          <input type="text" name="end" id="end" class="field" />
        </div>
        <div  dojoType="dijit.layout.ContentPane" class="formblock" > <span class="linelabel">Vertical scale</span>
          <button id="autoscale" dojoType="dijit.form.ToggleButton"  checked name="autoscale"
  				  onChange="resetScale();" 
  				  iconClass="dijitCheckBoxIcon"> Auto </button>
          <label for="min">Min</label>
          <input id="min" type="text" name="min" class="field"
    			 dojoType="dijit.form.ValidationTextBox"
    			 regExpGen="dojo.number._realNumberRegexp"
    			 trim="true"
    			 onFocus="setAutoscale(false);"
    			 onchange="updateScale"
    			 size="14" />
          <label for="max">Max</label>
          <input id="max" type="text" name="max" class="field"
    			 dojoType="dijit.form.ValidationTextBox"
    			 regExpGen="dojo.number._realNumberRegexp"
    			 trim="true"
    			 onFocus="setAutoscale(false);"
    			 onchange="updateScale"
    			 size="14" />
        </div>
        <button onClick="reloadTree(); return false" type="submit" dojoType="dijit.form.Button" >Render</button>
      </form>
    </div>
    <div  dojoType="dijit.layout.ContentPane" id="graphPane">
      <div class='graphblock'>
      
      </div>
    </div>
  </div>
</div>
</body>
</html>
