var queryParams = {};

define("jrds/RootButton",
		[ "dojo/_base/declare",
		  "dijit/form/Button",
		  "dojo",
		  "dijit" ],
		function(declare, button, dojo, dijit) {
return declare("jrds.RootButton", button, {
	'class': 'rootButton',
	onClick: function() {
		var tabs = dijit.byId('tabs');
		var tabSelected = tabs.attr('selectedChildWidget');
		if(tabSelected.id !==  queryParams.landtab) {
			tabs.selectChild(queryParams.landtab);
		} else {
			delete queryParams.host;
			delete queryParams.filter;
			delete queryParams.id;
			queryParams.tab = queryParams.landtab;
			fileForms();
			getTree(tabSelected.isFilters);
		}
	}
});
});

define( "jrds/Autoperiod",
		[ "dojo/_base/declare",
		  "dijit",
		  "dijit/form/Select" ],
    	function(declare, dijit) {
return declare("Autoperiod", dijit.form.Select, {
	postCreate: function() {
		this.set('value', queryParams.autoperiod, false);
		return this.inherited(arguments);
	},
	_setQueryParams: function(value) {
		queryParams.autoperiod = value;
	},
	setPeriod: function(value) {
		this.set('value', value, false);
		this._setQueryParams(value);
	},
	onChange: function(value) {
		this._setQueryParams(value);
		if (value == 0) {
			// manual time period
			return;
		}

		// refresh graph with new new time period
		getGraphList();

		// refresh begin/end filters for new period
		dojo.xhrGet({
			content: { autoperiod: value },
			sync: true,
			url: "queryparams",
			handleAs: "json",
			preventCache: true,
			load: function(response) {
				removeDateConstraints();
				dijit.byId('begin').setDate(new Date(response.begin));
				dijit.byId('end').setDate(new Date(response.end));
				dijit.byId('beginh').setDate(new Date(response.begin));
				dijit.byId('endh').setDate(new Date(response.end));
				updateDateConstraints();
				return response;
			},
			error: function(response) {
				console.error("init query failed with " + response.message);
				return response;
			}
		});
	}
});
});

define(
	"jrds/TimeTextBox",
	[
		"dojo/_base/declare",
		"dojo",
		"dijit",
		"dijit/form/TimeTextBox",
		"dojo/date",
		"dojo/date/locale"
	],
	function(declare, dojo, dijit, timeTextBox) {
		return declare("jrds.TimeTextBox", timeTextBox, {
			'class': 'field fieldHour',
			postCreate: function() {
				var constraint = this.get('constraints');
				constraint.timePattern = 'HH:mm';
				constraint.clickableIncrement = 'T00:30:00';
				constraint.visibleIncrement = 'T00:30:00';
				this._setValue(queryParams[this.queryId]);
				return this.inherited(arguments);
			},
			_setValue: function(date) {
				this.set('value', new Date(1970, 0, 1, date.getHours(), date.getMinutes(), 0), false);
			},
			_setQueryParams: function(date) {
				queryParams[this.queryId].setHours(date.getHours());
				queryParams[this.queryId].setMinutes(date.getMinutes());
			},
			onFocus: function() {
				dijit.byId('autoperiod').setPeriod(0);
			},
			setDate: function(date) {
				this._setValue(date);
				this._setQueryParams(date);
			},
			onChange: function(date) {
				this._setQueryParams(date);
				updateDateConstraints();
				getGraphList();
			}
		});
	}
);

