// Title: COOLjsTree
// Version: 1.4.0.
// URL: http://javascript.cooldev.com/scripts/cooltree/
// Last Modify: 11-28-2002 (mm-dd-yyyy)
// Author: Sergey Nosenko <jsinfo@cooldev.com>
// Notes: Registration needed to use this script on your web site.
// Registration for this version is FREE for evaluating, personal and non-profit use.
// Copyright (c) 2001-2002 by CoolDev.Com
// Copyright (c) 2001-2002 by Sergey Nosenko
window.NTrees=[];
function bw_check(){var is_major=parseInt(navigator.appVersion);this.nver=is_major;this.ver=navigator.appVersion;this.agent=navigator.userAgent;this.dom=document.getElementById?1:0;this.opera=window.opera?1:0;this.ie5=(this.ver.indexOf("MSIE 5")>-1&&this.dom&&!this.opera)?1:0;this.ie6=(this.ver.indexOf("MSIE 6")>-1&&this.dom&&!this.opera)?1:0;this.ie4=(document.all&&!this.dom&&!this.opera)?1:0;this.ie=this.ie4||this.ie5||this.ie6;this.mac=this.agent.indexOf("Mac")>-1;this.ns6=(this.dom&&parseInt(this.ver)>=5)?1:0;this.ie3=(this.ver.indexOf("MSIE")&&(is_major<4));this.hotjava=(this.agent.toLowerCase().indexOf('hotjava')!=-1)?1:0;this.ns4=(document.layers&&!this.dom&&!this.hotjava)?1:0;this.bw=(this.ie6||this.ie5||this.ie4||this.ns4||this.ns6||this.opera);this.ver3=(this.hotjava||this.ie3);this.opera7=((this.agent.toLowerCase().indexOf('opera 7')>-1) || (this.agent.toLowerCase().indexOf('opera/7')>-1));this.operaOld=this.opera&&!this.opera7;return this;};
function pldImg(arg){for(var i in arg){var im=new Image();im.src=arg[i];}}
function CTreeFormat(fmt, tree){
	this.init=function(fmt, tree){this.left=fmt[0];this.top=fmt[1];this.showB=fmt[2];this.clB=fmt[3][0];this.exB=fmt[3][1];this.iE=fmt[3][2];this.Bw=fmt[4][0];this.Bh=fmt[4][1];this.Ew=fmt[4][2];this.showF=fmt[5];this.clF=fmt[6][0];this.exF=fmt[6][1];this.iF=fmt[6][2];this.Fw=fmt[7][0];this.Fh=fmt[7][1];this.ident=fmt[8];this.back=new CTreeBack(this.left, this.top, fmt[9], 'cls'+tree.name+'_back');this.nst=fmt[10];this.nstl=fmt[11];this.so=fmt[12];this.pg=fmt[13][0];this.sp=fmt[13][1];
		if (this.showB)pldImg([this.clB,this.exB,this.iE]);
		if (this.showF)pldImg([this.exF,this.clF,this.iF]);
	}
	this.nstyle=function(lvl){return(und(this.nstl[lvl])) ? this.nst : this.nstl[lvl];}
	this.idn=function(lvl){var r=(und(this.ident[lvl])) ? this.ident[0]*lvl : this.ident[lvl];return r;}
	this.init(fmt, tree);
}
function COOLjsTree(name, nodes, format){
	this.REGISTERED=false;this.bw=new bw_check();this.ns4=this.bw.ns4;
	this.name=name;this.fmt=new CTreeFormat(format, this);if (und(window.NTrees)) window.NTrees=[];window.NTrees[this.name]=this;this.Nodes=[];
	this.rootNode=new CTreeNode(null, "", "", "", null);
	this.rootNode.treeView=this;this.selectedNode=null;this.maxWidth=0;this.maxHeight=0;this.ondraw=null;	
	this.nbn=function(nm){for (var i=0;i<this.Nodes.length;i++) if (this.Nodes[i].text == nm) return this.Nodes[i];return null;};this.nodeByName=this.nbn;
	this.nodeByID=function(id){for (var i=0;i<this.Nodes.length;i++) if (this.Nodes[i].nodeID==id) return this.Nodes[i];return null;}
	this.nodeByURL=function(u){for(var i=0;i<this.Nodes.length;i++) if (this.Nodes[i].url==u) return this.Nodes[i];return null;};
	this.moveTo=function(x,y){this.fmt.back.top=y;this.fmt.back.left=y;this.fmt.back.moveTo(x,y);this.fmt.top=y;this.fmt.left=x;}
	this.addNode=function(node){
		var parentNode=node.parentNode;
		this.Nodes[this.Nodes.length]=node;
		node.index=this.Nodes.length-1;
		if (parentNode == null) 
			this.rootNode.children[this.rootNode.children.length]=node;
		else 
			parentNode.children[parentNode.children.length]=node;
		return node;
	}
	this.rebuildTree=function(){
		for (var i=0;i < this.Nodes.length;i++) document.write(this.Nodes[i].init());
        for (var i=0;i < this.Nodes.length;i++){
			var node=this.Nodes[i];
			node.el=this.ns4?document.layers[node.id()+"d"]:this.bw.dom?document.getElementById(node.id()+"d"):document.all[node.id()+"d"];		
		}
	}
	this.getImgEl=function(node){
		if (this.ns4) {
			if (this.fmt.showF&&!node.nf)node.nf=node.el.document.images[node.id()+"nf"];
			if (this.fmt.showB&&!node.nb)node.nb=node.el.document.images[node.id()+"nb"];
		} else {
			if (this.fmt.showB&&!node.nb)node.nb=this.bw.dom? document.getElementById(node.id()+"nb"):document.all[node.id()+"nb"];
			if (this.fmt.showF&&!node.nf)node.nf=this.bw.dom? document.getElementById(node.id()+"nf"):document.all[node.id()+"nf"];
		}
	}
	this.draw=function(){
		this.currTop=this.fmt.top;
		this.maxHeight =0;this.maxWidth=0;
		for (var i=0;i < this.rootNode.children.length;i++) this.rootNode.children[i].draw(true);
		this.fmt.back.resize(this.maxWidth-this.fmt.left, this.maxHeight-this.fmt.top);
		if (this.ondraw!=null) this.ondraw();
	}
	this.updateImages=function(node, ff){
		this.getImgEl(node);
		var b = this.fmt[node.expanded? "exB" : "clB"];
		var f = this.fmt[node.hasChildren()?(node.expanded?"exF":"clF"):"iF"]; 
		if (node.treeView.fmt.showB && node.nb && node.nb.src!=b) node.nb.src=b;
		if (node.treeView.fmt.showF && node.nf && node.nf.src!=f) node.nf.src=f;
	}
	this.expandNode=function(index){
		var node=this.Nodes[index];
		var pNode=node.parentNode ? node.parentNode : null;
		if (!und(node) && node.hasChildren()){
			node.expanded=!node.expanded;
			this.updateImages(node);
			if (!node.expanded)
				node.hideChildren();
			else 
				if (this.fmt.so)
					for (var i=0;i < this.Nodes.length;i++){
						this.Nodes[i].show(false);
						if(this.Nodes[i] != node && this.Nodes[i].parentNode == pNode) {
							this.Nodes[i].expanded=false;
							this.updateImages(this.Nodes[i]);
						}
					}
            this.draw();
		}
	}
	this.selectNode=function(index){
		var node=this.Nodes[index];
		if(!und(node)) this.selectedNode=node;
		node.draw();
	}
	this.readNodes=function(nodes){
		var ind=0;var par=null;
		function readOne(arr , tree){
			if (und(arr)) return;
			var i=0;var nodeID=0;
			if (arr[0]&&arr[0].id) {nodeID=arr[0].id;i++};
			var node=tree.addNode(new CTreeNode(tree, par, arr[i], url=arr[i+1] == null? "": arr[i+1], arr[i+2] == null? "": arr[i+2]));
			node.nodeID=nodeID;
			while (!und(arr[i+3])){
				par=node;
				readOne(arr[i+3], tree);
				i++;
			}
		}
		if (und(nodes) || und(nodes[0]) || und(nodes[0][0])) return;
		for (var i=0;i<nodes.length;i++){
			par=null;readOne(nodes[i], this);
		}
	}
	this.collapseAll=function(rd){
		for (var i=0;i < this.Nodes.length;i++){
			if (this.Nodes[i].parentNode != this.rootNode) this.Nodes[i].show(false);
			this.Nodes[i].expanded=false;
			this.updateImages(this.Nodes[i]);
		}
		if (rd) this.draw();
	}
	this.expandAll=function(rd){
		for (var i=0;i < this.Nodes.length;i++){
			this.Nodes[i].expanded=true;
			this.updateImages(this.Nodes[i]);
		}
		if (rd) this.draw();
	}
	this.init=function(){
		this.readNodes(nodes);
		this.rebuildTree();
		this.draw();
	}
	this.init();
	return this;
}

