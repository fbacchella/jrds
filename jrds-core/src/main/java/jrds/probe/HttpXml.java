package jrds.probe;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPathExpression;

import org.slf4j.event.Level;
import org.w3c.dom.Document;

import jrds.CollectResolver;
import jrds.Util;
import jrds.factories.ProbeMeta;
import jrds.starter.XmlProvider;

/**
 * This probe can be used to collect values read from an XML document extracted
 * from a defined URL
 * <p>
 * The specific keywords it uses are
 * <ul>
 * <li>upTimePath – An xpath to the uptime</li>
 * <li>nouptime – No uptime is provided</li>
 * <li>startTimePath – when no uptime is provided, the xpath to the start time
 * of the agent</li>
 * <li>currentTimePath – when no uptime is provided, the xpath to the current
 * time of the agent</li>
 * <li>timePattern – when no uptime is provided, a pattern that will be used by
 * SimpleDateFormat object to parse time</li>
 * </ul>
 * This probe uses <a href="http://hc.apache.org/httpclient-3.x/">Apache's
 * Commons HttpClient</a>.
 * 
 * @author Fabrice Bacchella
 */
@ProbeMeta(
           topStarter = jrds.starter.XmlProvider.class,
           collectResolver = CollectResolver.StringResolver.class
                )
public class HttpXml extends HCHttpProbe<String> {

    private Map<XPathExpression, String> collectKeys = null;

    @Override
    protected boolean finishConfigure(List<Object> args) {
        log(Level.TRACE, "Configuring collect xpath with %s", args);
        CollectResolver<XPathExpression> cr = new XmlProvider.XmlResolver();
        collectKeys = new HashMap<>(getPd().getCollectMapping().size());
        for(Map.Entry<String, String> e: getPd().getCollectMapping().entrySet()) {
            String solved = Util.parseTemplate(String.format(e.getKey(), args != null ? args.toArray() : null), this, args);
            XPathExpression xpath = cr.resolve(solved);
            collectKeys.put(xpath, solved);
        }
        collectKeys = Collections.unmodifiableMap(collectKeys);
        log(Level.TRACE, "collect xpath mapping %s", collectKeys);
        return super.finishConfigure(args);
    }

    /**
     * A method that can be overriden to extract more values from the XML
     * document
     * 
     * @param d the XML document read from the URL
     * @param variables already parsed variables that can be overriden
     * @return a new set of variables, this implementation return variables
     *         without modifications
     */
    public Map<String, Number> dom2Map(Document d, Map<String, Number> variables) {
        return variables;
    }

    /**
     * Extract the the uptime from the XML document, reusing the XML provider
     * utilites
     * 
     * @param xmlstarter
     * @param d
     * @return
     */
    protected long findUptime(XmlProvider xmlstarter, Document d) {
        String upTimePath = getPd().getSpecific("upTimePath");
        if(upTimePath != null) {
            return xmlstarter.findUptime(d, upTimePath);
        } else if(getPd().getSpecific("nouptime") != null) {
            return Long.MAX_VALUE;
        } else {
            String startTimePath = getPd().getSpecific("startTimePath");
            String currentTimePath = getPd().getSpecific("currentTimePath");
            String timePattern = getPd().getSpecific("timePattern");
            if(startTimePath != null && currentTimePath != null && timePattern != null) {
                DateFormat df = new SimpleDateFormat(timePattern);
                return xmlstarter.findUptimeByDate(d, startTimePath, currentTimePath, df);
            } else {
                log(Level.ERROR, "No xpath for the uptime");
                return 0;
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see jrds.probe.HttpProbe#parseStream(java.io.InputStream)
     */
    @Override
    protected Map<String, Number> parseStream(InputStream stream) {
        XmlProvider xmlstarter = find(XmlProvider.class);
        if(xmlstarter == null) {
            log(Level.ERROR, "XML Provider not found");
            return null;
        }

        Document d = xmlstarter.getDocument(stream);
        if (! validateXml(xmlstarter, d)) {
            return null;
        }
        setUptime(findUptime(xmlstarter, d));
        Map<String, Number> vars = new HashMap<>(collectKeys.size());
        for (Map.Entry<XPathExpression, Number> e: xmlstarter.fileFromXpaths(d, collectKeys.keySet()).entrySet()) {
            vars.put(collectKeys.get(e.getKey()), e.getValue());
        }
        log(Level.TRACE, "Values found: %s", vars);
        log(Level.TRACE, "%s", vars);
        vars = dom2Map(d, vars);
        log(Level.TRACE, "%s", vars);
        return vars;
    }

    /**
     * This method can be used to check that a already parsed XML is comforming some
     * expected rules. The default implementation always return true
     * @param xmlstarter the help class for XML manipulation
     * @param d the document to validate
     * @return true if it complies with expected rules
     */
    protected boolean validateXml(XmlProvider xmlstarter, Document d) {
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jrds.probe.HttpProbe#getSourceType()
     */
    @Override
    public String getSourceType() {
        return "HttpXml";
    }

}
