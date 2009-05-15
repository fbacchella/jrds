package jrds;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import jrds.factories.xml.EntityResolver;

import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.spi.LoggingEvent;
import org.w3c.dom.Document;

public class JrdsTester {
	public static DocumentBuilder dbuilder = null;
	public static XPath xpather = null;

	static final Appender app = new WriterAppender() {
		public void doAppend(LoggingEvent event) {
			System.out.println(event.getLevel() + ": " + event.getMessage());
		}
	};

	 static public void configure() {
			System.getProperties().setProperty("java.awt.headless","true");
			jrds.JrdsLoggerConfiguration.initLog4J();
			app.setName(jrds.JrdsLoggerConfiguration.APPENDER);
			app.setLayout(new PatternLayout("[%d] %5p %c : %m%n"));
			jrds.JrdsLoggerConfiguration.putAppender(app);
			Logger.getRootLogger().setLevel(Level.INFO);
			Logger.getLogger(jrds.JrdsLoggerConfiguration.APPENDER).setLevel(Level.INFO);
			Logger.getLogger("org.apache.commons.digester.Digester").setLevel(Level.INFO);
			Logger.getLogger("org.apache.cactus.internal.configuration.ConfigurationInitializer").setLevel(Level.INFO);
	 }
	 
	 static public void prepareXml() throws ParserConfigurationException {
			DocumentBuilderFactory instance = DocumentBuilderFactory.newInstance();
			instance.setIgnoringComments(true);
			instance.setValidating(false);
			dbuilder = instance.newDocumentBuilder();
			dbuilder.setEntityResolver(new EntityResolver());
			xpather = XPathFactory.newInstance().newXPath();
	 }
	 
	 static public Document parseRessource(String name) throws Exception {
			InputStream is = JrdsTester.class.getResourceAsStream("/ressources/" + name);
			return parseRessource(is);
	 }
	 
	 static public Document parseRessource(InputStream is) throws Exception {
			return JrdsTester.dbuilder.parse(is);
	 }

}
