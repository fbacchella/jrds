package jrds.test;

import jrds.Graph;
import jrds.GraphNode;
import jrds.HostsList;
import jrds.Period;
import jrds.Probe;
import jrds.PropertiesManager;

import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class GraphTest {
	static final Appender app = new WriterAppender() {
		public void doAppend(LoggingEvent event) {
			System.out.println(event.getLevel() + ": " + event.getMessage());
		}
	};
	static final HostsList hl = HostsList.getRootGroup();
	@Test public void compare() {
		Probe p = GetMoke.getProbe();
		GraphNode gn = new GraphNode(p, GetMoke.getGraphDesc());
		Period pr = new Period();
		Graph g1 = new Graph(gn);
		g1.setPeriod(pr);
		Graph g2 = new Graph(gn);
		g2.setPeriod(pr);
		Assert.assertEquals(g1.hashCode(), g2.hashCode());
		Assert.assertEquals(g1, g2);
	}

	@BeforeClass static public void configure() {
		System.getProperties().setProperty("java.awt.headless","true");
		jrds.JrdsLoggerConfiguration.initLog4J();
		app.setName(jrds.JrdsLoggerConfiguration.APPENDER);
		jrds.JrdsLoggerConfiguration.putAppender(app);
		Logger.getRootLogger().setLevel(Level.INFO);
		Logger.getLogger(jrds.JrdsLoggerConfiguration.APPENDER).setLevel(Level.INFO);
		Logger.getLogger("org.apache.commons.digester.Digester").setLevel(Level.INFO);
		Logger.getLogger("jrds.Period").setLevel(Level.TRACE);
		PropertiesManager pm = new PropertiesManager();
		hl.configure(pm);
	}

}
