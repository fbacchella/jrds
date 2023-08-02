package jrds.probe.snmp;

import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;

import jrds.Log4JRule;
import jrds.Probe;
import jrds.Tools;
import jrds.factories.ProbeMeta;
import jrds.factories.xml.JrdsDocument;
import jrds.webapp.DiscoverAgent;

public class TestDiscoverAgent {

    @Rule
    public final Log4JRule logrule = new Log4JRule(this);

    @BeforeClass
    static public void configure() throws ParserConfigurationException {
        Tools.configure();
        Tools.prepareXml();
    }

    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, "jrds.DiscoverAgent.SNMP");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getHtmlCode() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Class<? extends Probe<?, ?>> snmpProbeClass = (Class<? extends Probe<?, ?>>) Class.forName("jrds.probe.snmp.SnmpProbe");
        ProbeMeta m = snmpProbeClass.getAnnotation(ProbeMeta.class);
        DiscoverAgent da = m.discoverAgent().newInstance();
        Assert.assertEquals(3, da.getFields().size());
        JrdsDocument d = new JrdsDocument(Tools.dbuilder.newDocument());
        d.doRootElement("host");
        da.doHtmlDiscoverFields(d);
        Map<String, String> properties = new HashMap<>();
        properties.put(OutputKeys.INDENT, "yes");
        properties.put("{http://xml.apache.org/xslt}indent-amount", "4");
    }

}
