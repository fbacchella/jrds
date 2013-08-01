package jrds.webapp;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import jrds.Period;
import jrds.Tools;
import org.junit.Assert;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;

public class TestPack {
    static final private Logger logger = Logger.getLogger(TestPack.class);

    ServletTester tester = null;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @BeforeClass
    static public void configure() throws Exception {
        Tools.configure();
        System.setProperty("org.mortbay.log.class", jrds.standalone.JettyLogger.class.getName());
        Tools.setLevel(logger, Level.TRACE, ParamsBean.class.getName(), JSonQueryParams.class.getName(), JSonPack.class.getName());
    }

    @Before
    public void launchServer() throws Exception {
        tester = ToolsWebApp.getMonoServlet(testFolder, new Properties(), JSonPack.class, "/jsonpack");
        tester.addServlet(JSonQueryParams.class, "/queryparams");

        tester.start();
    }

    private String packunpack(String inparams) throws IOException, Exception {
        HttpTester request = new HttpTester();
        HttpTester response = new HttpTester();
        request.setMethod("POST");
        request.setHeader("Host","tester");
        request.setHeader("Referer","http://tester/");
        request.setURI("/jsonpack");
        request.setVersion("HTTP/1.0");
        request.setContent(inparams);

        response.parse(tester.getResponses(request.generate()));
        Assert.assertEquals(200, response.getStatus());

        URL packedurl = new URL(response.getContent());

        response = ToolsWebApp.doRequestGet(tester, "http://tester/queryparams?" + packedurl.getQuery(), 200);
        logger.trace("queryparams returned: " + response.getContent());

        return(response.getContent());
    }

    @Test
    public void testPack1() throws IOException, Exception {
        JrdsJSONObject params = new JrdsJSONObject( packunpack("{'begin':'2010-08-17 00:00','end':'2010-08-18 23:59', 'min':'0', 'max':'10', 'autoperiod':'0','filter':['All hosts'],'host':'','treeType':'tree','id':'-744958554','path':['All filters','bougie','Services','jdbc:postgresql://appartland.eu/postgres','xwiki']}"));
        Assert.assertEquals(0, params.get("autoperiod"));
    }

    @Test
    public void testPack2() throws IOException, Exception {
        JrdsJSONObject params = new JrdsJSONObject( packunpack("{'begin':'1','end':'60000', 'min':'0', 'max':'10', 'autoperiod':'0','filter':'','host':'hosttest','treeType':'tree','id':'-744958554','path':['All filters','bougie','Services','jdbc:postgresql://appartland.eu/postgres','xwiki']}"));
        Assert.assertEquals(0, params.get("autoperiod"));
        Assert.assertEquals("0", params.get("min"));
        Assert.assertEquals("10", params.get("max"));

        Period p = new Period(params.getString("begin"), params.getString("end"));
        logger.trace(p);
        //		Assert.assertEquals(60 *1000, p.getBegin().getTime());
        //		Assert.assertEquals(3600 * 1000, p.getEnd().getTime());

    }

    @Test
    public void testPack3() throws IOException, Exception {
        JrdsJSONObject params = new JrdsJSONObject( packunpack("{'filter':['All hosts'],'host':'','treeType':'tree','id':'-1025598675','path':['All filters','fe1','System','ntp']}"));
        Assert.assertEquals(7, params.get("autoperiod"));
        Assert.assertEquals(JSONArray.class, params.get("path").getClass());
        Assert.assertEquals("[\"All filters\",\"fe1\",\"System\",\"ntp\"]", params.get("path").toString());
    }
}
