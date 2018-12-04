package jrds;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Hierarchy;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.config.PropertyPrinter;
import org.apache.log4j.config.PropertySetter;
import org.apache.log4j.net.SMTPAppender;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.RootLogger;
import org.apache.log4j.spi.TriggeringEventEvaluator;
import org.apache.log4j.xml.DOMConfigurator;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Log4jPlay {
    static final private Logger logger = Logger.getLogger(Log4jPlay.class);

    @BeforeClass
    static public void configure() throws IOException, ParserConfigurationException {
        Tools.configure();
        Tools.setLevel(logger, Level.TRACE);
        Tools.prepareXml();
    }

    @Test
    public void setter() throws SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
        TriggeringEventEvaluator evaluator = new TriggeringEventEvaluator() {
            public boolean isTriggeringEvent(LoggingEvent arg0) {
                return true;
            }
        };
        Constructor<? extends Appender> c = SMTPAppender.class.getConstructor(TriggeringEventEvaluator.class);
        Appender app = c.newInstance(evaluator);
        app.setLayout(new PatternLayout());
        PropertySetter setter = new PropertySetter(app);
        setter.setProperty("from", "root@localhost");
        setter.setProperty("subject", "Threshold reached");
        setter.setProperty("SMTPHost", "localhost");
        setter.setProperty("To", "Destination");
        // PropertyPrinter printer = new PropertyPrinter(new
        // PrintWriter(System.out));
        // printer.print(new PrintWriter(System.out));
    }

    @Test
    public void hierarchy() throws Exception {
        Hierarchy h = new Hierarchy(new RootLogger(Level.ALL));
        Document log4jdom = Tools.parseRessource("log4j.xml");
        DOMConfigurator configurator = new DOMConfigurator();
        configurator.doConfigure(log4jdom.getDocumentElement(), h);
        PropertyPrinter printer = new PropertyPrinter(new PrintWriter(System.out));
        printer.print(new PrintWriter(System.out));
    }

    @Test
    public void dumpConf() throws Exception {
        // PropertyPrinter printer = new PropertyPrinter(new
        // PrintWriter(System.out));
        // printer.print(new PrintWriter(System.out));
    }

    @Test
    public void construct() throws Exception {
        Document actionDocument = Tools.parseRessource("action.xml");
        Element e1 = actionDocument.createElementNS("http://jakarta.apache.org/log4j/", "log4j:configuration");
        e1.setAttribute("debug", "true");
        Element action = (Element) actionDocument.removeChild(actionDocument.getDocumentElement());
        actionDocument.appendChild(e1);
        e1.appendChild(action);
        String name = "jrds.Logger thresold." + action.getAttribute("name");
        action.setAttribute("name", name);
        Element loggerElem = actionDocument.createElement("logger");
        loggerElem.setAttribute("name", name);
        Element appenderRef = actionDocument.createElement("appender-ref");
        appenderRef.setAttribute("ref", name);
        e1.appendChild(loggerElem);
        loggerElem.appendChild(appenderRef);
        jrds.Util.serialize(actionDocument, System.out, null, null);
        DOMConfigurator.configure(actionDocument.getDocumentElement());
        PropertyPrinter printer = new PropertyPrinter(new PrintWriter(System.out));
        printer.print(new PrintWriter(System.out));
    }

    @Test
    public void playDom() throws Exception {
        // Document log4jdom = Tools.parseRessource("log4j.xml");
        // DOMConfigurator.configure(log4jdom.getDocumentElement());
        // logger.debug(Logger.getLogger("bidule").getAppender("bidule"));
    }

    @Test
    public void playProp() throws Exception {
        Properties prop = new Properties();
        // log4j.appender.A1=org.apache.log4j.ConsoleAppender

        prop.put("log4j.appender.bidule", "org.apache.log4j.net.SMTPAppender");
        prop.put("log4j.appender.bidule.from", "root@localhost");
        prop.put("log4j.appender.bidule.subject", "Threshold reached");
        prop.put("log4j.appender.bidule.SMTPHost", "localhost");
        prop.put("log4j.appender.bidule.To", "Destination");
        prop.put("log4j.appender.bidule.layout", "org.apache.log4j.SimpleLayout");
        prop.put("log4j.logger.bidule", "bidule");
        // PropertyConfigurator.configure(prop);
        // logger.debug(Logger.getLogger("bidule").getAppender("bidule"));
    }

    @Test
    public void multiappender() {
        Appender consoleAppender = new ConsoleAppender(new org.apache.log4j.SimpleLayout(), ConsoleAppender.SYSTEM_OUT);
        Logger l1 = Logger.getLogger("1");
        l1.addAppender(consoleAppender);
        Logger l2 = Logger.getLogger("1.2");
        l2.addAppender(consoleAppender);
        l2.setAdditivity(false);

        l2.fatal("One log");
    }
}
