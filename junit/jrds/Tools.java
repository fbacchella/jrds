package jrds;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import jrds.PropertiesManager.TimerInfo;
import jrds.factories.xml.EntityResolver;
import jrds.factories.xml.JrdsDocument;
import jrds.starter.Timer;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

final public class Tools {
    public static DocumentBuilder dbuilder = null;
    public static XPath xpather = null;

    static public void configure() throws IOException {
        Locale.setDefault(new Locale("POSIX"));
        System.getProperties().setProperty("java.awt.headless","true");
        System.setProperty("java.io.tmpdir",  "tmp");
        LogManager.resetConfiguration();
        //resetConfiguration is not enough
        @SuppressWarnings("unchecked")
        ArrayList<Logger> loggers = (ArrayList<Logger>)Collections.list(LogManager.getCurrentLoggers());
        for (Logger l: loggers) {
            l.removeAllAppenders();
            l.setLevel(Level.OFF);
        }
        JrdsLoggerConfiguration.jrdsAppender = new ConsoleAppender(new org.apache.log4j.PatternLayout(JrdsLoggerConfiguration.DEFAULTLAYOUT), ConsoleAppender.SYSTEM_OUT);
        JrdsLoggerConfiguration.jrdsAppender.setName(JrdsLoggerConfiguration.APPENDERNAME);
        JrdsLoggerConfiguration.initLog4J();
    }

    static public void prepareXml() throws ParserConfigurationException {
        prepareXml(true);
    }

    static public void prepareXml(boolean validating) throws ParserConfigurationException {
        DocumentBuilderFactory instance = DocumentBuilderFactory.newInstance();
        instance.setIgnoringComments(true);
        instance.setValidating(validating);
        instance.setExpandEntityReferences(false);
        dbuilder = instance.newDocumentBuilder();
        dbuilder.setEntityResolver(new EntityResolver());
        if(validating)
            dbuilder.setErrorHandler(new ErrorHandler() {
                public void error(SAXParseException exception) throws SAXException {
                    throw exception;
                }
                public void fatalError(SAXParseException exception) throws SAXException {
                    throw exception;
                }
                public void warning(SAXParseException exception) throws SAXException {
                    throw exception;
                }
            });
        xpather = XPathFactory.newInstance().newXPath();
    }

    static public JrdsDocument parseRessource(String name) throws Exception {
        InputStream is = Tools.class.getResourceAsStream("/ressources/" + name);
        return parseRessource(is);
    }

    static public JrdsDocument parseRessource(InputStream is) throws Exception {
        return new JrdsDocument(Tools.dbuilder.parse(is));
    }

    static public JrdsDocument parseString(String s) throws Exception { 
        InputStream is = new ByteArrayInputStream(s.getBytes());
        return Tools.parseRessource(is);
    }

    static public void setLevel(Logger logger, Level level, String... allLoggers) {
        Appender app = Logger.getLogger("jrds").getAppender(JrdsLoggerConfiguration.APPENDERNAME);
        //The system property override the code log level
        if(System.getProperty("jrds.testloglevel") != null){
            level = Level.toLevel(System.getProperty("jrds.testloglevel"));
        }
        if(logger != null) {
            logger.setLevel(level);            
        }
        for(String loggerName: allLoggers) {
            Logger l = Logger.getLogger(loggerName);
            l.setLevel(level);
            if(l.getAppender(JrdsLoggerConfiguration.APPENDERNAME) != null) {
                l.addAppender(app);
            }
        }
    }

    static public void setLevel(Level level, String... allLoggers) {
        setLevel(allLoggers, level);
    }

    static public void setLevel(String[] allLoggers, Level level) {
        setLevel(null, level, allLoggers);
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

    static public URI pathToUrl(String pathname) {
        File path = new File(pathname);
        return path.toURI();
    }

    static public List<LoggingEvent> getLockChecker(String... loggers) {
        final List<LoggingEvent> logs = new ArrayList<LoggingEvent>();
        Appender ta = new AppenderSkeleton() {
            @Override
            protected void append(LoggingEvent arg0) {
                logs.add(arg0);
            }
            public void close() {
                logs.clear();
            }
            public boolean requiresLayout() {
                return false;
            }
        };

        for(String loggername: loggers) {
            Logger logger = Logger.getLogger(loggername);
            logger.addAppender(ta);
            logger.setLevel(Level.TRACE);
        }
        return logs;
    }

    static private final PropertiesManager finishPm(PropertiesManager pm, String... props) {
        pm.setProperty("strictparsing", "true");

        pm.setProperty("tabs", "hoststab");
        for(String prop: props) {
            int pos = prop.indexOf('=');
            if(pos == 0 || pos == (prop.length() - 1) )
                continue;
            String key = prop.substring(0, pos);
            String value = prop.substring(pos +  1);
            pm.setProperty(key, value);
        }
        pm.update();
        pm.configureStores();
        pm.libspath.clear();
        pm.defaultStore.configureStore(pm, new Properties());
        pm.defaultStore.start();
        return pm;
    }

    static public final PropertiesManager makePm(String... props) throws IOException {
        PropertiesManager pm = new PropertiesManager();
        pm.setProperty("tmpdir", new File("tmp").getPath());
        pm.setProperty("configdir", new File("tmp").getPath());
        pm.setProperty("rrddir", new File("tmp").getPath());
        pm.setProperty("autocreate", "false");
        pm.setProperty("usepool", "false");

        return finishPm(pm, props);
    }

    static public final PropertiesManager makePm(TemporaryFolder testFolder, String... props) throws IOException {
        PropertiesManager pm = new PropertiesManager();
        pm.setProperty("tmpdir", testFolder.newFolder("tmp").getCanonicalPath());
        pm.setProperty("configdir", testFolder.newFolder("config").getCanonicalPath());
        pm.setProperty("rrddir", testFolder.newFolder("rrddir").getCanonicalPath());
        pm.setProperty("autocreate", "true");
        pm.setProperty("usepool", "false");

        return finishPm(pm, props);
    }

    static public final Map<String, Timer> getSimpleTimerMap() {
        PropertiesManager.TimerInfo ti = new PropertiesManager.TimerInfo();
        ti.numCollectors = 1;
        ti.step = 300;
        ti.timeout = 10;
        Timer t = new Timer(Timer.DEFAULTNAME, ti);
        Map<String, Timer> timerMap = new HashMap<String, Timer>(1);
        timerMap.put(t.getName(), t);
        return timerMap;
    }

    static public final Timer getDefaultTimer() {
        TimerInfo ti = new PropertiesManager.TimerInfo();
        ti.numCollectors = 1;
        ti.slowCollectTime = 5;
        ti.step = 300;
        ti.timeout = 10;
        return new Timer("TimerTester", ti);
    }
}