define("jrds/DateTextBox",
       [ "dojo/_base/declare",
         "dojo",
 		 "dijit",
         "dojo/date/locale",
         "dijit/form/DateTextBox"
       ],
       function(declare, dojo, dijit) {
return declare("jrds.DateTextBox", dijit.form.DateTextBox, {
	'class': 'field fieldDay', 
	dayFormat: {
		selector: 'date', 
		datePattern: 'yyyy-MM-dd'
	},
	regExp: "\\d\\d\\d\\d-\\d\\d-\\d\\d",
	postCreate: function() {
		this.get('constraints').timePattern = 'yyyy-MM-dd';
		this._setValue(queryParams[this.id]);
		return this.inherited(arguments);
	},
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
	_setValue: function(date) {
		var newDate = new Date(date.getTime());
		newDate.setHours(0);
		newDate.setMinutes(0);
		newDate.setSeconds(0);
		newDate.setMilliseconds(0);
		this.set('value', newDate, false);
	},
	_setQueryParams: function(date) {
		queryParams[this.id].setFullYear(date.getFullYear());
		queryParams[this.id].setMonth(date.getMonth());
		queryParams[this.id].setDate(date.getDate());
	},
	onFocus: function() {
		dijit.byId('autoperiod').setPeriod(0);
	},
	setDate: function(date) {
		this._setValue(date);
		this._setQueryParams(date);
	},
	onChange: function(date) {
		this._setQueryParams(date);
		if (val('begin').getTime() == val('end').getTime()) {
			if (val('beginh').getTime() >= val('endh').getTime()) {
				if (this.id == 'begin') {
					dijit.byId('beginh').setDate(new Date(val('endh').getTime() - 60000));
				} else {
					dijit.byId('endh').setDate(new Date(val('beginh').getTime() + 60000));
				}
			}
		}
		updateDateConstraints();
		getGraphList();
	}
});
});

define("jrds/PeriodNavigation",
		[ "dojo/_base/declare",
		  "dijit/form/Button",
		  "dojo"],
		function(declare, button, dojo) {
return declare("PeriodNavigation", button, {
	'class': 'periodNavigation',
	postCreate: function() {
		this.set('showLabel', false);
		return this.inherited(arguments);
	},
	onClick: function() {
		var content = {};
		content.autoperiod = queryParams.autoperiod;
		content.begin = queryParams.begin.getTime();
		content.end = queryParams.end.getTime();
		content[this.id] = '';
		dojo.xhrGet( {
			content: content,
			sync: false,
			url: "queryparams",
			handleAs: "json",
			preventCache: true,
			load: function(response) {
				removeDateConstraints();
				dijit.byId('autoperiod').setPeriod(0);
				dijit.byId('begin').setDate(new Date(response.begin));
				dijit.byId('end').setDate(new Date(response.end));
				dijit.byId('beginh').setDate(new Date(response.begin));
				dijit.byId('endh').setDate(new Date(response.end));
				updateDateConstraints();
				getGraphList();
				return response;
			},
			error: function(response) {
				console.error("init query failed with " + response.message);
				return response;
			}
		});
	}
});
});

define("jrds/jrdsTree",
       [ "dojo/_base/declare",
         "dojo",
 		 "dijit",
         "dijit/Tree"
       ],
       function(declare, dojo, dijit) {
return declare("jrdsTree", dijit.Tree, {
	focusNode: function(a) {
		// avoid error on empty trees
		if (a.domNode != null) {
			this.inherited(arguments);
		}
	},
	onLoad: function() {
		if(queryParams.path != null) {
			//This operation destroy the array used as an argument
			//so clone it !
			var tree = this;
			this.attr('path', dojo.clone(queryParams.path)).then(function() {
				if (tree.selectedNode.isExpandable && !tree.selectedNode.isExpanded) {
					tree._expandNode(tree.selectedNode);
				}
			});
		}
		if(this.standby != null) {
			this.standby.hide();
		}
	},
	_onExpandoClick: function(a) {
		a = a.node;
		if (a.deferredExpando != undefined && !a.deferredExpando.isFulfilled()) {
			return;
		}
		this.focusNode(a);
		a.deferredExpando = a.isExpanded ? this._collapseNode(a) : this._expandNode(a)
	},
	getIconClass: function(item, opened){
		if(item.type == 'filter')
			return "filterFolder";
		return this.inherited(arguments);
	}
});
});

