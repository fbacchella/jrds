package jrds.probe;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;

import jrds.HostInfo;
import jrds.Log4JRule;
import jrds.Probe;
import jrds.ProbeDesc;
import jrds.Tools;
import jrds.Util;
import jrds.starter.HostStarter;

public class HttpTest {

    static final private String HOST = "testhost";
    static final private HostStarter webserver = new HostStarter(new HostInfo(HOST));
    
    static private class TestHttpProbe extends HttpProbe<String> {
        @Override
        protected Map<String, Number> parseStream(InputStream stream) {
            return Collections.emptyMap();
        }
    }

    @Rule
    public final Log4JRule logrule = new Log4JRule(this);

    @BeforeClass
    static public void configure() {
        Tools.configure();
    }

    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, "jrds.Util");
    }

    private void validateBean(HttpProbe<String> p) throws IllegalArgumentException {
        Assert.assertEquals("invalid url bean", p.getUrl(), p.getPd().getBean("url").get(p));
        Assert.assertEquals("invalid port bean", p.getPort(), p.getPd().getBean("port").get(p));
        Assert.assertEquals("invalid file bean", p.getFile(), p.getPd().getBean("file").get(p));

        Assert.assertEquals("invalid url bean template", p.getUrl().toString(), Util.parseTemplate("${attr.url}", p));
        Assert.assertEquals("invalid port bean template", p.getPort().toString(), Util.parseTemplate("${attr.port}", p));
        Assert.assertEquals("invalid file bean template", p.getFile(), Util.parseTemplate("${attr.file}", p));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void build1() throws IllegalArgumentException, InvocationTargetException {
        HttpProbe<String> p = new TestHttpProbe();
        ProbeDesc<String> pd = new ProbeDesc<>();
        pd.setProbeClass((Class<? extends Probe<String, ?>>) p.getClass());
        p.setHost(webserver);
        p.setPd(pd);
        p.setFile("/");
        p.setPort(80);
        Assert.assertTrue(p.configure());
        Assert.assertEquals("http://" + HOST + ":80/", p.getUrlAsString());
        validateBean(p);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void build2() throws IllegalArgumentException, InvocationTargetException {
        HttpProbe<String> p = new TestHttpProbe();
        p.setHost(webserver);
        ProbeDesc<String> pd = new ProbeDesc<>();
        pd.setProbeClass((Class<? extends Probe<String, ?>>) p.getClass());
        p.setPd(pd);
        p.setFile("/file");
        p.setPort(80);
        Assert.assertTrue(p.configure("/file"));
        Assert.assertEquals("http://" + HOST + ":80/file", p.getUrlAsString());
        validateBean(p);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void build3() throws IllegalArgumentException, InvocationTargetException {
        HttpProbe<String> p = new TestHttpProbe();
        p.setHost(webserver);
        ProbeDesc<String> pd = new ProbeDesc<>();
        pd.setProbeClass((Class<? extends Probe<String, ?>>) p.getClass());
        p.setPd(pd);
        p.setFile("/file");
        p.setPort(81);
        Assert.assertTrue(p.configure());
        Assert.assertEquals("http://" + HOST + ":81/file", p.getUrlAsString());
        Assert.assertEquals("http://" + HOST + ":81/file", pd.getBean("url").get(p).toString());
        validateBean(p);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void build4() throws IllegalArgumentException, InvocationTargetException {
        HttpProbe<String> p = new TestHttpProbe();
        ProbeDesc<String> pd = new ProbeDesc<>();
        pd.setProbeClass((Class<? extends Probe<String, ?>>) p.getClass());
        p.setHost(webserver);
        p.setPd(pd);
        p.setFile("/");
        p.setPort(80);
        p.setLogin("login@domain");
        p.setPassword("password");
        Assert.assertTrue(p.configure());
        Assert.assertEquals("http://" + HOST + ":80/", p.getUrlAsString());
        validateBean(p);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void build5() throws IllegalArgumentException, InvocationTargetException {
        HttpProbe<String> p = new TestHttpProbe();
        ProbeDesc<String> pd = new ProbeDesc<>();
        pd.setProbeClass((Class<? extends Probe<String, ?>>) p.getClass());
        p.setHost(webserver);
        p.setPd(pd);
        p.setFile("/${1}");
        p.setPort(80);
        List<Object> args = List.of("file");
        Assert.assertTrue(p.configure(args));
        Assert.assertEquals("http://" + HOST + ":80/file", p.getUrlAsString());
        validateBean(p);
    }

}
