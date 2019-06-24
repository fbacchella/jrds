package jrds.factories.xml;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jrds.Util;

public class CompiledXPath {

    private static final Logger logger = LoggerFactory.getLogger(CompiledXPath.class);

    private static final XPathFactory XFACTORY = XPathFactory.newInstance();
    private static final ThreadLocal<XPath> xpather = new ThreadLocal<XPath>() {
        @Override
        protected XPath initialValue() {
            return XFACTORY.newXPath();
        }
    };

    private static final Map<String, XPathExpression> xpc = new ConcurrentHashMap<>();

    private CompiledXPath() {

    }

    public static XPathExpression get(String xpath) throws XPathExpressionException {
        XPathExpression e = xpc.get(xpath);
        if (e == null) {
            logger.debug("{}", Util.delayedFormatString("Uncompiled xpath: %s", xpath));
            e = xpather.get().compile(xpath);
            xpc.put(xpath, e);
        }
        return e;
    }

}