define("jrds/StateURLButton",
		[ "dojo/_base/declare",
		  "dijit/form/Button",
		  "dijit/Dialog",
		  "dojo"],
		function(declare, button, dialog, dojo) {
return declare("jrds.StateURLButton", button, {
	onClick: function(){
	    var xhrArgs = {
	            url: "jsonpack",
	            postData: dojo.toJson(queryParams),
	            handleAs: "text",
	            preventCache: false,
	            load: function(data, ioargs) {
	            	// create the dialog:
	            	var myDialog = new dialog({
	            		title: "Graph context",
	            	    content: "<a target='_blank' href='" + data +"'>" + data +"</a>"
	            	});	  
	            	myDialog.show();
	            },
	            error: function(error) {
	            	console.log(error);
	            }
	        }
	        //Call the asynchronous xhrPost
	        dojo.xhrPost(xhrArgs);
	    }
});
});

define("jrds/AutoscaleReset",
		[ "dojo/_base/declare",
		  "dijit/form/ToggleButton" ],
		function(declare, button) {
return declare("jrds.AutoscaleReset", button, {
	constructor: function() {
		this.oldmax = queryParams.max;
		this.oldmin = queryParams.min;		
	},
	onChange: function(checked) {
		if(checked) {
			this.oldmax = queryParams.max;
			this.oldmin = queryParams.min;
			delete queryParams.max;
			delete queryParams.min;
			dijit.byId("max").set('value', '');
			dijit.byId("min").set('value', '');
			getGraphList();
		}
		else {
			queryParams.max = this.oldmax;
			queryParams.min = this.oldmin;
			if(queryParams.max != undefined && queryParams.min != undefined ) {
				dijit.byId("max").set('value', queryParams.max);
				dijit.byId("min").set('value', queryParams.min);				
				getGraphList();
			}
		}
	},
	iconClass: "dijitCheckBoxIcon"
});
});

define("jrds/MinMaxTextBox",
	       [ "dojo/_base/declare",
	         "dijit",
	         "dijit/form/ValidationTextBox" ],
	       function(declare, dijit) {
return declare("jrds.MinMaxTextBox", dijit.form.ValidationTextBox, {
		regExp: "(-?\\d+(.\\d+)?)([a-zA-Z]{0,2})",
		trim: true,
		onFocus: function() {
			dijit.byId("autoscale").set('checked',false);
		},
		onChange: function(value) {
			if(value != undefined && value != "")
				queryParams[this.id] = value;			
		}
});
});

define(
	"jrds/ToogleSort",
	[
		"dojo/_base/declare",
		"dijit/form/ToggleButton"
	],
	function(declare, button) {
		return declare("jrds.ToogleSort", button, {
			onChange: function(checked) {
				queryParams.sort = checked;
				getGraphList();		
			},
			iconClass: "dijitCheckBoxIcon"
		});
	}
);

