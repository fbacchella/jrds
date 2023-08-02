package jrds.starter;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.event.Level;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import jrds.CollectResolver;
import jrds.Util;

/**
 * @author bacchell A provider is used for XML to solve multi thread problems
 * 
 *         As each one is parsed by one and only one thread, it we used one
 *         provider by host, we can simply solved the concurency problem and
 *         reuse factory and parser without too many risks
 * 
 */
public class XmlProvider extends Starter {

    public static class XmlResolver implements CollectResolver<XPathExpression> {
        private static final XPathFactory factory = XPathFactory.newInstance();
        private static final ThreadLocal<XPath> localXpath = ThreadLocal.withInitial(factory::newXPath);

        @Override
        public XPathExpression resolve(String collectKey) {
            try {
                return localXpath.get().compile(collectKey);
            } catch (XPathExpressionException ex) {
                throw new IllegalArgumentException(ex.getMessage(), ex);
            }
        }
    }

    private final ThreadLocal<DocumentBuilder> localDocumentBuilder;
    private final ThreadLocal<XPath> localXpath;

    public XmlProvider() {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setIgnoringComments(true);
        documentBuilderFactory.setValidating(false);
        Stream.of("http://xml.org/sax/features/external-general-entities",
                  "http://xml.org/sax/features/external-parameter-entities")
              .forEach(s -> {
                  try {
                      documentBuilderFactory.setFeature(s, false);
                  } catch (ParserConfigurationException ex) {
                      log(Level.ERROR, ex, "Unsupported feature: {}", Util.resolveThrowableException(ex));
                  }
              });
        localDocumentBuilder = ThreadLocal.withInitial(() -> {
            try {
                return documentBuilderFactory.newDocumentBuilder();
            } catch (ParserConfigurationException ex) {
                throw new IllegalStateException("No Document builder available: " + Util.resolveThrowableException(ex), ex);
            }
        });
        XPathFactory xPathFactory = XPathFactory.newInstance();
        localXpath = ThreadLocal.withInitial(xPathFactory::newXPath);
    }

    public long findUptimeByDate(Document d, String startTimePath, String currentTimePath, DateFormat pattern) {
        XPath xpather = localXpath.get();
        try {
            Date startTime = Optional.ofNullable((Node) xpather.evaluate(startTimePath, d, XPathConstants.NODE))
                .map(Node::getTextContent)
                .map(t -> {
                    try {
                        return pattern.parse(t);
                    } catch (ParseException e) {
                        log(Level.ERROR, e, "Date not parsed with pattern " + ((SimpleDateFormat) pattern).toPattern());
                        return null;
                    }
                }).orElse(null);

            Date currentTime = Optional.ofNullable((Node) xpather.evaluate(currentTimePath, d, XPathConstants.NODE))
                .map(Node::getTextContent)
                .map(t -> {
                    try {
                        return pattern.parse(t);
                    } catch (ParseException e) {
                        log(Level.ERROR, e, "Date not parsed with pattern " + ((SimpleDateFormat) pattern).toPattern());
                        return null;
                    }
                }).orElse(null);
            if (startTime != null && currentTime != null) {
                return (currentTime.getTime() - startTime.getTime()) / 1000;
            } else {
                return 0;
            }
        } catch (XPathExpressionException e) {
            log(Level.ERROR, e, "Time not found");
            return 0;
        }
    }

    public long findUptime(Document d, String upTimePath) {
        long uptime = 0;
        if (upTimePath == null) {
            log(Level.ERROR, "No xpath for the uptime for " + this);
            return 0;
        }
        try {
            XPath xpather = localXpath.get();
            Node upTimeNode = (Node) xpather.evaluate(upTimePath, d, XPathConstants.NODE);
            if (upTimeNode != null) {
                log(Level.TRACE, "Will parse uptime: %s", upTimeNode.getTextContent());
                String dateString = upTimeNode.getTextContent();
                uptime = jrds.Util.parseStringNumber(dateString, 0L);
            }
            log(Level.TRACE, "uptime is %d", uptime);
        } catch (XPathExpressionException e) {
            log(Level.ERROR, e, "Uptime not found");
        }
        return uptime;
    }

    public Map<XPathExpression, Number> fileFromXpaths(Document d, Set<XPathExpression> xpaths) {
        Map<XPathExpression, Number> values = new HashMap<>(xpaths.size());
        for (XPathExpression xpath: xpaths) {
            try {
                log(Level.TRACE, "Will search the xpath \"%s\"", xpath);
                Node n = (Node) xpath.evaluate(d, XPathConstants.NODE);
                if (n != null) {
                    Double value = jrds.Util.parseStringNumber(n.getTextContent(), Double.NaN);
                    values.put(xpath, value);
                }
            } catch (XPathExpressionException e) {
                log(Level.ERROR, "Invalid XPATH : %s for %s", xpath, this);
            } catch (NumberFormatException e) {
                log(Level.WARN, e, "value read from %s not parsable", xpath);
            }
        }
        log(Level.TRACE, "Values found: %s", values);
        return values;
    }

    public Document getDocument(InputSource stream) {
        DocumentBuilder dbuilder = localDocumentBuilder.get();
        log(Level.TRACE, "%s %s %s started %s@%s", stream, dbuilder, isStarted(), getClass().getName(), Util.delayedFormatString(() -> Integer.toHexString(hashCode())));
        try {
            dbuilder.reset();
            Document d = dbuilder.parse(stream);
            log(Level.TRACE, "just parsed a %s", d.getDocumentElement().getTagName());
            return d;
        } catch (SAXException e) {
            log(Level.ERROR, e, "Invalid XML: %s", e);
            return dbuilder.newDocument();
        } catch (IOException e) {
            log(Level.ERROR, e, "IO Exception getting values: %s", e);
            return dbuilder.newDocument();
        }
    }

    public Document getDocument(InputStream stream) {
        return getDocument(new InputSource(stream));
    }

    public Document getDocument(Reader stream) {
        return getDocument(new InputSource(stream));
    }

    /**
     * Used to get an empty document
     * 
     * @return an empty document
     */
    public Document getDocument() {
        DocumentBuilder dbuilder = localDocumentBuilder.get();
        dbuilder.reset();
        return dbuilder.newDocument();
    }

    public NodeList getNodeList(Document d, String xpath) throws XPathExpressionException {
        return (NodeList) localXpath.get().evaluate(xpath, d, XPathConstants.NODESET);
    }

    public Node getNode(Document d, String xpath) throws XPathExpressionException {
        return (Node) localXpath.get().evaluate(xpath, d, XPathConstants.NODE);
    }

    public XPathExpression compile(String path) {
        try {
            return localXpath.get().compile(path);
        } catch (XPathExpressionException ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }

}
