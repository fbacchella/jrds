var queryParams = {};

dojo.declare("jrdsTree", dijit.Tree, {
	onLoad: function() {
		if(queryParams.path != null) {
			//This operation destroy the array used as an argument
			//so clone it !
			this.attr('path', dojo.clone(queryParams.path));
		}
		if(this.standby != null) {
			this.standby.hide();
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
	onChange: function(date) {
		if(date) {
			dijit.byId('autoperiod').attr('value', 0); 
			var queryDate = queryParams[this.queryId];
			if(queryDate) {
				var elems = queryDate.split(' ');
				queryParams[this.queryId] = elems[0] + ' ' + dojo.date.locale.format(date, this.hourFormat);
			}
			else {
				dateText = date.getFullYear() + '-' + date.getMonth() + '-' + date.getDate();
				queryParams[this.queryId] = dateText + ' ' + dojo.date.locale.format(date, this.hourFormat);
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

dojo.declare('kgf.dijit.FixedFileUploader', dojox.form.FileUploader, {
    // summary:
    //    Private class containing fixes to FileUploader behavior.

    getHiddenWidget: function() {
      var widget = this.inherited(arguments);
      if (widget && dojo.position(widget.domNode).h > 0) {
        //false positive - sure the widget has onShow, but it's already shown!
        //(workaround to Dojo bug #11039)
        //TODO: will need to see if this check suffices for situations where
        //it's actually hidden (haven't used anywhere like that yet).
        return null;
      }
      return widget;
    }
  });


function initIndex() {
	initQuery();

	dojo.cookie("treeOneSaveStateCookie", null, {expires: -1});

	dojo.addOnLoad(function(){
		//The copy is saved
		var tempMainPane = dojo.byId('mainPane');
		mainPane = dojo.clone(tempMainPane);
		dojo.destroy(tempMainPane);

		dojo.xhrGet( {
			url: 'discoverhtml',
			handleAs: "text",
			sync: true,
			load: function(response, ioArgs) {
				var discoverAutoBlock = dojo.byId('discoverAutoBlock');
				dojo.place(response, discoverAutoBlock, 'replace');
			}
		});

		//The parse can be done
		dojo.parser.parse();

		setupCalendar();
		setupTabs();
		setupDisplay();
	});	
}

function initPopup() {
	initQuery();

	dojo.addOnLoad(function(){
		getGraphList();
	});
}

function initHistory() {
	initQuery();

	dojo.addOnLoad(function(){
		queryParams.history = 1;
		getGraphList();
	});
}

function initQuery() {
	var iq = dojo.xhrGet( {
		content:  dojo.queryToObject(window.location.search.slice(1)),
		sync: true,
		url: "queryparams",
		handleAs: "json",
		preventCache: false,
		load: function(response, ioArgs) {
			queryParams = response;
			if(queryParams.path && queryParams.path.length > 1 ) {
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
	dojo.attr(graphImg, "src", "graph?" + dojo.objectToQuery(graphParams));		
	dojo.place(graphImg, graphPane);
}

function doGraphList(result) {
	var graphPane = dojo.byId("graphPane");
	dojo.empty(graphPane);
	for(i in result.items) {
		graph = result.items[i];
		var  graphBlock = dojo.create("div", {"class": "graphblock"}, graphPane);

		var graphImg = dojo.create("img", {
			"class": "graph",
			name: graph.name,
			title: graph.name,
			id: graph.id,
			src: "graph?" + dojo.objectToQuery(graph.graph)
		}, 
		graphBlock);

		if(graph.width)
			dojo.attr(graphImg, "width", graph.width);
		if(graph.height)
			dojo.attr(graphImg, "height", graph.height);

		var iconsList =  dojo.create("div", {"class": "iconslist"}, graphBlock);

        //Create the popup button
		var application_double = dojo.create("img", {
                "class": "icon",
                src: "img/application_double.png",
                height: 16,
                width: 16,
                title: "Popup the graph"
            },
            iconsList);
        dojo.connect(application_double, "onclick", graph, function(){
            popup(dojo.objectToQuery(this.graph),this.id);
        });

        //Create the probe's details button
		var application_view_list = dojo.create("img", {
                "class": "icon",
                height: 16,
                width: 16,
                src: "img/application_view_list.png",
                title: "Graph details"
            },
            iconsList);
        dojo.connect(application_view_list, "onclick", graph, function(){
            details(dojo.objectToQuery(this.probe),this.probename);
        });

        //Create the history button
		var time = dojo.create("img", {
                "class": "icon",
                height: 16,
                width: 16,
                src: "img/date.png",
                title: "Graph history"
            },
            iconsList);
        dojo.connect(time, "onclick", graph, function(){
            history(dojo.objectToQuery(this.history), this.probename);
        });

        //Create the save button
		var disk = dojo.create("img", {
                "class": "icon",
                height: 16,
                width: 16,
                src: "img/disk.png",
                title: "Save data"
            },
            iconsList);
        dojo.connect(disk, "onclick", graph, function(){
            save(dojo.objectToQuery(this.graph), this.probename);
        });
	}
	if(this.standby != null)
		this.standby.hide();

	return result;
}

function getGraphList() {
	if(! queryParams.id &&  ! queryParams.pid)
		return;
	
	var graphStandby = startStandBy('graphPane');

	return dojo.xhrGet( {
		content: cleanParams(['id', 'begin', 'end', 'min', 'max', 'sort', 'autoperiod', 'history', 'filter', 'pid', 'dsName']),
		url: "jsongraph",
		handleAs: "json",
		load: doGraphList,
		standby: graphStandby
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
			dijit.byId(id).attr('value', null);
		});
	}

	var autoperiod = dijit.byId('autoperiod'); 
	autoperiod.attr('value', queryParams.autoperiod);

	setAutoscale(queryParams.max == null || queryParams.min == null);
		
	dijit.byId("sorted").attr('checked', parseBool(queryParams.sort));
}

function setupDisplay() {
	if(queryParams.id) {
		getGraphList();
	}
}

function startStandBy(pane) {
	if(dojo.isIE)
		return null;
	var standbyName = 'standby.' + pane;
	var standby = dijit.byId(standbyName);
	if(standby != null) {
		standby.destroyRecursive(false);
	}
	standby = new dojox.widget.Standby({
		target: pane,
		id: standbyName
	});
	document.body.appendChild(standby.domNode);
	standby.show();
	return standby;
}

function getTree(isFilters, unfold) {	
	var treeStandby = startStandBy('treePane');
	
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

	store.fetch({
		onError: function(errData, request) {
			console.log("on error detected in dojo.data.ItemFileReadStore:" + errData);
			var standby = dijit.byId('standby.' );
			if(standby != null) {
				standby.hide();
			}
		}
	});
	
	var treeModel = new dijit.tree.ForestStoreModel({
		store: store,
		query: {type: treeType},
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
		isFilters: isFilters,
		standby: treeStandby
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

function getTreeNodeUp(node) {
	var nodeInfo = node.item.id;
	if(nodeInfo instanceof Array) {
		nodeInfo = nodeInfo[0];
	}
	if(! node.getParent()) {
		return new Array(nodeInfo);
	}
	retValue = getTreeNodeUp(node.getParent());
	retValue.push(nodeInfo);
	return retValue;
}

function loadTree(item,  node){
	var tree = node.tree;

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
		
		queryParams.path = getTreeNodeUp(node);
	}
}

function toogleSort() {
	queryParams.sort = dijit.byId("sorted").attr('checked');
	getGraphList();
}

function setupCalendar() {
	beginTimeTextBox = new HourBox( {
		'class': 'field fieldHour', 
		hourFormat: hourFormat,  
		constraints: hourconstraints,
		name: 'beginh',
		value: '',
		queryId: 'begin'
	}, 'beginh');
	endTimeTextBox = new HourBox( {
		'class': 'field fieldHour', 
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
	var detailsWin = window.open("details?" + url, name, "width=400,resizable=yes,menubar=no,scrollbars=yes");
}

function popup(url,id)
{
	var img;
	if(id)
		img = document.getElementById(id);
	var width = "width=703";
	var height;
	var title;
	if(img != null) {
		height = "height=" + (img.height + 34);
		title = img.name;
	}
	else {
		height = "height=500";
	}
	return popupWin = window.open("popup.html?" + url, "_blank" , height + "," + width + ",menubar=no,status=no,resizable=yes,scrollbars=yes,location=yes");
}

function save(url, name)
{
	var popupWin = window.open("download?" + url, name , "menubar=no,status=no,resizable=no,scrollbars=no");
}

function history(url, name)
{
	var historyWin = window.open("history.html?" + url, "_blank", "width=750,menubar=no,status=no,resizable=yes,scrollbars=yes,location=yes");
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
			dijit.byId(id).attr('value', null);
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

function setupTabs() {
    var tabWidget = dijit.byId('tabs');
    dojo.connect(tabWidget,"_transition", transitTab);
    var i = 0;
    var isFilters;
	
    for(key in queryParams.tabslist) {
        var pane = dijit.byId(key)
        if(pane == undefined) {
		    pane = new dijit.layout.ContentPane({
	            title:  queryParams.tabslist[key].label,
	            id: queryParams.tabslist[key].id,
	            isFilters: queryParams.tabslist[key].isFilters,
	            callback: queryParams.tabslist[key].callback
	        });
		    tabWidget.addChild(pane, i++);
		}
		else {
		    pane.isFilters = queryParams.tabslist[key].isFilters;
		    pane.callback = queryParams.tabslist[key].callback;
		}		
	}

	//Tab was specified, so go to it, no need to load tree
	if(queryParams.tab) {
		tabWidget.selectChild(queryParams.tab);
	}
	//We're in the default tab, load the needed try now
	else {
        var child = tabWidget.getChildren()[0];
        child.keepParams = true;
	    tabWidget.selectChild(child);
		if(queryParams.host || queryParams.tree || queryParams.filter)
			isFilters = false;
		getTree(isFilters);
	}
	
    //adminTab is in index.html, so remove it if it's not in the explicit tab list
	if(queryParams.tabslist['adminTab'] == undefined) {
	    var adminTab = dijit.byId("adminTab");
	    tabWidget.removeChild(adminTab);
	}
}

function transitTab(newPage, oldPage){
    var newId = newPage.attr('id');
    var oldId = oldPage.attr('id');
    if(oldId != 'adminTab') {
        oldPage.destroyDescendants(false);
    }
    window[newPage.callback](newPage);
}

function treeTabCallBack(newTab) {
	newTab.attr('content', dojo.clone(mainPane));

	var treePane = dojo.byId('treePane');
    var keepParams = newTab.keepParams;

	//keepParams used during page setup, to keep queryParams fields
	if(keepParams) {
		delete newTab.keepParams;
	}
	else {
		if(queryParams.host)
			delete queryParams.host;
		if(queryParams.filter)
			delete queryParams.filter;
		if(queryParams.id)
			delete queryParams.id;
		queryParams.tab = newTab.attr('id');
		queryParams.landtab = newTab.attr('id');
	}

	setupCalendar();
	fileForms();
	
	//We don't load tree during initial setup
	//It's done later
	if(! keepParams)
		getTree(newTab.isFilters);
}

function sendReload(evt) {
	var iq = dojo.xhrGet( {
		sync: true,
		url: "reload?sync",
		handleAs: "text",
		preventCache: true,
		load: function(response, ioArgs) {
			refreshStatus();
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

function refreshStatus() {
	dojo.xhrGet( {
		url: "status?json",
		handleAs: "json",
		preventCache: true,
		load: function(response, ioArgs) {
			updateStatus(response);
		},
		error: function(response, ioArgs) {
			console.error("can't refresh status " + response.message);
			return response;
		}
	});
}

function updateStatus(statusInfo) {
	var oldButton = dijit.byId('refreshButton');
	if(oldButton != null)
		oldButton.destroyRecursive(false);

	var statusNode = dojo.byId('status');
	statusNode.innerHTML = '';

	var row1 = dojo.create("div", {'class': "statusrow"}, statusNode);
	dojo.create('span', {'class': 'statuslabel', innerHTML: 'Hosts'}, row1);
	dojo.create('span', {'class': 'statusvalue', innerHTML: statusInfo.Hosts}, row1);

	var row2 = dojo.create("div", {'class': "statusrow"}, statusNode);
	dojo.create('span', {'class': 'statuslabel', innerHTML: 'Probes'}, row2);
	dojo.create('span', {'class': 'statusvalue', innerHTML: statusInfo.Probes}, row2);

	var lastCollect = statusInfo.LastCollect + "s ago";
	var lastDuration = (statusInfo.LastDuration / 1000).toFixed(0) + "s";
	if(statusInfo.LastDuration == 0) {
		lastCollect = 'not run';
		lastDuration = 'not run';
	}
	else if(statusInfo.LastDuration < 1000) {
		var lastDuration = statusInfo.LastDuration + "ms";
	}
	var row3 = dojo.create("div", {'class': "statusrow"}, statusNode);
	dojo.create('span', {'class': 'statuslabel', innerHTML: 'Last collect'}, row3);
	dojo.create('span', {'class': 'statusvalue', innerHTML: lastCollect}, row3);

	var row4 = dojo.create("div", {'class': "statusrow"}, statusNode);
	dojo.create('span', {'class': 'statuslabel', innerHTML: 'Last duration'}, row4);
	dojo.create('span', {'class': 'statusvalue', innerHTML: lastDuration}, row4);

	var row5 = dojo.create("div", {'class': "statusrow"}, statusNode);
	dojo.create('span', {'class': 'statuslabel', innerHTML: 'Generation'}, row5);
	dojo.create('span', {'class': 'statusvalue', innerHTML: statusInfo.Generation}, row5);

	var button = dojo.create("button", {id: 'refreshButton'});
	dojo.place(button, statusNode, 'after');

   	new dijit.form.Button({
   		'class': 'refreshbutton',
        label: "Refresh",
        onClick: refreshStatus
    }, "refreshButton");
}

function discoverHost(evt) {
	try {
		var queryArgs = { };
		
		for(key in this.attr('value')) {
			if(dijit.byId(key).attr('checked') == undefined)
				queryArgs[key] = this.attr('value')[key];
			else
				queryArgs[key] = dijit.byId(key).attr('checked');
		}
		queryArgs.host = this.attr('value').discoverHostName;

		dojo.xhrGet( {
			url: "discover?" + dojo.objectToQuery(queryArgs),
			//url: "discover?" + dojo.objectToQuery(this.attr('value')),
			handleAs: "text",
			load: function(response, ioArgs) {
				var codeTag = dojo.byId('discoverResponse');
				dojo.style( codeTag, 'display', 'block');
				var toPlace = response.replace(/</g, "&lt;").replace(/>/g,"&gt;").replace('/\n/mg','<br>');
				dojo.place("<pre id='discoverResponse'>" + toPlace + "</pre>", codeTag, "replace");
			},
			error: function(response, ioArgs) {
			}
		});
	}
	catch(err) {
		console.log(err);
		console.error(err);
	}
	return false;
}

var filesSelect;

function doUpload(){
	dojo.byId("filesList").innerHTML = "uploading...";
	filesSelect.upload();
}

function setAdminTab() {
	refreshStatus();

	//Setup the discoverer
	var form = dojo.byId('discoverForm');
	form.discoverHostName.value = '';
	form.discoverSnmpCommunity.value = 'public';
	form.discoverSnmpPort.value = '161';
	dojo.style( dojo.byId('discoverResponse'), 'display', 'none');

	//Set up the uploader
	var uploader = dijit.byId('filesSelect');
	if(! uploader) {
		filesSelect = new kgf.dijit.FixedFileUploader({
			uploadUrl: 'upload',
			selectMultipleFiles: true,
			force: 'html',
			fileListId: 'filesList',
			fileMask: [],
			onChange: function(a) {
				dojo.style('filesList', 'height', 'auto');
			},
			onComplete: filesLoaded
		}, 'filesSelect');
		//The uploader set a bad lineHeight
		dojo.style('filesSelect', 'lineHeight', '');
	}
	else {
		//Don't forget to clean the file list
		var filesResult = dojo.byId('filesResult');
		dojo.place('<div id="filesResult"></div>', filesResult, 'replace');
	}
}

function filesLoaded(e) {
	try {
		var filesResult = dojo.byId('filesResult');
		var result = '<textarea id="filesResult" rows="' + e.length + '" cols="100">';
		dojo.forEach(e, function(f){
			result += f.name;
			if(parseBool(f.parsed)) {
				result += ': OK';
			}
			else {
				result += ': ' + f.error;
			}
			result += '\n';
		});
		result += "</textarea>";
		dojo.place(result, filesResult, 'replace');
	
		var filesList = dojo.byId('filesList');
		dojo.place('<div id="filesList" />', filesList, 'replace');
		dojo.style(filesList, 'height', '20px');
	}
	catch(err) {
		console.error(err);
	}
}

function dateNavPrevious() {
	console.log(queryParams);
	if(queryParams.autoperiod == 0) {
		var begin = dojo.date.locale.parse(queryParams.begin, 'yyyy-MM-dd HH:mm');
		var end = dojo.date.locale.parse(queryParams.end, 'yyyy-MM-dd HH:mm');
		console.log(end - begin);
	}
	console.log(queryParams);
	var dayTimeFormat = {
		datePattern: 'yyyy-MM-dd',
		timePattern: 'HH:mm'
	};
	if(queryParams.autoperiod == 0) {
		var begin = dojo.date.locale.parse(queryParams.begin, dayTimeFormat);
		var end = dojo.date.locale.parse(queryParams.end, dayTimeFormat);
		console.log(begin);
		console.log(end);
		console.log(end - begin);
	}
}

function dateNavNext() {
	console.log(queryParams);
	var dayTimeFormat = {
		datePattern: 'yyyy-MM-dd',
		timePattern: 'HH:mm'
	};
	if(queryParams.autoperiod == 0) {
		var begin = dojo.date.locale.parse(queryParams.begin, dayTimeFormat);
		var end = dojo.date.locale.parse(queryParams.end, dayTimeFormat);
		console.log(begin);
		console.log(end);
		console.log(end - begin);
	}
}
