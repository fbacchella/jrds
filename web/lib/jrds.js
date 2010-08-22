function initQuery() {
	var iq = dojo.xhrGet( {
		content:  dojo.queryToObject(window.location.search.slice(1)),
		sync: true,
		url: "queryparams",
		handleAs: "json",
		preventCache: true,
		load: function(response, ioArgs) {
		queryParams = response;
		return response;
	},
	error: function(response, ioArgs) {
		console.error("init query failed with " + response.message);
		return response;
	}

	});
}

function graphHistory() {
	var graphPane = dojo.byId("graphPane");
	dojo.empty(graphPane);

	var graphParams = {};
	graphParams.id = queryParams.id;
	graphParams.history = 1;
	var graphImg = dojo.create("img");
	dojo.attr(graphImg, "src", "graph?" + dojo.objToQuery(graphParams));		
	dojo.place(graphImg, graphPane);
}

function doGraphList(result) {
	var graphPane = dojo.byId("graphPane");
	dojo.empty(graphPane);
	for(i in result.items) {
		graph = result.items[i];
		var  graphBlock = dojo.create("div")
		dojo.attr(graphBlock, "class","graphblock");
		dojo.place(graphBlock,graphPane);

		var graphImg = dojo.create("img");
		dojo.attr(graphImg, "class","graph");
		dojo.attr(graphImg, "name", graph.name);
		dojo.attr(graphImg, "title", graph.name);
		dojo.attr(graphImg, "id", graph.id);
		dojo.attr(graphImg, "src", graph.imghref);
		if(graph.width)
			dojo.attr(graphImg, "width", graph.width);
		if(graph.height)
			dojo.attr(graphImg, "height", graph.height);

		var iconsList =  dojo.create("div");
		dojo.attr(iconsList, "class","iconslist");

		var application_double = dojo.create("img");
		dojo.attr(application_double, "class","icon");
		dojo.attr(application_double, "src","img/application_double.png");
		dojo.attr(application_double, "heigth","16");
		dojo.attr(application_double, "width","16");
		dojo.attr(application_double, "title","Popup the graph");
		dojo.attr(application_double, "onclick","popup('" + graph.popuparg + "'," + graph.id + ");");
		var application_view_list = dojo.create("img");
		dojo.attr(application_view_list, "class","icon");
		dojo.attr(application_view_list, "src","img/application_view_list.png");
		dojo.attr(application_view_list, "heigth","16");
		dojo.attr(application_view_list, "width","16");
		dojo.attr(application_view_list, "title","Graph details");
		dojo.attr(application_view_list, "onclick","details('" + graph.detailsarg + "', '" + graph.probename + "');");
		var time = dojo.create("img");
		dojo.attr(time, "class","icon");
		dojo.attr(time, "src","img/time.png");
		dojo.attr(time, "heigth","16");
		dojo.attr(time, "width","16");
		dojo.attr(time, "title","Graph history");
		dojo.attr(time, "onclick","history('" + graph.historyarg + "', '" + graph.probename + "');");
		var disk = dojo.create("img");
		dojo.attr(disk, "class","icon");
		dojo.attr(disk, "src","img/disk.png");
		dojo.attr(disk, "heigth","16");
		dojo.attr(disk, "width","16");
		dojo.attr(disk, "title","Save data");
		dojo.attr(disk, "onclick","save('" + graph.savearg + "', '" + graph.probename + "');");

		dojo.place(application_double, iconsList);
		dojo.place(application_view_list, iconsList);
		dojo.place(time, iconsList);
		dojo.place(disk, iconsList);

		dojo.place(graphImg, graphBlock);
		dojo.place(iconsList, graphBlock);
	}
	return result;
}

function reloadTree() {	
	return dojo.xhrGet( {
		content: queryParams,
		url: "jsongraph",
		handleAs: "json",
		load: doGraphList
	});
}

function fileForms() {	
	var dateForm = dojo.byId("dateForm");
	if(queryParams.begin && queryParams.end) {
		dateForm.end.value = queryParams.end;
		dateForm.begin.value = queryParams.begin;
	}

	setAutoscale(queryParams.max == null || queryParams.min ==null);
	var autoperiod = dijit.byId('autoperiod'); 
	autoperiod.attr('value', queryParams.autoperiod);
	autoperiod.dropDown.oldItemClick=autoperiod.dropDown.onItemClick;
	autoperiod.dropDown.onItemClick = setScale;

	if(queryParams.host)
		dojo.byId("hostForm").host.value = queryParams.host;
}

