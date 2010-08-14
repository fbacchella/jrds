<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns="http://www.w3.org/1999/xhtml" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" indent="yes" doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN"
				doctype-system = "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"
				encoding = "UTF-8" />
				
	<xsl:template match="probe">
<html>
<head>
	<title><xsl:value-of select="@name" /> on <xsl:value-of select="@host" /></title>
	<link href="dojo/resources/dojo.css" rel="stylesheet" type="text/css" />
	<link href="dijit/themes/nihilo/nihilo.css" rel="stylesheet" type="text/css" />
	<script type="text/javascript" src="dojo/dojo.js" djConfig="parseOnLoad:true, locale:'en-us'"></script>
	<script type="text/javascript" src="dojo/dojo-jrds.js"></script>
	<script type="text/javascript" src="lib/jrds.js"></script>
</head>
<body class="nihilo">
<h1>Probe instance name</h1>
<p><xsl:value-of select="@host" />/<xsl:value-of select="@name" /></p>
		<xsl:apply-templates />
</body>
</html>
	</xsl:template>
	
	<xsl:template match="probeName">
		<h1>Probe name</h1>
		<p><xsl:apply-templates/></p>
	</xsl:template>
	
	<xsl:template match="index">
		<h1>Index</h1>
		<p><xsl:apply-templates/></p>
	</xsl:template>
	
	<xsl:template match="url">
		<h1>Index</h1>
		<p><xsl:apply-templates/></p>
	</xsl:template>
	
	<xsl:template match="ds">
<h1>Data stores</h1>
<ul>
		<xsl:apply-templates>
		</xsl:apply-templates>
</ul>
	</xsl:template>
	
	<xsl:template match="name">
	<li><a href="popup.html?pid={@pid}&amp;dsName={@dsName}"  onclick="popup('popup.html?pid={@pid}&amp;dsName={@dsName}',null); return false"><xsl:apply-templates/></a></li>
	</xsl:template>
	
	<xsl:template match="graphs">
<h1>Graphs</h1>
<ul>
		<xsl:apply-templates>
		</xsl:apply-templates>
</ul>
	</xsl:template>

	<xsl:template match="graphname">
	<li><a href="popup.html?id={@id}"  onclick="popup('popup.html?id={@id}',null); return false"><xsl:apply-templates/></a></li>
	</xsl:template>

</xsl:stylesheet>
