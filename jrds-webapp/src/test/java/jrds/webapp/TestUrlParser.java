package jrds.webapp;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import jrds.GraphNode;
import jrds.HostsList;
import jrds.Log4JRule;
import jrds.Period;
import jrds.PropertiesManager;
import jrds.Tools;
import jrds.mockobjects.GetMoke;
import jrds.mockobjects.MockGraph;
import jrds.mockobjects.MokeProbe;

public class TestUrlParser {
    static final private DateFormat fullISOFORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    static private HostsList hl;

    @BeforeClass
    static public void configure() {
        System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.Slf4jLog");
        System.setProperty("org.eclipse.jetty.LEVEL", "DEBUG");
        Tools.configure();
        hl = new HostsList(new PropertiesManager());
    }

    @Rule
    public final Log4JRule logrule = new Log4JRule(this);
    private final Logger logger = logrule.getTestlogger();

    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, ParamsBean.class.getName());
    }

    @Test
    public void checkId() {
        Map<String, String[]> parameters = new HashMap<>();
        parameters.put("id", new String[] { "1" });
        ParamsBean pb = new ParamsBean(GetMoke.getRequest(parameters), hl);
        Assert.assertEquals(Integer.valueOf(1), pb.getId());
    }

    @Test
    public void checkIdRest() {
        Map<String, String[]> parameters = new HashMap<>();
        parameters.put("host", new String[] { "DummyHost" });
        parameters.put("graphname", new String[] { "MockGraph" });

        MokeProbe<String, Number> p = new MokeProbe<>();
        p.configure();
        GraphNode gn = new MockGraph(p);
        gn.getGraphDesc().setGraphName("MockGraph");
        logger.trace("{}", gn);
        p.addGraph(gn);
        hl.addProbe(p);
        ParamsBean pb = new ParamsBean(GetMoke.getRequest(parameters), hl);
        Assert.assertEquals("Graph not found by path", Integer.valueOf(gn.hashCode()), pb.getId());
    }

    @Test
    public void checkGraphRestURL1() throws ParseException {
        Map<String, String[]> parameters = new HashMap<>();
        parameters.put("begin", new String[] { "2007-01-01" });
        parameters.put("end", new String[] { "2007-12-31" });
        Date begin = fullISOFORMAT.parse("2007-01-01T00:00:00");
        Date end = fullISOFORMAT.parse("2007-12-31T23:59:59");
        MokeProbe<String, Number> p = new MokeProbe<>();
        p.configure();
        GraphNode gn = new MockGraph(p);
        gn.getGraphDesc().setGraphName("MockGraphInstance");
        logger.debug("Graph name: " + gn.getGraphDesc().getGraphName());
        logger.trace("host:" + p.getHost());

        p.addGraph(gn);
        hl.addProbe(p);

        ParamsBean pb = new ParamsBean(GetMoke.getRequest(parameters, "DummyHost", "MockGraphInstance"), hl, "host", "graphname");
        Assert.assertEquals("Graph not found by path", Integer.valueOf(gn.hashCode()), pb.getId());
        Assert.assertEquals("begin definition invalid", pb.getBegin(), begin.getTime());
        Assert.assertEquals("end definition invalid", pb.getEnd(), end.getTime());
    }

    /**
     * Test that missing rest argument can be provided as cgi parameters
     * 
     * @throws ParseException
     */
    @Test
    public void checkGraphRestURL2() throws ParseException {
        Map<String, String[]> parameters = new HashMap<>();
        parameters.put("graphname", new String[] { "MockGraphInstance" });
        parameters.put("begin", new String[] { "2007-01-01" });
        parameters.put("end", new String[] { "2007-12-31" });
        Date begin = fullISOFORMAT.parse("2007-01-01T00:00:00");
        Date end = fullISOFORMAT.parse("2007-12-31T23:59:59");
        MokeProbe<String, Number> p = new MokeProbe<>();
        p.configure();
        GraphNode gn = new MockGraph(p);
        gn.getGraphDesc().setGraphName("MockGraphInstance");
        logger.debug("Graph name: " + gn.getGraphDesc().getGraphName());
        logger.trace("host:" + p.getHost());

        p.addGraph(gn);
        hl.addProbe(p);

        ParamsBean pb = new ParamsBean(GetMoke.getRequest(parameters, "DummyHost"), hl, "host", "graphname");
        Assert.assertEquals("Graph not found by path", Integer.valueOf(gn.hashCode()), pb.getId());
        Assert.assertEquals("begin definition invalid", pb.getBegin(), begin.getTime());
        Assert.assertEquals("end definition invalid", pb.getEnd(), end.getTime());
    }

    @Test
    public void checkSortedTrue() {
        Map<String, String[]> parameters = new HashMap<>();
        parameters.put("sort", new String[] { "true" });
        ParamsBean pb = new ParamsBean(GetMoke.getRequest(parameters), hl);
        Assert.assertTrue(pb.isSorted());
    }

    @Test
    public void checkSortedFalseDefault() {
        Map<String, String[]> parameters = new HashMap<>();
        ParamsBean pb = new ParamsBean(GetMoke.getRequest(parameters), hl);
        Assert.assertTrue(!pb.isSorted());
    }

    @Test
    public void checkParseDate1() throws ParseException {
        Map<String, String[]> parameters = new HashMap<>();
        parameters.put("begin", new String[] { "2007-01-01" });
        parameters.put("end", new String[] { "2007-12-31" });
        ParamsBean pb = new ParamsBean(GetMoke.getRequest(parameters), hl);
        Period p = pb.getPeriod();

        Date begin = fullISOFORMAT.parse("2007-01-01T00:00:00");
        Date end = fullISOFORMAT.parse("2007-12-31T23:59:59");
        Assert.assertEquals(p.getBegin(), begin);
        Assert.assertEquals(p.getEnd(), end);
        Assert.assertEquals(Period.Scale.MANUAL, p.getScale());

        String url = pb.makeObjectUrl("root", "", false);
        Assert.assertTrue(url.contains("begin=" + begin.getTime()));
        Assert.assertTrue(url.contains("end=" + end.getTime()));

        logger.trace(url);
    }

    @Test
    public void checkParseDate2() {
        Map<String, String[]> parameters = new HashMap<>();
        ParamsBean pb = new ParamsBean(GetMoke.getRequest(parameters), hl);
        Period p = pb.getPeriod();

        // Period drop milliseconds
        Date now = new Date((long) (Math.floor(new Date().getTime() / 1000) * 1000L));
        long offset = Math.abs(now.getTime() - p.getEnd().getTime());
        logger.debug("end offset:" + offset);
        Assert.assertTrue("bad value for offset:" + offset, offset > 980 && offset < 1200);
        Assert.assertEquals(Period.Scale.DAY, p.getScale());
        String url = pb.makeObjectUrl("root", "", false);
        logger.trace(url);
    }

    @Test
    public void checkParseDate3() {
        Map<String, String[]> parameters = new HashMap<>();
        parameters.put("scale", new String[] { "4" });
        ParamsBean pb = new ParamsBean(GetMoke.getRequest(parameters), hl);
        Period p = pb.getPeriod();

        // Period drop milliseconds
        Date now = new Date((long) (Math.floor(new Date().getTime() / 1000) * 1000L));
        Calendar calBegin = Calendar.getInstance();
        calBegin.setTime(now);
        calBegin.add(Calendar.HOUR, -4);
        Date begin = calBegin.getTime();

        long delta = Math.abs(p.getBegin().getTime() - begin.getTime());
        long offset = Math.abs(now.getTime() - p.getEnd().getTime());
        Assert.assertTrue("bad value for offset:" + offset, offset > 980 && offset < 1200);
        Assert.assertTrue("delta begin is too high: " + delta, delta < 1000);
        Assert.assertEquals(Period.Scale.HOURS4, p.getScale());
        String url = pb.makeObjectUrl("root", "", false);
        logger.trace(url);
    }

    @Test
    public void checkUrl1() {
        Map<String, String[]> parameters = new HashMap<>();
        parameters.put("host", new String[] { "host" });
        parameters.put("scale", new String[] { "2" });
        parameters.put("max", new String[] { "2" });
        parameters.put("min", new String[] { "2" });
        ParamsBean pb = new ParamsBean(GetMoke.getRequest(parameters), hl);
        String url = pb.makeObjectUrl("root", "", false);
        logger.trace(url);
        Assert.assertTrue(url.contains("/root?"));
        Assert.assertTrue(url.contains("id=" + "".hashCode()));
        Assert.assertTrue(url.contains("scale=2"));
        Assert.assertTrue(url.contains("max=2"));
        Assert.assertTrue(url.contains("min=2"));
        Assert.assertFalse(url.contains("begin="));
        Assert.assertFalse(url.contains("end="));
    }

    @Test
    public void checkUrl2() {
        Map<String, String[]> parameters = new HashMap<>();
        parameters.put("max", new String[] { String.valueOf(Double.NaN) });
        parameters.put("min", new String[] { String.valueOf(Double.NaN) });
        ParamsBean pb = new ParamsBean(GetMoke.getRequest(parameters), hl);
        jrds.Filter f = new jrds.FilterHost("host");
        String url = pb.makeObjectUrl("root", f, true);
        logger.trace(url);
        Assert.assertTrue(url.contains("host=host"));
        Assert.assertTrue(url.contains("/root?"));
        Assert.assertFalse(url.contains("id=" + f.hashCode()));
        Assert.assertTrue(url.contains("begin="));
        Assert.assertTrue(url.contains("end="));
        Assert.assertFalse(url.contains("scale="));
        Assert.assertTrue(url.contains("max="));
        Assert.assertTrue(url.contains("min="));
    }

    @Test
    public void checkUrl3() {
        Map<String, String[]> parameters = new HashMap<>();
        jrds.Filter f = jrds.Filter.ALLHOSTS;
        String filterName = f.getName();
        parameters.put("filter", new String[] { filterName });

        ParamsBean pb = new ParamsBean(GetMoke.getRequest(parameters), hl);
        String url = pb.makeObjectUrl("root", f, true);
        Assert.assertTrue(url.contains("filter=" + URLEncoder.encode(filterName, StandardCharsets.UTF_8)));
        Assert.assertTrue(url.contains("/root?"));
        Assert.assertFalse(url.contains("id=" + f.hashCode()));
        Assert.assertTrue(url.contains("begin="));
        Assert.assertTrue(url.contains("end="));
        Assert.assertFalse(url.contains("scale="));
    }

    // @Test
    // public void testUnpack() {
    // Map<String, String[]> parameters = new HashMap<String, String[]>();
    // parameters.put("p", new String []
    // {"E3JMQqAMBBE0auEqRPYNcbodp5DbISogYBgtBLvbiwEq3n8uWAhA%2FqU1BzTEfYMjWk7lxgwavB3rls%2B8lsIYljDQwo1mrL8whVUxGSoNdyqyorrSq7%2F2SsiIcL9ACNvWBB2AAAA"});
    // ParamsBean pb = new ParamsBean(GetMoke.getRequest(parameters), hl);
    // }
    //

    @Test
    public void testPath() {
        Map<String, String[]> parameters = new HashMap<>();
        HttpServletRequest req = GetMoke.getRequest(parameters);
        ParamsBean pb = new ParamsBean(req, hl);
        String buildurl = pb.makeObjectUrl("graph", jrds.Filter.ALLHOSTS, true);
        Assert.assertTrue("bad build url: " + buildurl, buildurl.startsWith("/graph?"));
    }

}
