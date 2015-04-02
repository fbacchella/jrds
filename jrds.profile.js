// cd $DOJOSRC
// LANG=C $DOJOSRC/util/buildscripts/build.sh  --profile $JRDS_HOME/jrds.profile.js --bin java basePath=$PWD
// rsync -av release/jrds/dojo/dojo.js $JRDS_HOME/web/dojo/dojo.js
// rsync -av release/jrds/dojo/nls/dojo_en-us.js $JRDS_HOME/web/dojo/nls/dojo_en-us.js
// rsync -av --exclude '*.js.uncompressed.js' --delete release/jrds/{dojo,dojox,dijit} /var/jrds/
dependencies = [
    "dojo/main",
    "dijit/main",
    "dojox/main",
    "dojo/cookie",
    "dojo/number",
    "dojo/parser",
    "dojo/data/ItemFileReadStore",
    "dojo/fx/Toggler",
    "dijit/_base",
    "dijit/CheckedMenuItem",
    "dijit/Dialog",
    "dijit/PopupMenuItem",
    "dijit/TitlePane",
    "dijit/TooltipDialog",
    "dijit/Tree",
    "dijit/WidgetSet",
    "dijit/_base/focus",
    "dijit/_base/place",
    "dijit/_base/popup",
    "dijit/_base/scroll",
    "dijit/_base/sniff",
    "dijit/_base/typematic",
    "dijit/_base/window",
    "dijit/main",
    "dijit/layout/BorderContainer",
    "dijit/layout/ContentPane",
    "dijit/layout/LayoutContainer",
    "dijit/layout/TabContainer",
    "dijit/form/Button",
    "dijit/form/ComboButton",
    "dijit/form/DateTextBox",
    "dijit/form/Form",
    "dijit/form/NumberTextBox",
    "dijit/form/Select",
    "dijit/form/TextBox",
    "dijit/form/TimeTextBox",
    "dijit/form/ValidationTextBox",
    "dojox/widget/Standby",
    "dojox/form/FileUploader"
]

var profile = (function(){
    return {
    	releaseDir: "release",
        releaseName: "jrds",
        action: "release",
        //Optimization
        layerOptimize: "closure",
        optimize: "closure",
        cssOptimize: "comments",
        mini: true,
        stripConsole: "none",
        selectorEngine: "lite",
        packages:[{
            name: "dojo",
            location: "dojo"
        },{
            name: "dijit",
            location: "dijit"
        },{
            name: "dojox",
            location: "dojox"
        }],
        
	    layers: {
	    	"dojo/dojo": {
	    		include: dependencies,
	    		customBase: true,
	    		boot: true
	    	}
	    }
	};
})();
