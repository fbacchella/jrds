package jrds.webapp;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.junit.Assert;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;

public class ToolsWebApp {
    static ServletTester getTestServer(Properties ctxt) {
        ServletTester tester = new ServletTester();
        tester.setContextPath("/");
        ServletContext sc =  tester.getContext().getServletContext();
        Configuration c = new Configuration(ctxt);
        sc.setAttribute(Configuration.class.getName(), c);

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
