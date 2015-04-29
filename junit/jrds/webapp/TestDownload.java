package jrds.webapp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;

import jrds.Configuration;
import jrds.GraphDesc;
import jrds.GraphNode;
import jrds.HostsList;
import jrds.Probe;
import jrds.ProbeDesc;
import jrds.PropertiesManager;
import jrds.TestProbe.DummyProbe;
import jrds.Tools;
import jrds.graphe.Sum;
import jrds.mockobjects.Full;
import jrds.mockobjects.GenerateProbe;
import jrds.mockobjects.MokeProbe;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.jetty.http.HttpTester.Response;
import org.eclipse.jetty.servlet.ServletTester;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rrd4j.data.DataProcessor;

public class TestDownload extends Download {
    static final private Logger logger = Logger.getLogger(TestDownload.class);

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @BeforeClass
    static public void configure() throws IOException, ParserConfigurationException {
        System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.Slf4jLog");
        System.setProperty("org.eclipse.jetty.LEVEL", "DEBUG");
        Tools.configure();
        Tools.prepareXml(false);
        Tools.setLevel(logger, Level.TRACE, "jrds.webapp.ParamsBean", "jrds.webapp.JSonTree", "jrds.GraphDesc", Download.class.getCanonicalName(), "jrds.graphe.Sum");
    }

    @Test
    public void testEpochFormat() throws Exception
    {
        Date start = epochFormat.get().parse("0");
        String formatted = epochFormat.get().format(start);

        Assert.assertEquals("0", formatted);
        Assert.assertEquals(new Date(0), start);

    }

    @Test
    public void testwriteCsv() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataProcessor dp =  new DataProcessor(1, 10000);
        dp.setStep(10000/2);
        dp.processData();
        writeCsv(out, dp, epochFormat.get() );
        logger.debug(out.toString());
    }

    @Test
    public void testDownload() throws Exception {
        ServletTester tester = null;
        tester = ToolsWebApp.getMonoServlet(testFolder, new Properties(), Download.class, "/download");

        tester.start();

        HostsList hl = Configuration.get().getHostsList();
        PropertiesManager pm = Configuration.get().getPropertiesManager();

        GenerateProbe.ChainedMap<Object> args = GenerateProbe.ChainedMap.start();
        args.set(ProbeDesc.class, Full.getPd()).set(Probe.class, DummyProbe.class).set(PropertiesManager.class, pm);

        @SuppressWarnings("unchecked")
        Probe<String, Number> p = (MokeProbe<String, Number>) GenerateProbe.quickProbe(testFolder, args);

        p.checkStore();

        GraphDesc gd = Full.getGd();
        gd.setGraphName("SumTest");
        gd.setName("SumTest");
        p.addGraph(gd);

        hl.addHost(p.getHost());
        hl.addProbe(p);

        ArrayList<String> glist = new ArrayList<String>();
        glist.add(p.getGraphList().iterator().next().getQualifiedName());
        Sum s = new Sum("A sum test", glist);
        s.configure(hl);
        Collection<GraphNode> c = new HashSet<GraphNode>(1);
        c.add(s);
        hl.addGraphs(c);

        int id = s.getQualifiedName().hashCode();

        Response response = ToolsWebApp.doRequestGet(tester, "http://localhost/download?id=" + id, 200);
        Assert.assertEquals("Invalid content type", Download.CONTENT_TYPE, response.get("Content-Type"));;

    }

}
