<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns="http://www.w3.org/1999/xhtml" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" indent="yes" doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN"
				doctype-system = "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"
				encoding = "UTF-8" />
				
	<xsl:template match="probe">
<html>
<head>
	<title><xsl:value-of select="@name" /> on <xsl:value-of select="@host" /></title>
	<link rel="stylesheet" type="text/css" href="lib/jrds.css" />
<script type="text/javascript" src="lib/dojo/dojo.js.uncompressed.js" djConfig="parseOnLoad:true, isDebug: true, locale:'en-us'"></script>
	<script type="text/javascript" src="lib/jrds.js" ><xsl:text></xsl:text></script>
</head>
<body>
<h1><xsl:value-of select="@host" />/<xsl:value-of select="@name" /></h1>
		<xsl:apply-templates>
		</xsl:apply-templates>
</body>
</html>
	</xsl:template>
	
	<xsl:template match="probeName">
		<h2>Probe name</h2>
		<p><xsl:apply-templates/></p>
	</xsl:template>
	
	<xsl:template match="index">
		<h2>Index</h2>
		<xsl:apply-templates/>
	</xsl:template>
	
	<xsl:template match="url">
		<h2><xsl:apply-templates/></h2>
	</xsl:template>
	
	<xsl:template match="ds">
<h2>data stores</h2>
<ul>
		<xsl:apply-templates>
		</xsl:apply-templates>
</ul>
	</xsl:template>
	
	<xsl:template match="name">
		<xsl:text disable-output-escaping="yes">
	<li><a href="popup.html?id={@id}"  onclick="popup('popup.html?id={@id}',{@id}); return false"><xsl:apply-templates/></a></li>
		</xsl:text>
	</xsl:template>
</xsl:stylesheet>
