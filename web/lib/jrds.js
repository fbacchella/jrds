
var queryParams = {};

//Used to get a clean copy of the pane that will be in each tab
var mainPane;

initQuery();

dojo.cookie("treeOneSaveStateCookie", null, {expires: -1});

dojo.addOnLoad(function(){
	//The copy is saved
	var tempMainPane = dojo.byId('mainPane');
	mainPane = dojo.clone(tempMainPane);
	dojo.destroy(tempMainPane);
	//The parse can be done
	dojo.parser.parse();
	
	var tabs = dijit.byId("tabs");
	dojo.connect(tabs,"_transition", transitTab);
	setupCalendar();
	setupDisplay();
});

dojo.declare("jrdsTree", dijit.Tree, {
	onLoad: function() {
		if(queryParams.path != null) {
			//This operation destroy the array used as an argument
			//so clone it !
			this.attr('path', dojo.clone(queryParams.path));
		}
	},
	getIconClass: function(item, opened){
		if(item.type == 'filter')
			return "filterFolder";
		return this.inherited(arguments);
	}
});	

var hourFormat = {
		timePattern: 'HH:mm',
		selector: 'time'
};

var hourconstraints = {
		timePattern:'HH:mm',
		clickableIncrement:'T00:30:00',
		visibleIncrement:'T00:30:00',
		visibleRange:'T05:00:00'
};

dojo.declare("HourBox",dijit.form.TimeTextBox , {
	'class': 'field fieldHour', 
	onChange: function(date) {
		if(date) {
			dijit.byId('autoperiod').attr('value', 0); 
			var queryDate = queryParams[this.queryId];
			if(queryDate) {
				var elems = queryDate.split(' ');
				queryParams[this.queryId] = elems[0] + ' ' + dojo.date.locale.format(date, this.hourFormat);
			}
			else {
				queryParams[this.queryId] = '1970-01-01 ' + dojo.date.locale.format(date, this.hourFormat);
			}
		}
		return this.inherited(arguments);
	}
});

function dayRegExp() {
	return "\\d\\d\\d\\d-\\d\\d-\\d\\d";
};

dojo.declare("DayHourTextBox", dijit.form.DateTextBox, {
	dayFormat: {
		selector: 'date', 
		datePattern: 'yyyy-MM-dd'
	},
	hourFormat: hourFormat,        
	dateStr: '',
	format: function(date) {
		if(date)
			return dojo.date.locale.format(date, this.dayFormat);
		else
			return '';
	},
	parse: function(date) {
		if(date)
			return dojo.date.locale.parse(date, this.dayFormat);
		else
			return '';
	},
	serialize: function(date) {
		if(date)
			return dojo.date.locale.format(date, this.dayFormat);
		else
			return '';
	},
	onChange: function(date) {
		if(date) {
			queryParams.autoperiod = 0;

			//Let's try to keep the existing hour
			var hour = this.resetHour;
			if(queryParams[this.id]) {
				var dateArray = queryParams[this.id].split(' ');
				if(dateArray.length == 2) {
					hour = dateArray[1];
				}
			}
			//If hour in form not defined, set it.
			if(! this.timeBox.attr('value') ) {
				this.timeBox.attr('value', dojo.date.locale.parse(hour, this.hourFormat));
			}
			var sdate = dojo.date.locale.format(date, this.dayFormat);
			queryParams[this.id] = sdate + ' ' + hour;
			dijit.byId('autoperiod').attr('value', 0); 
		}
		return this.inherited(arguments);
	}
});

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
		dojo.attr(time, "src","img/date.png");
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

		dojo.place(application_double, iconsList);
		dojo.place(application_view_list, iconsList);
		dojo.place(time, iconsList);
		dojo.place(disk, iconsList);

		dojo.place(graphImg, graphBlock);
		dojo.place(iconsList, graphBlock);
	}
	return result;
}

function getGraphList() {	
	var graphPane = dojo.byId("graphPane");
	dojo.empty(graphPane);
	return dojo.xhrGet( {
		content: cleanParams(['id', 'begin', 'end', 'min', 'max', 'sort', 'autoperiod', 'history', 'filter', 'pid', 'dsName']),
		url: "jsongraph",
		handleAs: "json",
		load: doGraphList
	});
}

