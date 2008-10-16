package jrds.probe;

import java.net.MalformedURLException;
import java.net.URL;

import jrds.Probe;

import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.BeforeClass;
import org.junit.Test;

public class JsonProbe {
	static final Appender app = new WriterAppender() {
		public void doAppend(LoggingEvent event) {
			System.out.println(event.getLevel() + ": " + event.getMessage());
		}
	};

	@BeforeClass static public void configure() {
		System.getProperties().setProperty("java.awt.headless","true");
		jrds.JrdsLoggerConfiguration.initLog4J();
		app.setName(jrds.JrdsLoggerConfiguration.APPENDER);
		jrds.JrdsLoggerConfiguration.putAppender(app);
		Logger.getRootLogger().setLevel(Level.INFO);
		Logger.getLogger(jrds.JrdsLoggerConfiguration.APPENDER).setLevel(Level.INFO);
		Logger.getLogger("org.apache.commons.digester.Digester").setLevel(Level.INFO);
		Logger.getLogger("jrds.probe.json").setLevel(Level.TRACE);
	}

	@Test public void detect() throws MalformedURLException {
		//	Probe p  = new jrds.probe.json(new URL("http://ng252.prod.exalead.com:30199/ken/ListAllProcesses?output=json")) {
		Probe p  = new jrds.probe.json(new URL("http://ng10:10004/box/GetStatus?output=json")) {

		};

		p.getNewSampleValues();
	}
}
