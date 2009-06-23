<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="text" indent="no" omit-xml-declaration="yes"/>
	<xsl:strip-space elements="*"/>
	<xsl:template match="xport">
		<xsl:apply-templates/>
	</xsl:template>
	<xsl:template match="meta">
		<xsl:apply-templates/>
	</xsl:template>
	<xsl:template match="legend"><xsl:text>time,</xsl:text><xsl:apply-templates/>
	</xsl:template>
	<xsl:template match="entry">
		<xsl:value-of select="."/>,</xsl:template>
	<xsl:template match="data">
		<xsl:apply-templates/>
	</xsl:template>
	<xsl:template match="row">
		<xsl:apply-templates/>
	</xsl:template>
	<xsl:template match="t">
		<xsl:text>
 </xsl:text>
		<xsl:value-of select="."/>
	</xsl:template>
	<xsl:template match="v">,<xsl:value-of select="."/>
	</xsl:template>
	<xsl:template match="*"/>
</xsl:stylesheet>