define(
	"jrds/HostForm",
	[
		"dojo/_base/declare",
	  	"dijit/form/Form"
	],
	function(declare, form) {
		return declare("jrds.HostForm", form, {
			onSubmit: function(){
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
		});
	}
);

define("jrds/RenderForm",
		[ "dojo/_base/declare",
		  "dijit/form/Form" ],
		function(declare, form) {
return declare("jrds.RenderForm", form, {
	onSubmit: function(){
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
});
});

define("jrds/DiscoverHostForm",
		[ "dojo/_base/declare",
		  "dijit/form/Form",
		  "dojo/dom-style",
		  "dojo",
		  "dojo/dom" ],
		function(declare, form, dojoStyle, dojo) {
return declare("jrds.DiscoverHostForm", form, {
	onSubmit: function(){
		// try/catch is mandatory, failure here really submit the form
		try {
			if(document.activeElement.id == 'discoverClear') {
				dojo.place("<pre id='discoverResponse' />", dojo.byId('discoverResponse'), "replace");
				dojoStyle.set( dojo.byId('discoverResponse'), 'display', 'none');
			}
			else {
				if(! this.validate()) {
		            return false;
				}
				
				//Clean the result box before discover
				// TODO wait wheel for the pane
				dojo.place("<pre id='discoverResponse' />", dojo.byId('discoverResponse'), "replace");
				dojoStyle.set( dojo.byId('discoverResponse'), 'display', 'none');

				var queryArgs = { };
				formValues = this.get('value');
				for(key in formValues) {
					if(dijit.byId(key).get('checked') == undefined)
						queryArgs[key] = formValues[key];
					else
						queryArgs[key] = dijit.byId(key).get('checked');
				}
				queryArgs.host = formValues.discoverHostName;
				delete queryArgs.discoverHostName;

				dojo.xhrGet( {
					url: "discover?" + dojo.objectToQuery(queryArgs),
					handleAs: "text",
					load: function(response, ioArgs) {
						var codeTag = dojo.byId('discoverResponse');
						dojoStyle.set( codeTag, 'display', 'block');
						var toPlace = response.replace(/</g, "&lt;").replace(/>/g,"&gt;").replace('/\n/mg','<br>');
						dojo.place("<pre id='discoverResponse'>" + toPlace + "</pre>", codeTag, "replace");
					},
					error: function(response, ioArgs) {
						console.error(response);
					}
				});			
			}
		}
		catch(err) {
			console.error(err);
		}
		return false;
	}
});
});

define("jrds/TabContent",
		[ "dojo/_base/declare",
		  "dijit/layout/ContentPane" ],
		function(declare, layout) {
return declare("jrds.TabContent", layout, {
});
});

define("jrds/Tabs",
		[ "dojo/_base/declare",
		  "dijit/layout/TabContainer" ],
		function(declare, layout) {
return declare("jrds.Tabs", layout, {
	_onKeyDown: function() {},
	postCreate: function() {
		this.watch("selectedChildWidget", this.transitTab);
		return this.inherited(arguments);
	},
	transitTab: function(name, oldPage, newPage) {
	    var newId = newPage.attr('id');
	    var oldId = oldPage.attr('id');
	    if(oldId != 'adminTab') {
	        oldPage.destroyDescendants(false);
	    }
	    this[newPage.callback](newPage);
	},
	adminTabCallback: function () {
		dijit.byId('refreshButton').refreshStatus();
		//Empty and hide an eventually filled discover response
		dojo.place("<pre id='discoverResponse' />", dojo.byId('discoverResponse'), "replace");
		dojo.style( dojo.byId('discoverResponse'), 'display', 'none');
	},
	treeTabCallBack: function(newTab) {
		newTab.attr('content', dojo.clone(mainPane));

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

		fileForms();
		
		//We don't load tree during initial setup
		//It's done later
		if(! keepParams)
			getTree(newTab.isFilters);
	}
});
});

define("jrds/StatusButton",
		[ "dojo/_base/declare",
		  "dijit/form/Button" ],
		function(declare, StatusButton) {
return declare("jrds.StatusButton", StatusButton, {
	'class': 'refreshbutton',
    onClick: function() {
    	this.refreshStatus();
    },
    onShow: function() {
    	this.refreshStatus();
    },
    refreshStatus: function() {
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
});
});

define("jrds/ReloadButton",
		[ "dojo/_base/declare",
		  "dijit/form/Button" ],
		function(declare, button) {
return declare("jrds.ReloadButton", button, {
	onClick: function() {
		dojo.xhrGet( {
			sync: false,
			url: "reload?sync",
			handleAs: "text",
			preventCache: true,
			load: function(response, ioArgs) {
				dijit.byId('refreshButton').refreshStatus();
			},
			error: function(response, ioArgs) {
				console.error(response);
			}
		});		
	}
});
});

function initIndex() {
	initQuery();
	dojo.cookie("treeOneSaveStateCookie", null, {expires: -1});

	//The copy is saved
	var tempMainPane = dojo.byId('mainTab');
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
	dojo.parser.parse()

	setupTabs();
	getGraphList();
}

function initPopup() {
	initQuery();

	dojo.parser.parse();
	getGraphList();
}

function initHistory() {
	initQuery();

	queryParams.history = 1;
	dojo.parser.parse();
	getGraphList();
}

function initQuery() {
	var iq = dojo.xhrGet( {
		content:  dojo.queryToObject(window.location.search.slice(1)),
		sync: true,
		url: "queryparams",
		handleAs: "json",
		preventCache: false,
		load: function(response, ioArgs) {
			for(var key in response) {
				value = response[key];
				if(key == 'begin' || key == 'end') {
					queryParams[key] = new Date(value);
				}
				else if(key == 'sorted' || key == 'autoscale') {
					queryParams[key] = parseBool(value);
				}
				else if(key == 'path' && value.length > 1) {
					var last = value[value.length -1];
					queryParams.id = last.replace(/.*\./, "");
					queryParams.path = value;
				}
				else if(value)
					queryParams[key] = value;

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
		if(key == 'begin' || key == 'end') {
			cleaned[key] = value.getTime();
		}
		else if(value)
			cleaned[key] = value;
	});
	return cleaned;
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
            url = dojo.objectToQuery(this.probe);
        	var detailsWin = window.open("details.html?" + url, "_blank", "width=400,resizable=yes,menubar=no,scrollbars=yes");
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
            url = dojo.objectToQuery(this.history);
        	var historyWin = window.open("history.html?" + url, "_blank", "width=750,menubar=no,status=no,resizable=yes,scrollbars=yes,location=yes");
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
            url = dojo.objectToQuery(this.graph);
        	var popupWin = window.open("download?" + url, "_blank", "menubar=no,status=no,resizable=no,scrollbars=no");
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
		content: cleanParams(['id', 'begin', 'end', 'min', 'max', 'sort', 'autoperiod', 'periodnext', 'periodprevious', 'history', 'filter', 'pid', 'dsName']),
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

function constraints(id) {
	return dijit.byId(id).get('constraints');
}

function val(id) {
	return dijit.byId(id).get('value');
}

function updateDateConstraints() {
	var beginDay = new Date(val('begin').getTime());
	var endDay = new Date(val('end').getTime());

	constraints('begin').max = endDay;
	constraints('end').min = beginDay;

	if (beginDay.getTime() == endDay.getTime()) {
		constraints('beginh').max = new Date(val('endh').getTime() - 60000);
		constraints('endh').min = new Date(val('beginh').getTime() + 60000);
	} else {
		constraints('beginh').max = new Date(1970, 0, 1, 23, 59, 59, 999);
		constraints('endh').min = new Date(1970, 0, 1, 0, 0, 0);
	}
}

function removeDateConstraints() {
	delete constraints('begin').max;
	delete constraints('beginh').max;
	delete constraints('end').min;
	delete constraints('endh').min;
}

function fileForms() {
	if(queryParams.host) {
		dojo.byId("hostForm").host.value = queryParams.host;
	}
	else {
		dojo.byId("hostForm").host.value = '';
	}

	if(queryParams.max != undefined && queryParams.min != undefined) {
		dijit.byId("autoscale").set('checked', false);
		dijit.byId("min").set('value', queryParams.min);
		dijit.byId("max").set('value', queryParams.max);
	}
	else {
		dijit.byId("autoscale").set('checked', true);
		dijit.byId("min").set('value', '');
		dijit.byId("max").set('value', '');
	}

	dijit.byId("sorted").set('checked', parseBool(queryParams.sort));

	updateDateConstraints();
}

function startStandBy(pane) {
	if(dojo.isIE)
		return null;
	var standbyName = 'standby.' + pane;
	var standby = dijit.byId(standbyName);
	if(!standby) {
		standby = new dojox.widget.Standby({
			target: pane,
			id: standbyName,
			duration: 0
		});
		document.body.appendChild(standby.domNode);
	}
	standby.show();
	return standby;
}

function getTree(isFilters) {
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
	store.standby = treeStandby;

	store.fetch({
		onError: function(errData, request) {
			console.log("on error detected in dojo.data.ItemFileReadStore:" + errData);
			if(request.store.standby != null) {
				request.store.standby.hide();
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
		autoExpand: false,
		isFilters: isFilters,
		standby: treeStandby
	}, treeOneDiv);
}

function toggleFold() {
	var tree = dijit.byId('treeOne');
	var foldbutton = dojo.byId('foldButton');
	if (dojo.hasClass(foldbutton, 'dijitFolderClosed')) {
		tree.expandAll();
		dojo.removeClass(foldbutton, "dijitFolderClosed");
		dojo.addClass(foldbutton, "dijitFolderOpened");
	} else {
		tree.collapseAll();
		dojo.addClass(foldbutton, "dijitFolderClosed");
		dojo.removeClass(foldbutton, "dijitFolderOpened");
	}
}

function loadTree(item,  node){
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
		queryParams.path = node.getTreePath().map(function(node) { return node.id instanceof Array ? node.id[0] : node.id; });
	}
}

function popup(url,id) {
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
	return popupWin = window.open("popup.html?" + url, "_blank", height + "," + width + ",menubar=no,status=no,resizable=yes,scrollbars=yes,location=yes");
}

function setupTabs() {
    var tabWidget = dijit.byId('tabs');
    var i = 0;
    var isFilters;
	
    for(key in queryParams.tabslist) {
        var pane = dijit.byId(key)
        if(pane == undefined) {
		    pane = new jrds.TabContent ({
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
		//We keep the old id, because switching tabs reset it
		oldId = queryParams.id;
		tabWidget.selectChild(queryParams.tab);
		queryParams.id = oldId;
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

function updateStatus(statusInfo) {
	var statusNode = dojo.byId('status');
	statusNode.innerHTML = '';

	var row1 = dojo.create("div", {'class': "statusrow"}, statusNode);
	dojo.create('span', {'class': 'statuslabel', innerHTML: 'Hosts'}, row1);
	dojo.create('span', {'class': 'statusvalue', innerHTML: statusInfo.Hosts}, row1);

	var row2 = dojo.create("div", {'class': "statusrow"}, statusNode);
	dojo.create('span', {'class': 'statuslabel', innerHTML: 'Probes'}, row2);
	dojo.create('span', {'class': 'statusvalue', innerHTML: statusInfo.Probes}, row2);
    
    for(i in statusInfo.Timers) {
        stats = statusInfo.Timers[i]
        var name = stats.Name;
        var lastCollect = stats.LastCollect + "s ago";
        if(stats.LastDuration == 0) {
            lastCollect = 'not run';
            lastDuration = 'not run';
        }
        else if(stats.LastDuration < 1000) {
            var lastDuration = stats.LastDuration + "ms";
        }
        else {
            var lastDuration = (stats.LastDuration / 1000).toFixed(0) + "s";
        }
        var rowa = dojo.create("div", {'class': "statusrow"}, statusNode);
        dojo.create('span', {'class': 'statuslabel', innerHTML: 'Timer'}, rowa);
        dojo.create('span', {'class': 'statusvalue', innerHTML: name}, rowa);

        var rowb = dojo.create("div", {'class': "statusrow"}, statusNode);
        dojo.create('span', {'class': 'statuslabel', 'style': 'text-align: right;', innerHTML: 'Last collect'}, rowb);
        dojo.create('span', {'class': 'statusvalue', innerHTML: lastCollect}, rowb);

        var rowc = dojo.create("div", {'class': "statusrow"}, statusNode);
        dojo.create('span', {'class': 'statuslabel', 'style': 'text-align: right;', innerHTML: 'Last duration'}, rowc);
        dojo.create('span', {'class': 'statusvalue', innerHTML: lastDuration}, rowc);
    }

	var row5 = dojo.create("div", {'class': "statusrow"}, statusNode);
	dojo.create('span', {'class': 'statuslabel', innerHTML: 'Generation'}, row5);
	dojo.create('span', {'class': 'statusvalue', innerHTML: statusInfo.Generation}, row5);
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
