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
<style type="text/css">
@import "lib/dojo/dijit/themes/nihilo/nihilo.css";
@import "lib/dojo/dojox/form/resources/DropDownSelect.css";
/* Define Polices */
body, input, option, select, tab, div, td, a, textarea, .link a {
	font-family:'Bitstream Vera Sans', arial, Tahoma, 'Sans serif';
}
/* Defined Polices font and color*/
body, input, option, select, tab, div, td, a, textarea {
	color: black;
	font-size:11px;
	margin:0;
	padding:0;
}
.linelabel {
	width: 60px;
}
#mainDiv {
	height: 100%;
	border: 0;
	padding: 0;
	margin: 0;
	background-color:#FFF;
}
.graph {
	float:left;
}
#treePane {
	background-color:#DDDDDD;
	border:1px solid black;
	float:left;
	margin:5px;
	padding:5px;
	width:330px;
	height: 100%;
}
#graphPane {
	width:710px;
}
#formPane {
	width:710px;
}
.box {
	background-color: white;
	border: 2px solid black;
	padding: 8px;
	margin: 4px;
}
.formblock {
	background-color:#F5F5F5;
	border:1px solid #AAAAAA;
	margin:5px;
}
.field {
	margin: 5px;
}
.iconslist {
	float:left;
	width:16px;
	margin:5px;
}
.icon {
	cursor:pointer;
}
.graphblock {
	text-align:center;
}
</style>
<script type="text/javascript" src="lib/dojo/dojo.js.uncompressed.js" djConfig="parseOnLoad:true, isDebug: true, locale: 'en-us',"></script>
<script type="text/javascript">

dojo.require("dijit.layout.BorderContainer");
dojo.require("dijit.layout.ContentPane");
dojo.require("dijit.layout.AccordionContainer");
dojo.require("dijit.form.DateTextBox");
dojo.require("dijit.form.Button");
dojo.require("dijit.Dialog");
dojo.require("dijit.form.TextBox");
dojo.require("dijit.form.FilteringSelect");
dojo.require("dijit.form.TextBox");
dojo.require("dojox.form.DropDownSelect");
dojo.require("dijit.form.Form");
dojo.require("dijit.layout.ContentPane");
dojo.require("dojo.parser");  // scan page for widgets and instantiate them
dojo.require("dijit.form.Button");
dojo.require("dijit.form.ValidationTextBox");
dojo.require("dojo.number");
dojo.require("dojo.data.ItemFileReadStore");
dojo.require("dijit.Tree");

function doGraphList(result) {
	var graphPane = dojo.byId("graphPane");
	dojo.empty(graphPane);
	for(i in result.items) {
		graph = result.items[i];
		var  graphBlock = dojo.create("div")
		dojo.attr(graphBlock, "class","graphblock");
		 
		var graphImg = dojo.create("img");
		dojo.attr(graphImg, "class","graph");
		dojo.attr(graphImg, "name", graph.name);
		dojo.attr(graphImg, "alt", graph.name);
		dojo.attr(graphImg, "id", graph.id);
		dojo.attr(graphImg, "src", graph.imghref);

		var iconsList =  dojo.create("div");
		dojo.attr(iconsList, "class","iconslist");

		var application_double = dojo.create("img");
		dojo.attr(application_double, "class","icon");
		dojo.attr(application_double, "src","img/application_double.png");
		dojo.attr(application_double, "heigth","16");
		dojo.attr(application_double, "width","16");
	 	var application_view_list = dojo.create("img");
		dojo.attr(application_view_list, "class","icon");
		dojo.attr(application_view_list, "src","img/application_view_list.png");
		dojo.attr(application_view_list, "heigth","16");
		dojo.attr(application_view_list, "width","16");
		var time = dojo.create("img");
		dojo.attr(time, "class","icon");
		dojo.attr(time, "src","img/time.png");
		dojo.attr(time, "heigth","16");
		dojo.attr(time, "width","16");
		var disk = dojo.create("img");
		dojo.attr(disk, "class","icon");
		dojo.attr(disk, "src","img/disk.png");
		dojo.attr(disk, "heigth","16");
		dojo.attr(disk, "width","16");

		dojo.place(application_double, iconsList);
		dojo.place(application_view_list, iconsList);
		dojo.place(time, iconsList);
		dojo.place(disk, iconsList);
		 
		dojo.place(graphImg, graphBlock);
		dojo.place(iconsList, graphBlock);
		dojo.place(graphBlock,graphPane);
	}
	
	return result;
}

function loadTree(item,  node){
	var filter = item.filter;
	if(filter) {				
		queryParams.filter=filter;
		window.location.replace(location.href.replace(/&.*/, "") + "?" + dojo.objectToQuery(queryParams));
	}
	else {
		queryParams.id = item.id[0].replace(/.*\./, "");
		reloadTree();
	}
}

function reloadTree() {
	return dojo.xhrGet( {
						url: "jsongraph?" + dojo.objectToQuery(queryParams),
						handleAs: "json",
						load: doGraphList
						});
}

