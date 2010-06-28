package jrds;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import jrds.factories.xml.EntityResolver;

import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.spi.LoggingEvent;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

final public class Tools {
	public static DocumentBuilder dbuilder = null;
	public static XPath xpather = null;

	static final Appender app = new WriterAppender() {
		public void doAppend(LoggingEvent event) {
			System.out.println(event.getLevel() + ": " + event.getMessage());
		}
	};

	static public void configure() throws IOException {
		Locale.setDefault(new Locale("POSIX"));
		System.getProperties().setProperty("java.awt.headless","true");
		LogManager.getLoggerRepository().resetConfiguration();
		jrds.JrdsLoggerConfiguration.initLog4J();
		app.setName(jrds.JrdsLoggerConfiguration.APPENDER);
		app.setLayout(new PatternLayout("[%d] %5p %c : %m%n"));
		jrds.JrdsLoggerConfiguration.putAppender(app);
	}

	static public void prepareXml() throws ParserConfigurationException {
		DocumentBuilderFactory instance = DocumentBuilderFactory.newInstance();
		instance.setIgnoringComments(true);
		instance.setValidating(true);
		instance.setExpandEntityReferences(false);
		dbuilder = instance.newDocumentBuilder();
		dbuilder.setEntityResolver(new EntityResolver());
		xpather = XPathFactory.newInstance().newXPath();
	}

	static public void prepareXml(boolean validating) throws ParserConfigurationException {
		DocumentBuilderFactory instance = DocumentBuilderFactory.newInstance();
		instance.setIgnoringComments(true);
		instance.setValidating(validating);
		instance.setExpandEntityReferences(false);
		dbuilder = instance.newDocumentBuilder();
		dbuilder.setEntityResolver(new EntityResolver());
		xpather = XPathFactory.newInstance().newXPath();
	}

	static public Document parseRessource(String name) throws Exception {
		InputStream is = Tools.class.getResourceAsStream("/ressources/" + name);
		return parseRessource(is);
	}

	static public Document parseRessource(InputStream is) throws Exception {
		return Tools.dbuilder.parse(is);
	}

	static public Document parseString(String s) throws Exception { 
		InputStream is = new ByteArrayInputStream(s.getBytes());
		Document d = Tools.parseRessource(is);
		return d;
	}

	static public void setLevel(String[] allLoggers, Level level) {
		Appender app = Logger.getLogger("jrds").getAppender(JrdsLoggerConfiguration.APPENDER);
		for(String loggerName: allLoggers) {
			Logger l = Logger.getLogger(loggerName);
			l.setLevel(level);
			if(l.getAppender(JrdsLoggerConfiguration.APPENDER) != null) {
				l.addAppender(app);
			}
		}
	}

	static public Element appendElement(Node n, String name, Map<String, String> attributes) {
		Document d = n.getOwnerDocument();
		Element e = d.createElement(name);
		if(attributes != null)
			for(Map.Entry<String, String> a: attributes.entrySet()) {
				e.setAttribute(a.getKey(), a.getValue());
			}
		n.appendChild(e);
		return e;
	}
	
	static public Node appendString(Node n, String xmlString) throws Exception {
		Document d = parseString(xmlString);
		
		Element docElem = d.getDocumentElement();
		Node newNode = n.getOwnerDocument().importNode(docElem, true);
		n.appendChild(newNode);

		return newNode;
	}
}
