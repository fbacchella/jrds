package jrds.probe;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import jrds.RdsHost;
import jrds.XmlProvider;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

public class HttpXml extends HttpProbe {

	static final private Logger logger = Logger.getLogger(HttpXml.class);

	/*static final private DocumentBuilder dbuilder;
	static {
		DocumentBuilder dbuilderTemp = null;
		try {
			DocumentBuilderFactory instance = DocumentBuilderFactory.newInstance();
			instance.setValidating(false);
			dbuilderTemp = instance.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			logger.fatal("No Document builder available");
		}
		finally {
			dbuilder = dbuilderTemp;
		}
	}
	static final protected XPathFactory xpathfactory = XPathFactory.newInstance();*/

	public XmlProvider xmlstarter = null;

	public HttpXml(URL url) {
		super(url);
	}

	public HttpXml(Integer port, String file) {
		super(port, file);
	}

	public HttpXml(String file) {
		super(file);
	}

	public HttpXml(Integer port) {
		super(port);
	}

	public HttpXml() {
		super();
	}

	/* (non-Javadoc)
	 * @see jrds.probe.HttpProbe#setHost(jrds.RdsHost)
	 */
	@Override
	public void setHost(RdsHost monitoredHost) {
		super.setHost(monitoredHost);
		xmlstarter = new XmlProvider(monitoredHost);
		xmlstarter = (XmlProvider) xmlstarter.register(monitoredHost);
	}

	public Map<String, Number> dom2Map(Document d, Map<String, Number> variables) {
		return variables;
	}

	protected long findUptime(Document d/*, XPath xpather*/) {
		String upTimePath = getPd().getSpecific("upTimePath");
		if(upTimePath == null) {
			logger.error("No xpath for the uptime for " + this);
			return 0;
		}
		return xmlstarter.findUptime(d, upTimePath);
		/*try {
			Node upTimeNode = (Node) xpather.evaluate(upTimePath, d, XPathConstants.NODE);
			if(upTimeNode != null) {
				uptime = Long.parseLong(upTimeNode.getTextContent());
			}
			logger.debug("uptime for " + this + " is " + uptime);
		} catch (NumberFormatException e) {
			logger.warn("Uptime not parsable for " + this + ": " + getUrl() + e);
		} catch (XPathExpressionException e) {
			logger.error("Uptime not found" + e);
		}
		return uptime;*/
	}

	/* (non-Javadoc)
	 * @see jrds.probe.HttpProbe#parseStream(java.io.InputStream)
	 */
	@Override
	protected Map<String, Number> parseStream(InputStream stream) {
		Document d = xmlstarter.getDocument(stream)/*null*/;
		/*try {
			synchronized(dbuilder) {
				d = dbuilder.parse(stream);
			}
			logger.trace("just parsed a " + d.getDocumentElement().getTagName() + " from " + getUrl() + " for " + this);
		} catch (SAXException e) {
			logger.error("Invalid XML for the probe " + this + ":" + e, e);
			return vars;
		} catch (IOException e) {
			logger.error("IO Exception getting values for " + this + ":" + e);
			return vars;
		}
		XPath xpath = XPathFactory.newInstance().newXPath();*/
		setUptime(findUptime(d/*, xpath)*/));
		Map<String, Number> vars = new HashMap<String, Number>();
		xmlstarter.fileFromXpaths(d, getPd().getCollectStrings().keySet(), vars);
		//vars = fileFromXpaths(d, getPd().getCollectStrings().keySet(), new HashMap<String, Number>(), xpath);
		vars = dom2Map(d, vars);
		return vars; 
	}

	@Override
	public String getSourceType() {
		return "HttpXml";
	}

	/*public Map<String, Number> fileFromXpaths(Document d, Set<String> xpaths, Map<String, Number> oldMap, XPath xpather) {
		for(String xpath: xpaths) {
			try {
				if(logger.isTraceEnabled())
					logger.trace("Will search the xpath \"" + xpath + "\" for " + this);
				if("".equals(xpath))
					continue;
				Node n = (Node)xpather.evaluate(xpath, d, XPathConstants.NODE);
				double value = 0;
				if(n != null) {
					logger.trace(n);
					value = Double.parseDouble(n.getTextContent());
					oldMap.put(xpath, Double.valueOf(value));
				}
			} catch (XPathExpressionException e) {
				logger.error("Invalid XPATH : " + xpath + " for " + this);
			} catch (NumberFormatException e) {
				logger.warn("value read from " + xpath + "  not parsable for " + this + ": " + getUrl() + e);
			}
		}
		return oldMap;
	}*/

}
