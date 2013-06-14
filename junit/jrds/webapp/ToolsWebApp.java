package jrds.webapp;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import javax.servlet.http.HttpServlet;

import jrds.Configuration;
import jrds.HostInfo;
import jrds.Probe;
import jrds.mockobjects.MokeProbe;
import jrds.starter.HostStarter;

import org.junit.Assert;
import org.junit.rules.TemporaryFolder;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;

public class ToolsWebApp {
    static ServletTester getTestServer(Properties ctxt) {
        ServletTester tester = new ServletTester();
        tester.setContextPath("/");
        Configuration.configure(ctxt);

        return tester;
    }

    static ServletTester getMonoServlet(TemporaryFolder testFolder, Properties props, Class< ? extends HttpServlet> sclass, String path) throws IOException {
        String root = testFolder.getRoot().getCanonicalPath();
        Properties config = new Properties();
        config.put("tmpdir", root);
        config.put("configdir", root + "/config");
        config.put("autocreate", "true");
        config.put("rrddir", root);
        config.put("libspath", "build/probes");
        config.putAll(props);

        ServletTester tester = ToolsWebApp.getTestServer(config);

        Configuration c = Configuration.get();
        HostStarter h = new HostStarter(new HostInfo("localhost"));
        Probe<?,?> p = new MokeProbe<String, Number>();
        p.setHost(h);
        h.addProbe(p);
        c.getHostsList().addHost(h.getHost());
        c.getHostsList().addProbe(p);
        tester.addServlet(sclass, path);

        return tester;
    }

    static HttpTester doRequestGet(ServletTester tester, String query, int expectedStatus) throws IOException, Exception {
        URL queryURL = new URL(query);
        HttpTester request = new HttpTester();
        HttpTester response = new HttpTester();
        request.setMethod("GET");
        request.setHeader("Host", queryURL.getHost());
        String args = queryURL.getQuery();
        request.setURI(queryURL.getPath()  + (args != null ? "?" + args : ""));
        request.setVersion("HTTP/1.0");
        response.parse(tester.getResponses(request.generate()));

        Assert.assertEquals(expectedStatus,response.getStatus());

        return response;
    }

}
