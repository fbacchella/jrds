package jrds.probe;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;

import jrds.PropertiesManager;
import jrds.Util;
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
    private Integer port = null;
    @Getter @Setter
    private String file = null;
    @Getter @Setter
    private String scheme = null;
    @Getter @Setter
    private String login = null;
    @Getter @Setter
    private String password = null;

    private final HttpClientContext context = HttpClientContext.create();

    @Override
    public void configure(PropertiesManager pm) {
        super.configure(pm);
        if (login != null && password != null) {
            CredentialsProvider bcp = new BasicCredentialsProvider();
            bcp.setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                               new UsernamePasswordCredentials(login, password));
            context.setCredentialsProvider(bcp);
        }
    }

    public URL resolve(HttpClientStarter.UrlBuilder urlbuilder, HttpProbe<?> p, List<Object> args) throws MalformedURLException {
        Optional.ofNullable(url).ifPresent(urlbuilder::setUrl);
        Optional.ofNullable(scheme).ifPresent(s -> urlbuilder.setScheme(Util.parseTemplate(scheme, p, args)));
        Optional.ofNullable(urlhost).ifPresent(s -> urlbuilder.setUrlhost(Util.parseTemplate(urlhost, p, args)));
        Optional.ofNullable(port).ifPresent(urlbuilder::setPort);
        Optional.ofNullable(file).ifPresent(s -> urlbuilder.setFile(Util.parseTemplate(file, p, args)));
        Optional.ofNullable(p).ifPresent(s -> urlbuilder.setProbe(p));
        return urlbuilder.build();
    }

    @Override
    public HttpClient getConnection() {
        return getHttpClient();
    }

    public HttpClientContext getClientContext() {
        return context;
    }

    @Override
    public long setUptime() {
        return Long.MAX_VALUE;
    }

    public HttpClient getHttpClient() {
        return getLevel().find(HttpClientStarter.class).getHttpClient();
    }

    @Override
    public boolean startConnection() {
        return true;
    }

    @Override
    public void stopConnection() {
    }

}