function CTreeNode(treeView, parentNode , text, url, target){
	this.index=-1;this.treeView=treeView;this.parentNode=parentNode;
	this.text=text;this.url=url;this.target=target;this.expanded=false;
	this.children=[];
	this.level=function(){
		var node=this;var i=0;
		while (node.parentNode != null){i++;node=node.parentNode;}
		return i;
	}
	this.hasChildren=function(){return this.children.length > 0;}
	this.init=function(){
		var bw = this.treeView.bw;
		return this.treeView.ns4?
			'<layer id="'+this.id()+'d" z-index="'+this.index+10+'" visibility="hidden">'+this.getContent()+'</layer>'
			:'<div id="'+this.id()+'d" style="position:absolute;visibility:hidden;'+(bw.opera&&(bw.nver!=6)||bw.ie4?'width:1px':'')+';z-index:'+this.index+10+';">'+this.getContent()+'</div>';
	}
	this.getH=function(){
		var bw = this.treeView.bw;
		this.css=bw.dom||bw.ie4?this.el.style:this.el;this.doc=bw.dom||bw.ie4?document:this.css.document;
		this.h=this.el.offsetHeight||this.css.clip.height||this.doc.height||this.css.pixelHeight;
		return this.h;
	}
	
	this.getH1=function(){if(!this.h)this.h=this.treeView.ns4 ? this.el.clip.height:this.treeView.bw.dom&&!this.treeView.bw.operaOld? this.el.firstChild.offsetHeight:this.el.offsetHeight;return this.h}
    this.getW=function(){if(!this.w)this.w=this.treeView.ns4 ? this.el.clip.width:this.treeView.bw.dom&&!this.treeView.bw.operaOld? this.el.firstChild.offsetWidth:this.el.offsetWidth;return this.w}
	this.id=function(){return 'nt'+this.treeView.name+this.index;}
	this.getContent=function(){
		function itemSquare(node){
                var img=node.treeView.fmt[node.hasChildren()?(node.expanded?"exF":"clF"):"iF"]; 
				var w=node.treeView.fmt.Fw;var h=node.treeView.fmt.Fh;
				var img = "<img id=\""+node.id()+"nf\" name=\""+node.id()+"nf\" src=\"" + img + "\" width="+w+" height="+h+" border=0>"
				img = node.hasChildren()?'<a href="javascript:CTExpand(\''+node.treeView.name+'\','+node.index+')">'+img+'</a>':img;
				return "<td valign=\"middle\" width=\""+w+"\">"+img+"</td>\n";
		}
		function buttonSquare(node){
            var img=node.treeView.fmt[node.expanded? "exB" : "clB"]; 
			var w=node.treeView.fmt.Bw;var h=node.treeView.fmt.Bh;
			return '<td valign=\"middle\" width="'+w+'"><a href="javascript:CTExpand(\''+node.treeView.name+'\','+node.index+')"><img name=\''+node.id()+'nb\' id=\''+node.id()+'nb\' src="' + img + '" width="'+w+'" height="'+h+'" border=0></a></td>\n';
		}
		function blankSquare(node, ww){
			var img=node.treeView.fmt.iE;
			return "<td width=\""+ww+"\"><img src=\"" + img + "\" width="+ww+" height=1 border=0></td>\n"
		}
		var s='';
		var ll=this.level();
		s += '<table cellpadding='+this.treeView.fmt.pg+' cellspacing='+this.treeView.fmt.sp+' border=0 class="cls'+this.treeView.name+'_back'+ll+'"><tr>';
		var idn=this.treeView.fmt.idn(ll);
		if (idn > 0) s += blankSquare(this, idn);
		if(this.treeView.fmt.showB) s += this.hasChildren() ? buttonSquare(this) : blankSquare(this, this.treeView.fmt.Ew);
		if(this.treeView.fmt.showF) s += itemSquare(this);
		var n = this.treeView.name;
		if(this.url == "")	
			s += this.hasChildren()? '<td nowrap=\"1\"><a class="'+this.treeView.fmt.nstyle(ll)+'" href="javascript:CTExpand(\''+n+'\','+this.index+')">'+this.text+'</a></td></tr></table>' : '<td nowrap=\"1\"><a class="'+this.treeView.fmt.nstyle(ll)+'" href="javascript:void(0)">'+this.text+'</a></td></tr></table>';
		else 
			s += '<td nowrap=\"1\"><a class="'+this.treeView.fmt.nstyle(ll)+'" href="'+this.url+'" target="'+this.target+'" onclick="CTExpand(\''+n+'\','+this.index+')">'+this.text+'</a></td></tr></table>';
        return s;
	}
	this.moveTo=function(x, y){if (this.treeView.ns4)this.el.moveTo(x,y);else{this.el.style.left=x;this.el.style.top=y;}}
	this.show=function(sh){if (this.visible == sh)return;this.visible=sh;var vis=this.treeView.ns4 ? (sh ? 'show': 'hide') : (sh ? 'visible': 'hidden');if (this.treeView.ns4)this.el.visibility=vis;else this.el.style.visibility=vis;}
	this.hideChildren=function(){this.show(false);for (var i=0;i < this.children.length;i++)this.children[i].hideChildren();}
	this.draw=function(){var ll=this.treeView.fmt.left;this.moveTo(this.treeView.fmt.left, this.treeView.currTop);this.show(true);var w = this.getW();if (ll+ w> this.treeView.maxWidth)this.treeView.maxWidth=ll+w;this.treeView.currTop += this.getH();if (this.treeView.currTop > this.treeView.maxHeight)this.treeView.maxHeight=this.treeView.currTop;if (this.expanded && this.hasChildren())for (var i=0;i < this.children.length;i++)this.children[i].draw();}
}
function CTreeBack(aleft, atop, color, name){
	this.bw=new bw_check();this.ns4=this.bw.ns4;this.left=aleft;this.top=atop;this.name=name;this.color=color;this.t=unescape('%43%4f%4f%4C%6A%73%54%72%65%65');
	this.moveTo=function(x, y){if (this.ns4)this.el.moveTo(x,y);else{this.el.style.left=x;this.el.style.top=y;this.el2.style.left=x;}};
	this.resize=function(w,h){if (this.ns4){this.el.resizeTo(w,h);}else{this.el.style.width=w;this.el.style.height=h;if (this.r) this.el2.style.top=h+this.top-5;}};
	this.init=function(){if (this.r)if (!this.ns4) {var bgc=this.color == ""? "" : " background-color:"+this.color+";";document.write('<div id="'+this.name+'c" style="'+bgc+'position:absolute;z-index:0;top:'+this.top+'px;left:'+this.left+'px;">'+'&nbsp;<span style="font-size:7px;color:#d0d0d0;">'+this.t+'</span>'+'</div>');this.el2=document.all? document.all[this.name+'c'] : document.getElementById(this.name+'c');}if(this.ns4){var bgc=this.color == ""? "" : ' bgcolor="'+this.color+'" ';document.write('<layer '+bgc+' top="'+this.top+'" left="'+this.left+'" id="'+this.name+'" z-index="0">'+ '</layer>');this.el=document.layers[this.name];} else {var bgc=this.color == ""? "" : " background-color:"+this.color+";";document.write('<div id="'+this.name+'" style="'+bgc+'position:absolute;z-index:0;top:'+this.top+'px;left:'+this.left+'px">'+ '</div>');this.el=document.all? document.all[this.name] : document.getElementById(this.name);}};this.r=1;
	this.init();
}
function und(val){return typeof(val) == 'undefined';}
window.oldCTOnLoad=window.onload;
function CTOnLoad(){
	var bw=new bw_check();
	if (bw.operaOld)window.operaResizeTimer=setTimeout('resizeHandler()',1000);
	if (typeof(window.oldCTOnLoad)=='function') window.oldCTOnLoad();
	if (bw.ns4) window.onresize=resizeHandler;
}
window.onload=CTOnLoad;
function resizeHandler() {
	if (window.reloading) return;
	if (!window.origWidth){
		window.origWidth=window.innerWidth;
		window.origHeight=window.innerHeight;
	}
	var reload=window.innerWidth != window.origWidth || window.innerHeight != window.origHeight;
	window.origWidth=window.innerWidth;window.origHeight=window.innerHeight;
	if (window.operaResizeTimer)clearTimeout(window.operaResizeTimer);
	if (reload) {window.reloading=1;document.location.reload();return};
	if (new bw_check().operaOld){window.operaResizeTimer=setTimeout('resizeHandler()',500)};
}
function CTExpand(name, index){window.NTrees[name].expandNode(index)}
