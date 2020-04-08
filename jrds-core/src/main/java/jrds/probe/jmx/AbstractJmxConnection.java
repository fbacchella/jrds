package jrds.probe.jmx;

import javax.management.remote.JMXServiceURL;

import jrds.probe.JMXConnection;
import jrds.starter.Connection;
import jrds.starter.StarterNode;

public abstract class AbstractJmxConnection<CNX, DS extends JmxAbstractDataSource<CNX>> extends Connection<JmxAbstractDataSource<CNX>> {

    protected static final String startTimeObjectName = "java.lang:type=Runtime";
    protected static final String startTimeAttribue = "Uptime";

    protected JMXServiceURL url;
    protected JmxProtocol protocol;
    protected int port;
    protected String path;
    protected String user;
    protected String password;
    protected JMXConnection parent;
    protected boolean ssl;

    public abstract <T> T getMBean(String name, Class<T> interfaceClass);

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
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * @return the protocol
     */
    public JmxProtocol getProtocol() {
        return protocol;
    }

    /**
     * @param protocol the protocol to set
     */
    public void setProtocol(JmxProtocol protocol) {
        this.protocol = protocol;
    }

    /**
     * @return the url
     */
    public JMXServiceURL getUrl() {
        return url;
    }

    /**
     * @param url the url to set
     */
    public void setUrl(JMXServiceURL url) {
        this.url = url;
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

    public void setParent(JMXConnection parent) {
        this.parent = parent;
    }

    @Override
    public StarterNode getLevel() {
        return parent.getLevel();
    }

    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

}
