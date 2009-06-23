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

function fileForms() {	
    var dateForm = dojo.byId("dateForm");
	dateForm.end.value =  queryParams.end;
	dateForm.begin.value =  queryParams.begin;
	dateForm.max.value =  queryParams.max;
	dateForm.min.value = queryParams.min;
	
	if(queryParams.max || queryParams.min) {
		setAutoscale(false);
	    dijit.byId("max").attr('value',queryParams.max);
	    dijit.byId("min").attr('value',queryParams.min);
	}

    var autoperiod = new dojox.form.DropDownSelect({
         	value:queryParams.autoperiod,
        	name: "autoperiod"
         }, "autoperiod");
    autoperiod.dropDown.oldItemClick=autoperiod.dropDown.onItemClick;
    autoperiod.dropDown.onItemClick = setScale;

    if(queryParams.host)
    	dojo.byId("hostForm").host.value = queryParams.host;
}

function updateScale(value) {
	queryParams[this.id] = value;
}

function setScale(item, evt) {
	queryParams.autoperiod = item.option.value;
	reloadTree();
    return this.oldItemClick(item, evt);
}

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
	queryParams.max = "";
	queryParams.min = "";

    var dateForm = dojo.byId("dateForm");
	dateForm.max.value =  queryParams.max;
	dateForm.min.value = queryParams.min;
	
	reloadTree();
}

function setAutoscale(value) {
	var autoscale = dijit.byId("autoscale");
	autoscale.attr('checked',value)	
}

function setupCalendar() {
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
        }
    });
   new DayHourTextBox({
            name: "begin",
    }, "begin");
    new DayHourTextBox({
            name: "end",
    }, "end");
	
}

		
