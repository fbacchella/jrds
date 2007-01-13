<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" indent="yes" doctype-public="//W3C//DTD XHTML 1.0 Strict//EN"
				doctype-system = "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"
				encoding = "UTF-8" />
				
	<xsl:template match="probe">
<html>
<head>
	<title><xsl:value-of select="@name" /> on <xsl:value-of select="@host" /></title>
	<link rel="stylesheet" type="text/css" href="/lib/jrds.css" />
	<script type="text/javascript" src="lib/jrds.js" ></script>
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
		<xsl:apply-templates/>
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
	<li><a href="popup.jsp?id={@id}"><xsl:apply-templates/></a></li>
	</xsl:template>
</xsl:stylesheet>
