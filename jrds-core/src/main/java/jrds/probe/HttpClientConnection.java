package jrds.probe;

import java.net.URL;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;

import jrds.PropertiesManager;
import jrds.factories.ProbeBean;
import jrds.starter.Connection;
import lombok.Getter;
import lombok.Setter;

@ProbeBean({ "port", "file", "url", "urlhost", "scheme", "login", "password" })
public class HttpClientConnection extends Connection<HttpClient> {

    @Getter @Setter
    private URL url = null;
    @Getter @Setter
    private String urlhost = null;
    @Getter @Setter
    private int port = -1;
    @Getter @Setter
    private String file = "/";
    @Getter @Setter
    private String scheme = null;
    @Getter @Setter
    private String login = null;
    @Getter @Setter
    private String password = null;

    private final HttpClientStarter starter = new HttpClientStarter();
    private final HttpClientContext context = HttpClientContext.create();

    @Override
    public void configure(PropertiesManager pm) {
        starter.initialize(getLevel());
        starter.configure(pm);
        super.configure(pm);
        if (login != null && password != null) {
            CredentialsProvider bcp = new BasicCredentialsProvider();
            bcp.setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                               new UsernamePasswordCredentials(login, password));
            context.setCredentialsProvider(bcp);
        }
    }

    @Override
    public HttpClient getConnection() {
        return starter.getHttpClient();
    }

    public HttpClientContext getClientContext() {
        return context;
    }

    @Override
    public boolean startConnection() {
        return starter.start();
    }

    @Override
    public void stopConnection() {
        starter.stop();
    }

    @Override
    public long setUptime() {
        return 0;
    }

    public HttpClient getHttpClient() {
        return starter.getHttpClient();
    }

    public HttpHost getHttpHost() {
        return new HttpHost(getHostName(), port, scheme);
    }

}
