package jrds;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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

import jrds.mockobjects.GenerateProbe;
import jrds.mockobjects.MokeProbe;
import jrds.starter.HostStarter;

public class UtilTest {

    @Rule
    public final Log4JRule logrule = new Log4JRule(this);
    private final Logger logger = logrule.getTestlogger();

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @BeforeClass
    static public void configure() throws ParserConfigurationException {
        Tools.configure();
        Tools.prepareXml();
    }

    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, "jrds.Util");
    }

    @Test
    public void testSiPrefix1() {
        Assert.assertEquals(3, jrds.Util.SiPrefix.k.getExponent());
        Assert.assertEquals(0, jrds.Util.SiPrefix.FIXED.getExponent());
        Assert.assertEquals(34.0, jrds.Util.SiPrefix.FIXED.evaluate(34, true), 1e-10);
        Assert.assertEquals(0.001, jrds.Util.SiPrefix.m.evaluate(1, true), 1e-10);
        Assert.assertEquals(1024.0, jrds.Util.SiPrefix.k.evaluate(1, false), 1e-10);
        Assert.assertEquals(1024 * 1024.0, jrds.Util.SiPrefix.M.evaluate(1, false), 1e-10);
    }

    @Test
    public void testSiPrefix2() {
        int lastExp = 24;
        for(jrds.Util.SiPrefix p: jrds.Util.SiPrefix.values()) {
            int newExp = p.getExponent();
            Assert.assertTrue(newExp - lastExp <= -1 || newExp - lastExp >= -3);
        }
    }

    @Test
    public void testSerialization1() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document d;

        d = Tools.parseRessource("customgraph.xml");
        Util.serialize(d, out, null, null);
    }

    @Test
    public void testSerialization2() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document d;
        Map<String, String> prop;
        String outBuffer;

        d = Tools.parseRessource("goodhost1.xml");
        prop = new HashMap<>();
        prop.put("omit-xml-declaration", "yes");
        Util.serialize(d, out, null, prop);
        outBuffer = out.toString();
        logger.debug(outBuffer);
        Assert.assertFalse(outBuffer.contains("<?xml"));
    }

    @Test
    public void testSerialization3() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document d;
        Map<String, String> prop;
        String outBuffer;

        d = Tools.parseRessource("customgraph.xml");
        String publicId = d.getDoctype().getPublicId();
        String systemId = d.getDoctype().getSystemId();
        prop = new HashMap<>();

        Util.serialize(d, out, null, prop);
        outBuffer = out.toString();
        logger.debug("out buffer for testSerialization3: \n" + outBuffer + "\n");
        // It should have auto-detected the doc type
        Assert.assertTrue(outBuffer.contains(publicId));
        Assert.assertTrue(outBuffer.contains(systemId));
        logger.debug("{}", outBuffer.contains(d.getXmlEncoding()));
        Assert.assertTrue("Output encoding " + d.getXmlEncoding() + " not found", outBuffer.contains(d.getXmlEncoding()));
    }

    @Test
    public void testParseStringNumber1() {
        double n = Util.parseStringNumber("1", Double.NaN);
        Assert.assertEquals(1.0, n, 0.001);
    }

    @Test
    public void testParseStringNumber2() {
        double n = Util.parseStringNumber(null, Double.NaN);
        Assert.assertTrue(Double.isNaN(n));
    }

    @Test
    public void testParseStringNumber3() {
        double n = Util.parseStringNumber("a", Double.NaN);
        Assert.assertTrue(Double.isNaN(n));
    }

    @Test
    public void testParseStringNumber5() {
        int n = Util.parseStringNumber("1", 2);
        Assert.assertEquals(1, n);
    }

    @Test
    public void testParseStringNumber6() {
        double n = Util.parseStringNumber("1", 2.0d);
        Assert.assertEquals(1.0, n, 0.001);
    }

    @Test
    public void testParseStringNumber7() {
        double n = Util.parseStringNumber("1.33427403132E11", 1.0d);
        Assert.assertEquals(1.33427403132E11, n, 1e-10);
    }

    @Test
    public void testParseOldTemplate1() {
        Probe<?, ?> p = new MokeProbe<String, Number>();
        p.setHost(new HostStarter(new HostInfo("Moke")));
        p.setLabel("label");
        Object[] keys = {
                         "${host}",
                         "${probename}",
                         "${label}"
        };
        String parsed = Util.parseOldTemplate("{0} {1} {2} ${label}", keys, p);
        Assert.assertEquals("Moke DummyProbe label label", parsed);
    }

    @Test
    public void testParseOldTemplate2() {
        Probe<?, ?> p = new MokeProbe<String, Number>();
        p.setHost(new HostStarter(new HostInfo("Moke")));
        p.setLabel("label");
        Object[] keys = {
                         "${host}",
                         "${probename}",
                         "${label}"
        };
        String parsed = Util.parseOldTemplate("${label} {0} {1} {2} ${label}", keys, p);
        Assert.assertEquals("label Moke DummyProbe label label", parsed);
    }

    @Test
    public void testParseTemplate1() {
        Probe<?, ?> p = new MokeProbe<String, Number>();
        p.setHost(new HostStarter(new HostInfo("Moke")));
        p.setLabel("label");
        String parsed = Util.parseTemplate("'${host}' \"${probename}\" ${label}", p);
        Assert.assertEquals("'Moke' \"DummyProbe\" label", parsed);
    }

    @Test
    public void testParseTemplate2() {
        System.setProperty("jrds.unittest", "true");

        String parsed = Util.parseTemplate("${jrds.unittest}", System.getProperties());
        Assert.assertEquals("true", parsed);
    }

    @Test
    public void testParseTemplate3() {
        List<String> args = Arrays.asList("unittest1", "unittest2");

        String parsed = Util.parseTemplate("${1} ${2}", args);
        Assert.assertEquals("unittest1 unittest2", parsed);
    }

    @Test
    public void testParseTemplate4() {
        String parsed = Util.parseTemplate("%string");
        Assert.assertEquals("%string", parsed);
    }

    @Test
    public void testParseTemplate5() throws Exception {
        Probe<String, Number> testProbe = GenerateProbe.fillProbe(new GenerateProbe.EmptyProbe<>(), testFolder, GenerateProbe.ChainedMap.start());
        GenericBean b1 = new GenericBean.CustomBean("one");
        b1.set(testProbe, "one");
        testProbe.getPd().addBean(b1);
        GenericBean b2 = new GenericBean.CustomBean("two");
        testProbe.getPd().addBean(b2);
        String parsed = Util.parseTemplate("${attr.one} ${attr.two} ${attr.two.signature} ${attr.three} ${probename}", testProbe);
        Assert.assertEquals("one ${attr.two} ${attr.two.signature}  EmptyProbe", parsed);
    }

    @Test
    @SuppressWarnings("unused")
    public void testParseTemplate6() {
        Object o = new Object() {
            public String getOne() {
                return "one";
            }
            public void setOne(String bean) {
            }
            public String getTwo() {
                return null;
            }
            public void setTwo(String bean) {
            }
        };
        String parsed = Util.parseTemplate("${attr.one} ${attr.two} ${attr.two.signature} ${attr.three}", o);
        Assert.assertEquals("one ${attr.two} ${attr.two.signature} ", parsed);
    }

    @Test
    public void testNormalization1() {
        Probe<?, ?> p = new jrds.mockobjects.DummyProbe() {
            @Override
            public Date getLastUpdate() {
                return new Date();
            }

            @Override
            public int getStep() {
                return 300;
            }
        };
        Date now = new Date();

        Date n = Util.endDate(p, now);
        Assert.assertTrue(Math.abs(now.getTime() - n.getTime()) < 500 * 1000);
    }

    @Test
    public void testNormalization2() {
        Probe<?, ?> p = new jrds.mockobjects.DummyProbe() {
            @Override
            public Date getLastUpdate() {
                Date now = new Date();
                Calendar calBegin = Calendar.getInstance();
                calBegin.setTime(now);
                calBegin.add(Calendar.MONTH, -4);
                return calBegin.getTime();
            }

            @Override
            public int getStep() {
                return 300;
            }
        };

        Date now = new Date();

        Date n = Util.endDate(p, now);
        Assert.assertTrue(Math.abs(now.getTime() - n.getTime()) < 500 * 1000);
    }

    @Test
    public void testNormalization3() {
        Probe<?, ?> p = new jrds.mockobjects.DummyProbe() {
            @Override
            public Date getLastUpdate() {
                Date now = new Date();
                Calendar calBegin = Calendar.getInstance();
                calBegin.setTime(now);
                calBegin.add(Calendar.MONTH, 4);
                return calBegin.getTime();
            }

            @Override
            public int getStep() {
                return 300;
            }
        };

        Date now = new Date();

        Date n = Util.endDate(p, now);
        Assert.assertTrue(Math.abs(now.getTime() - n.getTime()) < 500 * 1000);
    }

    @Test
    public void testSort() {
        // Test using a set, as value that compare with 0 will be merged
        String[] toSort = new String[] {
                "zOS", "linux", "Linux", "host10", "host2", "host03",
                "st001", "st01a", "st01"};
        Set<String> sorted = new TreeSet<>(jrds.Util.nodeComparator);
        sorted.addAll(Arrays.asList(toSort));
        Assert.assertEquals("[host2, host03, host10, linux, Linux, st001, st01, st01a, zOS]", sorted.toString());
    }

}
