<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns="http://www.w3.org/1999/xhtml" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="html" indent="yes" doctype-public="-//W3C//DTD HTML 4.01 Transitional//EN"
				doctype-system = "http://www.w3.org/TR/html4/loose.dtd"
				omit-xml-declaration = "yes"
				standalone = "yes"
				encoding = "UTF-8" />
				
	<xsl:template match="probe">
<html class="fillspace">
<head>
	<title><xsl:value-of select="@name" /> on <xsl:value-of select="@host" /></title>
	<link href="dojo/resources/dojo.css" rel="stylesheet" type="text/css" />
	<link href="dijit/themes/nihilo/nihilo.css" rel="stylesheet" type="text/css" />
	<link href="lib/jrds.css" rel="stylesheet" type="text/css" />
	<script type="text/javascript" src="dojo/dojo.js" djConfig="parseOnLoad:true, isDebug:true, locale:'en-us'">/* */ </script>
	<script type="text/javascript" src="dojo/dojo-jrds">/* */ </script>
</head>
<body class="nihilo fillspace">
<div  dojoType="dijit.layout.BorderContainer" gutters="true"  class="fillspace">
<div dojoType="dijit.layout.ContentPane" region="center">
<div dojoType="dijit.TitlePane" title="Probe instance name">
<p><xsl:value-of select="@host" />/<xsl:value-of select="@name" /></p>
</div>
		<xsl:apply-templates />
</div>
</div>
</body>
</html>
	</xsl:template>
	
	<xsl:template match="probeName">
		<div  dojoType="dijit.TitlePane" title="Probe name" ><xsl:apply-templates/></div>
	</xsl:template>
	
	<xsl:template match="index">
		<div  dojoType="dijit.TitlePane" title="Index" ><xsl:apply-templates/></div>
	</xsl:template>
	
	<xsl:template match="url">
		<div  dojoType="dijit.TitlePane" title="Index" ><xsl:apply-templates/></div>
	</xsl:template>
	
	<xsl:template match="ds">
<div  dojoType="dijit.TitlePane" title="Data stores" >
<ul>
		<xsl:apply-templates>
		</xsl:apply-templates>
</ul>
</div>
	</xsl:template>
	
	<xsl:template match="name">
	<li><a href="popup.html?pid={@pid}&amp;dsName={@dsName}"  onclick="popup('popup.html?pid={@pid}&amp;dsName={@dsName}',null); return false"><xsl:apply-templates/></a></li>
	</xsl:template>
	
	<xsl:template match="graphs">
<div  dojoType="dijit.TitlePane" title="Graphs" >
<ul>
		<xsl:apply-templates>
		</xsl:apply-templates>
</ul>
</div>
	</xsl:template>

	<xsl:template match="graphname">
	<li><a href="popup.html?id={@id}"  onclick="popup('popup.html?id={@id}',null); return false"><xsl:apply-templates/></a></li>
	</xsl:template>

</xsl:stylesheet>
