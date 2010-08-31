function initQuery() {
	var iq = dojo.xhrGet( {
		content:  dojo.queryToObject(window.location.search.slice(1)),
		sync: true,
		url: "queryparams",
		handleAs: "json",
		preventCache: false,
		load: function(response, ioArgs) {
			queryParams = response;
			if(queryParams.path) {
				var last = queryParams.path[queryParams.path.length -1];
				queryParams.id = last.replace(/.*\./, "");
			}
			return response;
		},
		error: function(response, ioArgs) {
			console.error("init query failed with " + response.message);
			return response;
		}
	});
}

function cleanParams(paramslist) {
	var cleaned = {};
	// Only interesting values from query params are kept
	dojo.forEach(paramslist, function(key){
		value = queryParams[key];
		if(value)
			cleaned[key] = value;
	});
	return cleaned;
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
		var link = dojo.create("img");
		dojo.attr(link, "class","icon");
		dojo.attr(link, "src","img/application_link.png");
		dojo.attr(link, "heigth","16");
		dojo.attr(link, "width","16");
		dojo.attr(link, "title","Save data");
		dojo.attr(link, "id", "link." + graph.id);
		dojo.attr(link, "onclick","sendlink('" + graph.id + "');");

		dojo.place(application_double, iconsList);
		dojo.place(application_view_list, iconsList);
		dojo.place(time, iconsList);
		dojo.place(disk, iconsList);
		dojo.place(link, iconsList);

		dojo.place(graphImg, graphBlock);
		dojo.place(iconsList, graphBlock);
	}
	return result;
}

function getGraphList() {	
	var graphPane = dojo.byId("graphPane");
	dojo.empty(graphPane);
	return dojo.xhrGet( {
		content: cleanParams(['id','begin','end','min','max', 'sort','autoperiod']),
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

	setAutoscale(queryParams.max == null || queryParams.min == null);
	var autoperiod = dijit.byId('autoperiod'); 
	autoperiod.attr('value', queryParams.autoperiod);
	autoperiod.dropDown.oldItemClick=autoperiod.dropDown.onItemClick;
	autoperiod.dropDown.onItemClick = setAutoperiod;
	console.log(autoperiod);
	if(queryParams.host) {
		dojo.byId("hostForm").host.value = queryParams.host;
	}
	
}

function setupDisplay() {
	fileForms();
	
	var type = 'filter';
	if(queryParams.host != null|| queryParams.filter != null) {
		type = 'tree';
	}

	//selectChild send a transitTab that send getTree
	//Don't do it twice
	if(queryParams.tab) {
		tabWidget = dijit.byId('tabs');
		tabWidget.selectChild(queryParams.tab);
	}
	else {
		getTree(type);
	}
	if(queryParams.id) {
		getGraphList();
	}
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
		url: "jsontree?" + dojo.objectToQuery(cleanParams(['host', 'filter']))
	});

	var treeModel = new dijit.tree.ForestStoreModel({
		store: store,
		query: {"type": treeType },
		rootId: '0.0',
		rootLabel: 'All filters',
		childrenAttrs: ["children"]
	});
	
	treeOneDiv = dojo.create("div", {id: 'treeOne'}, dojo.byId('treePane'), "last");

	treeOne = new dijit.Tree({
		model: treeModel,
		showRoot: false,
		onClick: loadTree,
		onLoad: function() {
			if(queryParams.path != null) {
				this.attr('path', queryParams.path);
			}
		}
	}, treeOneDiv);	
}

function loadTree(item,  node){
	var tree = dijit.byId("treeOne");
	if(item.filter) {
		queryParams.filter = item.filter[0];
		delete queryParams.hos;
		getTree('tree');
	}
	else if(item.host) {
		queryParams.host = item.host[0];
		delete queryParams.filter;
		getTree('tree');
	}
	else {
		queryParams.id = item.id[0].replace(/.*\./, "");
		getGraphList();
		
		var path = new Array();
		dojo.forEach(tree.attr('path'), function(entry, i){
			if(i !=0)
				path.push(entry.id[0]);
			else
				path.push(entry.id);
			});
		
		queryParams.path = path;
	}
}

function toogleSort() {
	queryParams.sort = dijit.byId("sorted").attr('checked');
	getGraphList();
}

