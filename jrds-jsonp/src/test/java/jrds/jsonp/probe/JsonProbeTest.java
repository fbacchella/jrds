package jrds.jsonp.probe;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.event.Level;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JsonOrgJsonProvider;
import com.jayway.jsonpath.spi.mapper.JsonOrgMappingProvider;

import jrds.HostInfo;
import jrds.JrdsSample;
import jrds.Log4JRule;
import jrds.ProbeDesc;
import jrds.PropertiesManager;
import jrds.Tools;
import jrds.jsonp.starter.JsonpProvider;
import jrds.probe.HttpClientStarter;
import jrds.starter.HostStarter;

public class JsonProbeTest {

    @Rule
    public final TemporaryFolder testFolder = new TemporaryFolder();

    @Rule
    public final Log4JRule logrule = new Log4JRule(this);

    @BeforeClass
    static public void configure() throws Exception {
        Tools.configure();
        Tools.prepareXml(false);
    }

    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, "jrds.jsonp.probe.HttpJson", "jrds.jsonp.starter.JsonpProvider");
    }

    // Expected to fail when https://github.com/json-path/JsonPath/issues/497 will be corrected
    @Test(expected=NullPointerException.class)
    public void issue497() throws IOException {
        String json = "{\"foo\": \"bar\", \"emptyObject\": {}}";

        Configuration config = Configuration.defaultConfiguration()
                        .jsonProvider(new JsonOrgJsonProvider())
                        .mappingProvider(new JsonOrgMappingProvider());

        Object result = JsonPath.using(config).parse(json).read("$..foo");
        Assert.assertNotNull(result);
    }

    @Test
    public void parseJson() throws Exception {
        PropertiesManager pm = Tools.makePm(testFolder, "timeout=1", "collectorThreads=1");

        URL dataurl = getClass().getResource("/data.json");
        HttpJson p = new HttpJson() {
            @Override
            public String getName() {
                return "Moke";
            }
            @Override
            public Map<String, Number> getNewSampleValues() {
                try (InputStream is = url.openStream()) {
                    return parseStream(is);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        };
        HostStarter host = new HostStarter(new HostInfo("moke", "127.0.0.1"));
        ProbeDesc<String> pd = jrds.configuration.GeneratorHelper.getProbeDesc(Tools.parseRessource("httpjsonprobedesc.xml"));
        p.setHost(host);
        p.setPd(pd);
        p.setUrl(dataurl);
        p.setLabel("f");
        host.registerStarter(new JsonpProvider());
        HttpClientStarter cnx = new HttpClientStarter();
        cnx.configure(pm);
        host.registerStarter(cnx);

        p.configure();
        host.startCollect();
        p.startCollect();
        Map<String, Number> vars = p.getNewSampleValues();
        JrdsSample sample = p.newSample();
        p.injectSample(sample, vars);
        p.stopCollect();
        host.stopCollect();

        Assert.assertEquals(1.0, sample.get("a").doubleValue(), 1e-5);
        Assert.assertEquals(2.0, sample.get("b").doubleValue(), 1e-5);
        Assert.assertEquals(3.5, sample.get("e").doubleValue(), 1e-5);
        Assert.assertEquals(3, p.getUptime());
    }

}
