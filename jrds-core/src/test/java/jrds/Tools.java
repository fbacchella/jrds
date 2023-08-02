package jrds;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import jrds.PropertiesManager.TimerInfo;
import jrds.factories.xml.EntityResolver;
import jrds.factories.xml.JrdsDocument;
import jrds.starter.Timer;

final public class Tools {
    public static DocumentBuilder dbuilder = null;
    public static XPath xpather = null;

    static {
        Locale.setDefault(new Locale("POSIX"));
        System.getProperties().setProperty("java.awt.headless", "true");
        Log4JRule.configure();
    }

    static public void configure() {
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
        try (InputStream is = Tools.class.getClassLoader().getResourceAsStream(name)) {
            return parseRessource(is);
        }
    }

    static public JrdsDocument parseRessource(InputStream is) throws Exception {
        return new JrdsDocument(Tools.dbuilder.parse(is));
    }

    static public JrdsDocument parseString(String s) throws Exception {
        try (InputStream is = new ByteArrayInputStream(s.getBytes())) {
            return Tools.parseRessource(is);
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

    static public URI pathToUrl(String pathname) {
        File path = new File(pathname);
        return path.toURI();
    }

    static private final PropertiesManager finishPm(PropertiesManager pm, String... props) {
        pm.setProperty("strictparsing", "true");

        pm.setProperty("tabs", "hoststab");
        for(String prop: props) {
            int pos = prop.indexOf('=');
            if(pos == 0 || pos == (prop.length() - 1))
                continue;
            String key = prop.substring(0, pos);
            String value = prop.substring(pos + 1);
            pm.setProperty(key, value);
        }
        pm.update();
        pm.configureStores();
        pm.libspath.clear();
        return pm;
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
        PropertiesManager.TimerInfo ti = TimerInfo.builder().numCollectors(1).step(300).timeout(10).build();
        Timer t = new Timer(Timer.DEFAULTNAME, ti);
        Map<String, Timer> timerMap = new HashMap<String, Timer>(1);
        timerMap.put(t.getName(), t);
        return timerMap;
    }

    static public final Timer getDefaultTimer() {
        PropertiesManager.TimerInfo ti = TimerInfo.builder().numCollectors(1).step(300).timeout(10).slowCollectTime(5).build();
        return new Timer("TimerTester", ti);
    }

    static public void findDescs(PropertiesManager pm) {
        try {
            for(URL u: jrds.Util.iterate(pm.extensionClassLoader.getResources("desc"))) {
                pm.libspath.add(u.toURI());
            }
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
