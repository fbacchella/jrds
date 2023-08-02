package jrds.webapp;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.eclipse.jetty.http.HttpTester.Request;
import org.eclipse.jetty.http.HttpTester.Response;
import org.eclipse.jetty.servlet.ServletTester;
import org.json.JSONArray;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import jrds.Log4JRule;
import jrds.Period;
import jrds.Tools;

public class TestPack {

    ServletTester tester = null;

    @Rule
    public final TemporaryFolder testFolder = new TemporaryFolder();

    @Rule
    public final Log4JRule logrule = new Log4JRule(this);
    private final Logger logger = logrule.getTestlogger();

    @BeforeClass
    static public void configure() throws Exception {
        System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.Slf4jLog");
        System.setProperty("org.eclipse.jetty.LEVEL", "DEBUG");
        Tools.configure();
    }

    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, ParamsBean.class.getName(), JSonQueryParams.class.getName(), JSonPack.class.getName());
    }

    @Before
    public void launchServer() throws Exception {
        tester = ToolsWebApp.getMonoServlet(testFolder, new Properties(), JSonPack.class, "/jsonpack");
        tester.addServlet(JSonQueryParams.class, "/queryparams");

        tester.start();
    }

    private String packunpack(final String inparams) throws Exception {
        Response response = ToolsWebApp.doRequestPost(tester, "http://tester/jsonpack", new ToolsWebApp.MakePostContent() {
            @Override
            void fillRequest(Request r) {
                r.setContent(inparams);
            }
        }, 200);

        URL packedurl = new URL(response.getContent());

        response = ToolsWebApp.doRequestGet(tester, "http://tester/queryparams?" + packedurl.getQuery(), 200);
        logger.trace("queryparams returned: " + response.getContent());

        return (response.getContent());
    }

    @Test
    public void testPack1() throws Exception {
        JrdsJSONObject params = new JrdsJSONObject(packunpack("{'begin':'2010-08-17 00:00','end':'2010-08-18 23:59', 'min':'0', 'max':'10', 'autoperiod':'0','filter':['All hosts'],'host':'','treeType':'tree','id':'-744958554','path':['All filters','bougie','Services','jdbc:postgresql://appartland.eu/postgres','xwiki']}"));
        Assert.assertEquals(Period.Scale.MANUAL.ordinal(), params.get("autoperiod"));
    }

    @Test
    public void testPack2() throws Exception {
        JrdsJSONObject params = new JrdsJSONObject(packunpack("{'begin':'1000','end':'60000', 'min':'0', 'max':'10', 'autoperiod':'0','filter':'','host':'hosttest','treeType':'tree','id':'-744958554','path':['All filters','bougie','Services','jdbc:postgresql://appartland.eu/postgres','xwiki']}"));
        Assert.assertEquals(Period.Scale.MANUAL.ordinal(), params.get("autoperiod"));
        Assert.assertEquals("0", params.get("min"));
        Assert.assertEquals("10", params.get("max"));
        Period p = new Period(params.get("begin").toString(), params.get("end").toString());
        logger.trace("{}", p);
        Assert.assertEquals(1000, p.getBegin().getTime());
        Assert.assertEquals(60000, p.getEnd().getTime());
    }

    @Test
    public void testPack3() throws Exception {
        JrdsJSONObject params = new JrdsJSONObject(packunpack("{'filter':['All hosts'],'host':'','treeType':'tree','id':'-1025598675','path':['All filters','fe1','System','ntp']}"));
        Assert.assertEquals(Period.Scale.DAY.ordinal(), params.get("autoperiod"));
        Assert.assertEquals(JSONArray.class, params.get("path").getClass());
        Assert.assertEquals("[\"All filters\",\"fe1\",\"System\",\"ntp\"]", params.get("path").toString());
    }
}