function setScale(item, evt) {
	queryParams.autoperiod = item.option.value;
	reloadTree();
	return this.oldItemClick(item, evt);
}

function getTree(treeType) {	
	//treeType can be :
	// filter, subfilter for a custom tree
	// graph, node, tree filter
	// graph, node, tree for an host

	
	var treeOne = dijit.byId("treeOne");
	if( treeOne) {
		treeOne.destroyRecursive(true);
	}

	var store = new dojo.data.ItemFileReadStore({
		url: "jsontree?" + dojo.objectToQuery(queryParams)
	});

	var treeModel = new dijit.tree.ForestStoreModel({
		store: store,
		query: {"type": treeType },
		rootLabel: 'All filters',
		childrenAttrs: ["children"]
	});
	
	treeOneDiv = dojo.create("div", {id: 'treeOne'}, dojo.byId('treePane'), "last");

	treeOne = new dijit.Tree({
		model: treeModel,
		showRoot: false,
		onClick: loadTree
	}, treeOneDiv);	

}

function loadTree(item,  node){
	var filter = item.filter;
	var host = item.host;
	if(filter) {
		queryParams.filter=filter;
		queryParams.host='';
		getTree('tree');
	}
	else if(host) {
		queryParams.host=host;
		queryParams.filter='';
		getTree('tree');
	}
	else {
		queryParams.treeType = 'tree';
		queryParams.id = item.id[0].replace(/.*\./, "");
		reloadTree();
	}
}

function toogleSort() {
	queryParams.sort = dijit.byId("sorted").attr('checked');
	reloadTree();
}

function sort()
{
	if(! queryParams ) {
		queryParams.sort = 1;
	}
	else
		queryParams.sort = undef;
	reloadTree();
}

function setupCalendar() {
	dojo.declare("HourBox",dijit.form.TimeTextBox , {
		style: "width:50px",
		hourFormat: {
		timePattern: 'HH:mm',
		locale: 'en-us',
		selector: 'time'
	},        
	onChange: function(date) {
		if(date && ! date == '') {
			var elems = queryParams[this.queryId].split(' ');
			queryParams[this.queryId] = elems[0] + ' ' + dojo.date.locale.format(date, this.hourFormat);
		}
		return this.inherited(arguments);
	}
	});

	var beginHour = new Date();
	beginHour.setHours(00);
	beginHour.setMinutes(00);
	beginHour.setSeconds(00);
	beginTimeTextBox = new HourBox( {
		name: 'beginh',
		value: '',
		queryId: 'begin',
		constraints:{timePattern:'HH:mm', clickableIncrement:'T00:30:00', visibleIncrement:'T00:30:00', visibleRange:'T05:00:00'}
	}, 'beginh');
	var endHour = new Date();
	endHour.setHours(23);
	endHour.setMinutes(59);
	endHour.setSeconds(59);
	endTimeTextBox = new HourBox( {
		name: 'endh',
		value: '',
		queryId: 'end',
		constraints:{timePattern:'HH:mm', clickableIncrement:'T00:30:00', visibleIncrement:'T00:30:00', visibleRange:'T05:00:00'}
	}, 'endh');

	dojo.declare("DayHourTextBox", dijit.form.DateTextBox, {
		style: "width:100px",
		jrdsFormatDate: {
		selector: 'date', 
		datePattern: 'yyyy-MM-dd',
		locale: 'en-us'
	},
	dateStr: '',
	regExpGen: function() {
		return "\\d\\d\\d\\d-\\d\\d-\\d\\d";
	},
	format: function(date) {
		return dojo.date.locale.format(date, this.jrdsFormatDate);
	},
	parse: function(date) {
		return dojo.date.locale.parse(date, this.jrdsFormatDate);
	},
	serialize: function(date) {
		return dojo.date.locale.format(date, this.jrdsFormatDate);
	},
	onChange: function(date) {
		var sdate = dojo.date.locale.format(date, this.jrdsFormatDate);
		queryParams[this.id] = sdate;
		queryParams.autoperiod = -1;
		this.timeBox.attr('value', this.resetHour);

		return this.inherited(arguments);
	}
	});
	new DayHourTextBox({
		//A bug in dojo 4.2 ?
		regExpGen: function() {
		return "\\d\\d\\d\\d-\\d\\d-\\d\\d";
	},
	name: "begin",
	resetHour: beginHour,
	timeBox: beginTimeTextBox
	}, "begin");
	new DayHourTextBox({
		//A bug in dojo 1.4.2 ?
		regExpGen: function() {
			return "\\d\\d\\d\\d-\\d\\d-\\d\\d";
		},
	name: "end",
	resetHour: endHour,
	timeBox: endTimeTextBox
	}, "end");

}