function initQuery() {
	var queryParams = dojo.queryToObject(window.location.search.slice(1));
	
	queryParams.end = "<jsp:getPropertyname="period" property="stringEnd" />";
	queryParams.begin = "<jsp:getPropertyname="period" property="stringBegin" />";
	queryParams.autoperiod = "<jsp:getPropertyname="period" property="scale" />";	

	queryParams.max = "<jsp:getPropertyname="period" property="maxStr" />";
	queryParams.min = "<jsp:getPropertyname="period" property="minStr" />";

	return queryParams;
}

function fileForms() {	
    var dateForm = dojo.byId("dateForm");
	dateForm.end.value =  queryParams.end;
	dateForm.begin.value =  queryParams.begin;
	dateForm.max.value =  queryParams.max;
	dateForm.min.value = queryParams.min;
	
	if(queryParams.max || queryParams.min) {
		setAutoscale(false);
	}

    var autoperiod = new dojox.form.DropDownSelect({
    		//onItemClick: function(item, evt) {
    		//	queryParams.autoperiod = item.option.value;
    		//	reloadTree();
    		//    return this.inherited(item, evt);
    		//},
        	value:queryParams.autoperiod,
         }, "autoperiod");
    autoperiod.dropDown.oldItemClick=autoperiod.dropDown.onItemClick;
    autoperiod.dropDown.onItemClick = setScale;

    if(queryParams.host)
    	dojo.byId("hostForm").host.value = queryParams.host;
}

function updateScale(value) {
	var id = this.id;
	queryParams[id] = value;
}

function setScale(item, evt) {
	queryParams.autoperiod = item.option.value;
	reloadTree();
    return this.oldItemClick(item, evt);
};

function getTree() {
	var type="filter";
	if(queryParams.filter || queryParams.host) {
		type="tree";
	}

	var store = new dojo.data.ItemFileReadStore({
	    url: "jsontree?" + dojo.objectToQuery(queryParams)
	});

	var treeModel = new dijit.tree.ForestStoreModel({
		jsId:"treeModel",
		id:"treeModel",
        store: store,
        query: {"type": type},
        rootId: "root",
        childrenAttrs: ["children"]
    });

    new dijit.Tree({
        model: treeModel,
		showRoot: false,
        onClick: loadTree,
     }, "treeOne");	
}

function resetScale() {
	var dateForm = dojo.byId("dateForm");
	queryParams.max = "";
	queryParams.min = "";

	reloadTree();
}

function setAutoscale(value) {
	var autoscale = dijit.byId("autoscale");
	autoscale.attr('checked',value)	
}

var queryParams=initQuery();

        dojo.addOnLoad(function(){
                dojo.declare("DayHourTextBox", dijit.form.DateTextBox, {
                	jrdsFormat: {
                        selector: 'date', 
                        datePattern: 'yyyy-MM-dd',
                        locale: 'en-us'
                	},
                	regExpGen: function() { 
                		return "\\d\\d\\d\\d-\\d\\d-\\d\\d";
                	},
                	format: function(date) {
                    	return dojo.date.locale.format(date, this.jrdsFormat);
                    },
                	parse: function(date) {
                    	return dojo.date.locale.parse(date, this.jrdsFormat);
                    },
                	serialize: function(date) {
                    	console.log("Serialize in " + this.id);
                    	var sdate = dojo.date.locale.format(date, this.jrdsFormat);
                    	queryParams[this.id] = sdate;
                    	queryParams.autoperiod = -1;
                    	return sdate
                    },
                });
               new DayHourTextBox({
                        name: "begin",
                }, "begin");
                new DayHourTextBox({
                        name: "end",
                }, "end");

	fileForms();
	getTree();
        });		
</script>
</head>
<body class="nihilo">
<div id="mainDiv" dojoType="dijit.layout.BorderContainer" >
 	<div id="treePane" dojoType="dijit.layout.ContentPane" region="leading" >
    	<form dojoType="dijit.form.Form" id="hostForm" action="draft.jsp" method="get" name="hostForm">
			<a id="toroot" href="index.jsp"><img src="img/back_to_album.gif" alt="" height="16" width="20" border="0" /></a>
      		<label for="host">Only host</label>
      		<input type="text" name="host" size="24"  dojoType="dijit.form.TextBox" trim="true" />
    	</form>
    	<div id="treeOne" > </div>
	</div>
  <div dojoType="dijit.layout.ContentPane" region="center">
    <div dojoType="dijit.layout.ContentPane" id="formPane" >
      <form  id="dateForm" name="dateForm" action="draft.jsp" method="get">
       <div  dojoType="dijit.layout.ContentPane" class="formblock" > <span class="linelabel">Time scale</span>
       <select onchange="log" onClick="log" id="autoperiod" name="autoperiod">
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
          <input id="min" type="text" name="min" class="field" value=""
    			 dojoType="dijit.form.ValidationTextBox"
    			 regExpGen="dojo.number._realNumberRegexp"
    			 trim="true"
    			 onFocus="setAutoscale(false);"
    			 onchange="updateScale"
    			 size="14" />
          <label for="max">Max</label>
          <input id="max" type="text" name="max" class="field" value=""
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
