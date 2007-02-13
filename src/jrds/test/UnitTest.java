package jrds.test;

import java.io.IOException;

import jrds.ArgFactory;
import jrds.DescFactory;

import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class UnitTest {
	static final Appender app = new WriterAppender() {
		public void doAppend(LoggingEvent event) {
			System.out.println(event.getLevel() + ": " + event.getMessage());
			//Assert.fail(event.getLevel() + " " + event.getLoggerName() + " " + event.getMessage());
		}
	};

	@Test public void loadDescriptions() throws IOException {
		ArgFactory af= new ArgFactory();
		DescFactory df = new DescFactory(af);
			df.importDescUrl(DescFactory.class.getResource("/probe"));
			Assert.assertTrue(df.getGraphDescMap().size() == 0);
			Assert.assertTrue(df.getProbesDescMap().size() > 0);
			df.importDescUrl(DescFactory.class.getResource("/graph"));
			Assert.assertTrue(df.getGraphDescMap().size() > 0);
	}
	@BeforeClass static public void configure() {
		System.getProperties().setProperty("java.awt.headless","true");
		jrds.JrdsLoggerConfiguration.initLog4J();
		app.setName(jrds.JrdsLoggerConfiguration.APPENDER);
		jrds.JrdsLoggerConfiguration.putAppender(app);
		Logger.getRootLogger().setLevel(Level.INFO);
		Logger.getLogger(jrds.JrdsLoggerConfiguration.APPENDER).setLevel(Level.INFO);
		Logger.getLogger("org.apache.commons.digester.Digester").setLevel(Level.INFO);
	}
	    
	@After public void tearDown() {
	}
}
