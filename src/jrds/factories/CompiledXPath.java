package jrds.factories;

import java.util.HashMap;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;

public class CompiledXPath {
	static final private Logger logger = Logger.getLogger(CompiledXPath.class);

	private static final XPath xpather = XPathFactory.newInstance().newXPath();
	static final Map<String, XPathExpression> xpc = new HashMap<String, XPathExpression>();
	static {
		String[] xpaths = { "/filter/name", "/filter/path", "/filter/tag", "/macrodef/tag", "/macrodef/probe", 
						    "/host/@dnsName", "/host/tag", "/host/@name", "/host/snmp", "/host/probe", "/host/macro/@name", "/host/probe | /host/rrd",
						    "arg", "list", "snmp",
						    "/graphdesc/name", "/graphdesc/graphName", "/graphdesc/verticalLabel", "/graphdesc/graphTitle", "/graphdesc/upperLimit", "/graphdesc/lowerLimit", "/graphdesc/unit/base", "/graphdesc/unit/binary", "/graphdesc/unit/SI",
						    "/graphdesc/add",
						    "/graphdesc/hosttree/*", "/graphdesc/viewtree/*",
						    "/probedesc/probeName", "/probedesc/name", "/probedesc/probeClass", "/probedesc/uniq", "/probedesc/uptimefactor",
						    "/probedesc/graphs/name", "/probedesc/specific", "/probedesc/snmpRequester", "/probedesc/index",
						    "/probedesc/defaultargs/arg", "/probedesc/ds",
						    "/filter/name", "/filter/@name",
						    "/macrodef/@name", "/macrodef/probe | /macrodef/rrd",
						    "/sum/@name", "/sum/element/@name",
						    "*",
		};
		for(String xpath: xpaths) {
			try {
				XPathExpression e = xpather.compile(xpath);
				xpc.put(xpath, e);
			} catch (XPathExpressionException e1) {
				logger.fatal("Invalid xpath:" + xpath + ": " + e1);
			}
		}
	}
	
	public static XPathExpression get(String xpath)	{
		XPathExpression e =  xpc.get(xpath);
		if(e == null) {
			logger.warn("Uncompiled xpath: " + xpath);
			try {
				e = xpather.compile(xpath);
				xpc.put(xpath, e);
			} catch (XPathExpressionException e1) {
				logger.error("invalid xpath:" + xpath);
				throw new RuntimeException("Invalid xpath " + xpath, e1);
			}
		}
		return e;
	}

}
