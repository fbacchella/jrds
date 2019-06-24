package jrds.probe;

import java.net.MalformedURLException;

import javax.management.remote.JMXServiceURL;

import jrds.PropertiesManager;
import jrds.factories.ProbeBean;
import jrds.probe.jmx.AbstractJmxConnection;
import jrds.probe.jmx.JmxAbstractDataSource;
import jrds.probe.jmx.JmxProtocol;
import jrds.probe.jmx.JolokiaJmxConnection;
import jrds.probe.jmx.NativeJmxConnection;
import jrds.starter.Connection;

@ProbeBean({ "url", "protocol", "port", "path", "user", "password", "ssl" })
public class JMXConnection extends Connection<JmxAbstractDataSource<?>> {

    private JMXServiceURL url = null;
    private JmxProtocol protocol = JmxProtocol.rmi;
    private Integer port = null;
    private String path = null;
    private String user = null;
    private String password = null;
    private boolean ssl = false;
    private AbstractJmxConnection cnx;

    public JMXConnection() {
        super();
    }

    public JMXConnection(Integer port) {
        super();
        this.port = port;
    }

    public JMXConnection(Integer port, String user, String password) {
        super();
        this.port = port;
        this.user = user;
        this.password = password;
    }

    @Override
    public JmxAbstractDataSource<?> getConnection() {
        return cnx.getConnection();
    }

    @Override
    public boolean startConnection() {
        return cnx.startConnection();
    }

    @Override
    public void stopConnection() {
        cnx.stopConnection();
    }

    @Override
    public long setUptime() {
        return cnx.setUptime();
    }

    @Override
    public void configure(PropertiesManager pm) {
        switch(protocol) {
        case jolokia:
            cnx = new JolokiaJmxConnection();
            break;
        default:
            cnx = new NativeJmxConnection();
            break;
        }
        cnx.setParent(this);
        cnx.setProtocol(protocol);
        if (port != null) {
            cnx.setPort(port);
        }
        if (path != null) {
            cnx.setPath(path);
        }
        if (url != null) {
            cnx.setUrl(url);
        }
        cnx.setSsl(ssl);
        cnx.setUser(user);
        cnx.setPassword(password);
        cnx.configure(pm);
        super.configure(pm);
    }

    /**
     * @return the port
     */
    public Integer getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(Integer port) {
        this.port = port;
    }

    /**
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * @param path the path to set
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return the protocol
     */
    public String getProtocol() {
        return protocol.name();
    }

    /**
     * @param protocol the protocol to set
     */
    public void setProtocol(String protocol) {
        this.protocol = JmxProtocol.valueOf(protocol.trim().toLowerCase());
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return url.toString();
    }

    /**
     * @param url the url to set
     * @throws MalformedURLException 
     */
    public void setUrl(String url) throws MalformedURLException {
        this.url = new JMXServiceURL(url);
    }

    /**
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     * @param user the user to set
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * @return the ssl
     */
    public boolean isSsl() {
        return ssl;
    }

    /**
     * @param ssl the ssl to set
     */
    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    public String toString() {
        if (cnx != null) {
            return cnx.toString();
        } else {
            return super.toString();
        }
    }

    /**
     * Resolve a mbean interface, given the interface and it's name.
     * Kept for compatibility with jrdsagent
     * 
     * @param name
     * @param interfaceClass
     * @return
     */
    public <T> T getMBean(String name, Class<T> interfaceClass) {
        return cnx.getMBean(name, interfaceClass);
    }

}