function parseBool(stringbool){
	var argtype = typeof stringbool;
	if(argtype == 'boolean')
		return stringbool;
	if(stringbool == null)
		return false;
    switch(dojo.trim(stringbool.toLowerCase())){
    	case "true":
        case "yes":
        case "1":
        	return true;
        default:
        	return false;
    }
}

function fileForms() {
	if(queryParams.host) {
		dojo.byId("hostForm").host.value = queryParams.host;
	}
	else {
		dojo.byId("hostForm").host.value = '';
	}

	var dateForm = dojo.byId("dateForm");
	if(queryParams.begin && queryParams.end) {
		var beginarray = queryParams.begin.split(" ");
		var endarray = queryParams.end.split(" ");
		if(beginarray.length == 2 && endarray.length == 2) {
			dijit.byId('begin').attr('value', new Date(beginarray[0]));
			dijit.byId('beginh').attr('value', new Date(beginarray[1]));
			dijit.byId('end').attr('value', new Date(endarray[0]));
			dijit.byId('endh').attr('value', new Date(endarray[1]));
		}
	}
	else {
		dojo.forEach(['begin', 'beginh', 'end', 'endh'], function(id, i) {
			dijit.byId(id).attr('value', '');
		});
	}

	var autoperiod = dijit.byId('autoperiod'); 
	autoperiod.attr('value', queryParams.autoperiod);

	setAutoscale(queryParams.max == null || queryParams.min == null);
		
	dijit.byId("sorted").attr('checked', parseBool(queryParams.sort));
}

function setupDisplay() {
	var tabWidget = dijit.byId('tabs');
	var i = 0;

	dojo.forEach(queryParams.tabslist, function(key) {
		var pane = new dijit.layout.ContentPane({
	        title: key.label,
	        id: key.id,
	        isFilters: key.isFilters
	    });
		tabWidget.addChild(pane, i++);		
	    if(key.selected && ! queryParams.tab) {
	    	pane.keepParams = true;
			tabWidget.selectChild(pane);
	    }	    	
	});

	if(queryParams.tab) {
		tabWidget.selectChild(queryParams.tab);
	}

	fileForms();

	if(queryParams.id) {
		getGraphList();
	}
}

function getTree(isFilters, unfold) {	
	var foldbutton = dojo.byId('foldButton');
	var treeType;
	if(isFilters) {
		dojo.style(foldbutton, 'display', 'none');
		treeType = 'filter';
	}
	else {
		dojo.style(foldbutton, 'display', 'block');
		treeType = 'tree';
	}
	//treeType can be :
	// filter, subfilter for a custom tree
	// graph, node, tree filter
	// graph, node, tree for an host

	var treeOne = dijit.byId("treeOne");
	if( treeOne) {
		treeOne.destroyRecursive(true);
	}

	var store = new dojo.data.ItemFileReadStore({
		url: "jsontree?" + dojo.objectToQuery(cleanParams(['host', 'filter', 'tree', 'tab']))
	});

	var treeModel = new dijit.tree.ForestStoreModel({
		store: store,
		query: {"type": treeType },
		rootId: '0.0',
		rootLabel: 'All filters',
		childrenAttrs: ["children"]
	});
	
	treeOneDiv = dojo.create("div", {id: 'treeOne'}, dojo.byId('treePane'), "last");
	
	treeOne = new jrdsTree({
		model: treeModel,
		showRoot: false,
		onClick: loadTree,
		persist: false,
		autoExpand: true == unfold,
		isFilters: isFilters
	}, treeOneDiv);

}

function doUnfold() {
	var foldbutton = dojo.byId('foldButton');
	dojo.toggleClass(foldbutton, "dijitFolderClosed");
	dojo.toggleClass(foldbutton, "dijitFolderOpened");
	var tree = dijit.byId('treeOne');
	var unfold = tree.attr('autoExpand');
	var model = tree.attr('model');
	var type = model.query.type;
	getTree(tree.isFilters, true != unfold);
}

