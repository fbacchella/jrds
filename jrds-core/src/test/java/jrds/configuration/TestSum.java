package jrds.configuration;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.event.Level;
import org.w3c.dom.Document;

import jrds.GraphDesc;
import jrds.HostInfo;
import jrds.HostsList;
import jrds.Log4JRule;
import jrds.Tools;
import jrds.factories.xml.JrdsDocument;
import jrds.factories.xml.JrdsElement;
import jrds.graphe.Sum;
import jrds.mockobjects.MockGraph;
import jrds.starter.HostStarter;
import jrds.webapp.ACL;
import jrds.webapp.RolesACL;

public class TestSum {

    static final private String goodSumSXml =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" +
                    "<!DOCTYPE sum PUBLIC \"-//jrds//DTD Sum//EN\" \"urn:jrds:sum\">" +
                    "<sum name=\"sumname\">" +
                    "</sum>";


    @Rule
    public final TemporaryFolder testFolder = new TemporaryFolder();

    @Rule
    public final Log4JRule logrule = new Log4JRule(this);
    private final Logger logger = logrule.getTestlogger();

    @BeforeClass
    static public void configure() throws ParserConfigurationException, IOException {
        Tools.configure();
        Tools.prepareXml(false);
    }

    @Before
    public void loggers() {
        logrule.setLevel(Level.INFO, "jrds.factories.xml.CompiledXPath");
        logrule.setLevel(Level.TRACE, "jrds.factories", "jrds.probe.SumProbe", "jrds.graphe.Sum");
    }

    private Sum doSum(JrdsDocument d, HostsList hl) throws Exception {
        HostInfo host = new HostInfo("SumHost");

        SumBuilder sm = new SumBuilder();
        sm.setPm(Tools.makePm(testFolder, "security=yes"));
        Sum sp = sm.makeSum(d);
        try {
            sp.configure(hl);
        } catch (Exception e) {
        }
        sp.getProbe().setHost(new HostStarter(host));
        hl.addHost(host);
        hl.addProbe(sp.getProbe());

        jrds.GraphNode mg = new MockGraph();
        mg.getGraphDesc().add(GraphDesc.getDsDescBuilder().setName("plot"));

        hl.addHost(mg.getProbe().getHost());
        hl.addProbe(mg.getProbe());

        return sp;
    }

    @Test
    public void testLoad() throws Exception {
        JrdsDocument d = Tools.parseString(goodSumSXml);
        JrdsElement je = d.getRootElement();
        je.addElement("element", "name=DummyHost/DummyProbe");

        HostsList hl = new HostsList();
        Sum s = doSum(d, hl);
        Document sumDocument = s.getGraphDesc().dumpAsXml();
        logger.trace("{}", sumDocument);
    }

    @Test
    public void testRoles() throws Exception {
        JrdsDocument d = Tools.parseString(goodSumSXml);
        JrdsElement je = d.getRootElement();
        je.addElement("element", "name=DummyHost/DummyProbe");
        je.addElement("role").setTextContent("role1");

        HostsList hl = new HostsList();
        Sum s = doSum(d, hl);

        ACL acl = s.getACL();
        Assert.assertEquals("Not an role ACL", RolesACL.class, acl.getClass());

    }

}
