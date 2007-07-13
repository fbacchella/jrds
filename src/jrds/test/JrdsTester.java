package jrds.test;

import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.spi.LoggingEvent;

public class JrdsTester {
	static final Appender app = new WriterAppender() {
		public void doAppend(LoggingEvent event) {
			System.out.println(event.getLevel() + ": " + event.getMessage());
			//Assert.fail(event.getLevel() + " " + event.getLoggerName() + " " + event.getMessage());
		}
	};

	 static public void configure() {
			System.getProperties().setProperty("java.awt.headless","true");
			jrds.JrdsLoggerConfiguration.initLog4J();
			app.setName(jrds.JrdsLoggerConfiguration.APPENDER);
			jrds.JrdsLoggerConfiguration.putAppender(app);
			Logger.getRootLogger().setLevel(Level.INFO);
			Logger.getLogger(jrds.JrdsLoggerConfiguration.APPENDER).setLevel(Level.INFO);
			Logger.getLogger("org.apache.commons.digester.Digester").setLevel(Level.INFO);
	 }
}