function loadTree(item,  node){
	var tree = dijit.byId("treeOne");
	if(item.filter) {
		queryParams.filter = item.filter[0];
		delete queryParams.host;
		delete queryParams.tree;
		getTree(false);
	}
	else if(item.host) {
		queryParams.host = item.host[0];
		delete queryParams.filter;
		delete queryParams.tree;
		getTree(false);
	}
	else if(item.tree) {
		queryParams.tree = item.host[0];
		delete queryParams.host;
		delete queryParams.filter;
		getTree(false);
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
	beginTimeTextBox = new HourBox( {
		hourFormat: hourFormat,  
		constraints: hourconstraints,
		name: 'beginh',
		value: '',
		queryId: 'begin'
	}, 'beginh');
	endTimeTextBox = new HourBox( {
		hourFormat: hourFormat,  
		constraints: hourconstraints,
		name: 'endh',
		value: '',
		queryId: 'end'
	}, 'endh');

	new DayHourTextBox({
		'class': 'field fieldDay', 
		//A bug in dojo 1.4+ ?
		regExpGen: dayRegExp,
		name: "begin",
		resetHour: '00:00',
		timeBox: beginTimeTextBox
		}, "begin");
	
	new DayHourTextBox({
		'class': 'field fieldDay', 
		//A bug in dojo 1.4+ ?
		regExpGen: dayRegExp,
		name: "end",
		resetHour: '23:59',
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

function sendlink()
{
    var xhrArgs = {
        url: "jsonpack",
        postData: dojo.toJson(queryParams),
        handleAs: "text",
        preventCache: false,
        load: function(data, ioargs) {
            linkdialog(data, ioargs);
        },
        error: function(error) {
        }
    }
    //Call the asynchronous xhrPost
    var deferred = dojo.xhrPost(xhrArgs);
}

function linkdialog(data, ioargs) {
	// create the dialog:
	var myDialog = new dijit.Dialog({
		title: "Graph context",
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
	if(! value && value != 0 )
		value = null;
	queryParams[this.id] = value;
}

function setAutoperiod(value) {
	queryParams.autoperiod = value;
	//value = 0 means manual time period, so don't mess with the period fields
	if(value != 0) {
		submitRenderForm();
		dojo.forEach(['begin', 'beginh', 'end', 'endh'], function(id, i) {
			dijit.byId(id).attr('value', '');
		});
	}
}

function submitRenderForm(evt) {
	try {
		if( queryParams.id == null )
			return false;
		if(queryParams.autoperiod == 0 && (queryParams.begin == null || queryParams.end == null) )
			return false;
		if( (queryParams.max != null && queryParams.min == null) || (queryParams.max == null && queryParams.min != null) )
			return false;
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

	newPage.attr('content', dojo.clone(mainPane));

	var treeType = 'tree';

	//keepParams used during page setup, to keep queryParams fields
	if(newPage.keepParams) {
		delete newPage.keepParams;
	}
	else {
		delete queryParams.host;
		delete queryParams.filter;
		delete queryParams.id;
		queryParams.tab = newPage.attr('id');
		queryParams.landtab = newPage.attr('id');
	}
	//To manage special tabs
	if(newId == 'sumstab') {
		treeType = 'graph';
	}

	setupCalendar();
	fileForms();
	getTree(newPage.isFilters);
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
			console.error(response);
		}
	});
}

function searchHost(evt) {
	try {
		queryParams.host = this.attr('value').host;
		delete queryParams.filter;
		delete queryParams.id;
		delete queryParams.tab;
		getTree(false);
	}
	catch(err) {
		console.error(err);
	}
	return false;
}

function goHome(evt) {
	var tabs = dijit.byId('tabs');
	var tabSelected = tabs.attr('selectedChildWidget');
	if(tabSelected.id !=  queryParams.landtab)
		tabs.selectChild(queryParams.landtab);
	else {
		delete queryParams.host;
		delete queryParams.filter;
		delete queryParams.id;
		queryParams.tab = queryParams.landtab;
		fileForms();
		getTree(tabSelected.isFilters);
	}		
}