function sort()
{
	if(! queryParams ) {
		queryParams.sort = 1;
	}
	else
		delete queryParams.sort;
	getGraphList();
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
			dijit.byId('autoperiod').attr('value', 0); 
		}
		return this.inherited(arguments);
	}
	});

	var beginHour = new Date();
	beginHour.setHours(00);
	beginHour.setMinutes(00);
	beginHour.setSeconds(00);
	beginTimeTextBox = new HourBox( {
		constraints:{timePattern:'HH:mm', clickableIncrement:'T00:30:00', visibleIncrement:'T00:30:00', visibleRange:'T05:00:00'},
		name: 'beginh',
		value: '',
		queryId: 'begin'
	}, 'beginh');
	var endHour = new Date();
	endHour.setHours(23);
	endHour.setMinutes(59);
	endHour.setSeconds(59);
	endTimeTextBox = new HourBox( {
		constraints:{timePattern:'HH:mm', clickableIncrement:'T00:30:00', visibleIncrement:'T00:30:00', visibleRange:'T05:00:00'},
		name: 'endh',
		value: '',
		queryId: 'end'
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
			if(date && ! date == '')
				return dojo.date.locale.format(date, this.jrdsFormatDate);
			else
				return '';
		},
		parse: function(date) {
			if(date && ! date == '')
				return dojo.date.locale.parse(date, this.jrdsFormatDate);
			else
				return '';
		},
		serialize: function(date) {
			if(date && ! date == '')
			return dojo.date.locale.format(date, this.jrdsFormatDate);
			else
				return '';
		},
		onChange: function(date) {
			if(date && ! date == '') {
				var sdate = dojo.date.locale.format(date, this.jrdsFormatDate);
				queryParams[this.id] = sdate;
				queryParams.autoperiod = 0;
				this.timeBox.attr('value', this.resetHour);
				dijit.byId('autoperiod').attr('value', 0); 
			}
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

function sendlink(id)
{
    var xhrArgs = {
    	parentid: "link." + id,
        url: "jsonpack",
        postData: dojo.toJson(queryParams),
        handleAs: "text",
        preventCache: false,
        load: function(data, ioargs) {
            linkdialog(data, ioargs);
        },
        error: function(error) {
            //We'll 404 in the demo, but that's okay.  We don't have a 'postIt' service on the
            //docs server.
            //dojo.byId("response2").innerHTML = "Message posted.";
        }
    }
    //Call the asynchronous xhrPost
    var deferred = dojo.xhrPost(xhrArgs);
}

function linkdialog(data, ioargs) {
	dojo.require("dijit.Dialog");
	// create the dialog:
	var myDialog = new dijit.Dialog({
		title: "Graph context",
	    //content: "<i>" + data +"</i>"
	    content: "<a target='_blank' href='" + data +"'>" + data +"</a>"
	});	  
	myDialog.show();
}

//called onChange for scale button
function resetScale() {
	if(dijit.byId("autoscale").attr('checked')) {
		delete queryParams.max;
		delete queryParams.min;
		dijit.byId("max").attr('value', '');
		dijit.byId("min").attr('value', '');
		if(queryParams.id && queryParams.id != 0 )
			getGraphList();
	}
	else {
		dijit.byId("max").attr('value', queryParams.max);
		dijit.byId("min").attr('value', queryParams.min);
	}
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

function setAutoperiod(item, evt) {
	queryParams.autoperiod = item.option.value;
	submitRenderForm();
	dojo.forEach(['begin', 'beginh', 'end', 'endh'], function(id, i) {
		dijit.byId(id).attr('value', '');
	});
	return this.oldItemClick(item, evt);
}

function submitRenderForm(evt) {
	try {
		if( ! queryParams.id)
			return false;
		if(queryParams.autoperiod == 0 && (! queryParams.begin || ! queryParams.end) )
			return false;
		if(! queryParams.max || ! queryParams.min)
			return false;
		
		if(queryParams.id && queryParams.id != 0 )
			getGraphList();
	}
	catch(err) {
		console.error(err);
	}
	return false;	
}

function transitTab(newPage, oldPage){
	var newId = newPage.attr('id');
	var oldId = oldPage.attr('id');
	if(oldId != 'adminTab') {
		oldPage.destroyDescendants(false);
	}
	//Nothing special to do when showing adminTab
	if(newId == 'adminTab') {
		return;
	}
	var treeType = 'tree';
	queryParams.filter = newPage.attr('title');
	queryParams.tab = newPage.attr('id');
	
	//To manage special tabs
	if(newId == 'mainTab') {
		treeType = 'filter';
		queryParams.filter = '';
	}
	else if(newId == 'tagstab') {
		treeType = 'subfilter';
	}
	else if(newId == 'sumstab') {
		treeType = 'graph';
	}

	newPage.attr('content', dojo.clone(mainPane));
	setupCalendar();
	delete queryParams.host;
	fileForms();
	getTree(treeType);
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
	try {
		queryParams.host = this.attr('value').host;
		queryParams.filter = '';
		getTree('tree');
	}
	catch(err) {
		console.error(err);
	}
	return false;
}

function goHome(evt) {
	delete queryParams.host;
	delete queryParams.filter;
	tabs = dijit.byId('tabs');
	tabs.selectChild('mainTab');
	getTree('filter');
}
