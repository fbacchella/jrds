/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.webapp.WebAppContext;

/**
 *
 * @author bbonfils
 */
public class Jetty {

    public static void runJetty(String webDirectory)
            throws Exception {
        String jetty_default = new java.io.File("./start.jar").exists() ? "." : "../..";
        String jetty_home = System.getProperty("jetty.home", jetty_default);

        Server server = new Server();

        Connector connector = new SelectChannelConnector();
        connector.setPort(Integer.getInteger("jetty.port", 8080).intValue());
        server.setConnectors(new Connector[]{connector});

        WebAppContext webapp = new WebAppContext();
        webapp.setContextPath("/");
        webapp.setWar(webDirectory);
        webapp.setDefaultsDescriptor(webDirectory  + "/webdefault.xml");

        server.setHandler(webapp);

        server.start();
        server.join();
    }
}