function download_onClick(url)
{
	var historyWin = window.open("download" + document.location.search, "download" + document.location.search, "menubar=no,status=no");
}

function details(url, name)
{
	var detailsWin = window.open(url, name, "resizable=yes,scrollbars=yes");
}

function popup(url,id)
{
	var img = document.getElementById(id);
	if(img != null) {
		var width = "width=" + img.width * 1.1;
		var height = "height=" + img.height * 1.1;
		var title = img.name;
	}
	else {
		var width = "width=750";
		var height = "height=500";
	}
	return popupWin = window.open(url, "_blank" , height + "," + width + ",menubar=no,status=no,resizable=yes,scrollbars=yes,location=yes");
}

function save(url, name)
{
	var popupWin = window.open(url, name , "menubar=no,status=no,resizable=no,scrollbars=no");
}

function history(url, name)
{
	var historyWin = window.open(url, "_blank", "width=750,menubar=no,status=no,resizable=yes,scrollbars=yes,location=yes");
}

//called onChange for scale button
function resetScale() {
	if(dijit.byId("autoscale").attr('checked')) {
		queryParams.max = null;
		queryParams.min = null;
		if(queryParams.id || queryParams.id == 0 )
			reloadTree();
	}
	dijit.byId("max").attr('value', queryParams.max);
	dijit.byId("min").attr('value', queryParams.min);
}

//Called upon startup and focus on scale text boxes, will propagate good values using onChange function : resetScale and updateScale, 
function setAutoscale(value) {
	var autoscale = dijit.byId("autoscale");
	autoscale.attr('checked',value);
}

//Called upon edited scale text boxes
function updateScale(value) {
	//value==0 is not a null value, keep it
	if(! value && value !=0 )
		value = null;
	queryParams[this.id] = value;
}

function scaleKeyEvent(evt)
{
	if(evt.keyCode == dojo.keys.ENTER || evt.keyCode == dojo.keys.TAB){
		queryParams[this.id] = this.getValue();
		if(evt.keyCode == dojo.keys.ENTER) {
			if(queryParams.max != null && queryParams.min != null) {
				reloadTree();
			}
			dojo.stopEvent(evt);
		}
	}
}

function periodKeyEvent(evt)
{
	if(evt.keyCode == dojo.keys.ENTER){
		dojo.stopEvent(evt);
	}
}

function transitTab(newPage, oldPage){
	var newId = newPage.attr('id');
	var oldId = oldPage.attr('id');
	if(oldId != 'adminTab') {
		oldPage.destroyDescendants(false);
	}
	var treeType;
	if(newId == 'mainTab') {
		treeType = 'filter';
		queryParams.filter = '';
	}
	else if(newId == 'tagstab') {
		treeType = 'subfilter';
		queryParams.filter = 'All tags';
	}
	else {
		treeType = 'tree';
		queryParams.filter = newPage.attr('title');
	}
	if(newId != 'adminTab') {
		newPage.attr('content', dojo.clone(mainPane));
		setupCalendar();
		queryParams.host = '';
		fileForms();
		getTree(treeType);
	}
}

function sendReload(evt) {
	var iq = dojo.xhrGet( {
		sync: true,
		url: "reload",
		handleAs: "text",
		preventCache: true,
		load: function(response, ioArgs) {
	},
	error: function(response, ioArgs) {
	}

	});
}

function searchHost(evt) {
	try
	{
		queryParams.host = this.attr('value').host;
		queryParams.filter = '';
		getTree('tree');
	}
	catch(err)
	{
		console.error(err);
	}
	return false;
}

function goHome(evt) {
	queryParams.host = '';
	queryParams.filter = '';
	tabs = dijit.byId('tabs');
	tabs.selectChild('mainTab');
	getTree('filter');
}