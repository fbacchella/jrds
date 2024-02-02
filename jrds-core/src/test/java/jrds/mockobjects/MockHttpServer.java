package jrds.mockobjects;

import java.io.Closeable;
import java.net.InetAddress;
import java.net.URL;

import jakarta.servlet.http.HttpServlet ;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class MockHttpServer extends Server implements Closeable {

    private ServletContextHandler ctx = null;
    private final HandlerCollection handlers = new HandlerList();

    public MockHttpServer(boolean withSSL) {
        super();
        ServerConnector connector;
        if(withSSL) {
            HttpConfiguration https = new HttpConfiguration();
            https.addCustomizer(new SecureRequestCustomizer());
            SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
            URL resource = getClass().getClassLoader().getResource("localhost.jks");
            if(resource == null) {
                throw new RuntimeException("Unable to find 'localhost.jks' file to setup SSL connector");
            }
            sslContextFactory.setKeyStorePath(resource.toExternalForm());
            sslContextFactory.setKeyStorePassword("123456");
            sslContextFactory.setKeyManagerPassword("123456");
            connector = new ServerConnector(this, new SslConnectionFactory(sslContextFactory, "http/1.1"), new HttpConnectionFactory(https));
        } else {
            connector = new ServerConnector(this);
        }
        connector.setHost(InetAddress.getLoopbackAddress().getHostName());
        connector.setPort(0);
        setConnectors(new Connector[] { connector });
        setHandler(handlers);
    }

    public MockHttpServer addServlet(HttpServlet servlet, String path) {
        if(ctx == null) {
            ctx = new ServletContextHandler(handlers, "/", ServletContextHandler.NO_SESSIONS);
        }
        ctx.addServlet(new ServletHolder(servlet), path);
        return this;
    }

    public MockHttpServer addResourceHandler(ResourceHandler handler) {
        handlers.addHandler(handler);
        return this;

    }

    @Override
    public void close() {
        try {
            stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
