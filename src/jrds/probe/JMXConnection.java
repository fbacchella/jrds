package jrds.probe;

import java.io.EOFException;
import java.io.IOException;
import java.lang.management.RuntimeMXBean;
import java.net.MalformedURLException;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.generic.GenericConnector;
import javax.management.remote.message.Message;

import org.apache.log4j.Level;

import com.sun.jmx.remote.socket.SocketConnection;

import jrds.JuliToLog4jHandler;
import jrds.PropertiesManager;
import jrds.factories.ProbeBean;
import jrds.starter.Connection;
import jrds.starter.SocketFactory;

@ProbeBean({"url", "protocol", "port", "path", "user", "password"})
public class JMXConnection extends Connection<MBeanServerConnection> {
    static {
        //If not already configured, we filter it
        JuliToLog4jHandler.catchLogger("javax.management", Level.FATAL);
    }

    private static enum PROTOCOL {
        rmi {
            @Override
            public JMXServiceURL getURL(JMXConnection cnx) throws MalformedURLException {
                return new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + cnx.getHostName() + ":" + cnx.port + cnx.path);
            }
        },
        iiop {
            @Override
            public JMXServiceURL getURL(JMXConnection cnx) throws MalformedURLException {
                return new JMXServiceURL("service:jmx:iiop:///jndi/iiop://" + cnx.getHostName() + ":" + cnx.port + cnx.path);
            }
        },
        jmxmp {
            @Override
            public JMXServiceURL getURL(JMXConnection cnx) throws MalformedURLException {
                return new JMXServiceURL("service:jmx:jmxmp://" + cnx.getHostName() + ":" + cnx.port);
            }

        };
        abstract public JMXServiceURL getURL(JMXConnection cnx)  throws MalformedURLException ;
    }

    final static String startTimeObjectName = "java.lang:type=Runtime";
    final static String startTimeAttribue = "Uptime";

    // close can be slow
    private final static AtomicInteger closed = new AtomicInteger();
    private final static ThreadFactory closerFactory = new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "Closer" + closed.getAndIncrement());
            t.setDaemon(true);
            return t;
        }      
    };
    private final static ExecutorService closer = new ThreadPoolExecutor(0, 4, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), closerFactory);

    private JMXServiceURL url = null;
    private PROTOCOL protocol  = PROTOCOL.rmi;
    private int port;
    private String path = "/jmxrmi";
    private String user = null;
    private String password = null;
    private JMXConnector connector;
    private MBeanServerConnection connection;

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
    public void configure(PropertiesManager pm) {
        super.configure(pm);
        if (url == null) {
            try {
                url = protocol.getURL(this);
            } catch (MalformedURLException e) {
                throw new RuntimeException(String.format("Invalid jmx URL %s: %s", protocol.toString()), e);
            }
        }
        // connector is always set, so close in Stop() always works 
        Map<String,?> dummy = Collections.emptyMap();
        try {
            connector = JMXConnectorFactory.newJMXConnector(url, dummy);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Invalid jmx URL %s: %s", protocol.toString()), e);
        }
    }

    @Override
    public MBeanServerConnection getConnection() {
        return connection;
    }

    /**
     * Resolve a mbean interface, given the interface and it's name
     * @param name
     * @param interfaceClass
     * @return
     */
    public <T> T getMBean(String name,  Class<T> interfaceClass) {
        MBeanServerConnection mbsc = getConnection();
        try {
            ObjectName mbeanName = new ObjectName(name);
            return javax.management.JMX.newMBeanProxy(mbsc, mbeanName, 
                    interfaceClass, true);        
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException("wrong mbean name: " + name, e);
        }
    }

    @Override
    public long setUptime() {
        try {
            RuntimeMXBean mxbean = getMBean(startTimeObjectName, RuntimeMXBean.class);
            if (mxbean != null)
                return mxbean.getUptime() /1000;
        } catch (Exception e) {
            log(Level.ERROR, e, "Uptime error for %s: %s", this, e);
        }
        return 0;
    }

    /* (non-Javadoc)
     * @see jrds.Starter#start()
     */
    @Override
    public boolean startConnection() {
        try {
            log(Level.TRACE, "connecting to %s", url);
            Map<String, Object> attributes = new HashMap<String, Object>();
            if(user != null && password != null ) {
                String[] credentials = new String[]{user, password};
                attributes.put("jmx.remote.credentials", credentials);
            }
            attributes.put("jmx.remote.x.request.timeout", getTimeout() * 1000);
            attributes.put("jmx.remote.x.server.side.connecting.timeout", getTimeout() * 1000);
            attributes.put("jmx.remote.x.client.connected.state.timeout", getTimeout() * 1000);
            if(protocol == PROTOCOL.rmi) {
                attributes.put("sun.rmi.transport.tcp.responseTimeout", getTimeout() * 1000);
            }
            else if(protocol == PROTOCOL.jmxmp) {
                SocketFactory sf = getLevel().find(SocketFactory.class); 
                if( ! sf.isStarted()) {
                    return false;
                }
                Socket s = sf.createSocket(url.getHost(), url.getPort());
                // A custom class is needed, to catch log messages
                attributes.put(GenericConnector.MESSAGE_CONNECTION, new SocketConnection(s) {

                    @SuppressWarnings("rawtypes")
                    @Override
                    public void connect(Map env) throws IOException {
                        try {
                            super.connect(env);
                        } catch (EOFException e) {
                            // don't log this one
                            throw e;
                        } catch (Exception e) {
                            log(Level.ERROR, e, "failed to read message: %s", e);
                            throw e;
                        }
                    }

                    @Override
                    public void writeMessage(Message msg) throws IOException {
                        try {
                            super.writeMessage(msg);
                        } catch (EOFException e) {
                            // don't log this one
                            throw e;
                        } catch (Exception e) {
                            log(Level.ERROR, e, "failed to read message: %s", e);
                            throw e;
                        }
                    }

                    @Override
                    public Message readMessage()
                            throws IOException, ClassNotFoundException {
                        try {
                            return super.readMessage();
                        } catch (EOFException e) {
                            // don't log this one
                            throw e;
                        } catch (Exception e) {
                            log(Level.ERROR, e, "failed to read message: %s", e);
                            throw e;
                        }
                    }

                });
            }
            // connect can hang in a read !
            // So separate creation from connection, and then it might be possible to do close
            // on a connecting probe
            connector = JMXConnectorFactory.newJMXConnector(url, attributes);
            connector.connect();
            connection = connector.getMBeanServerConnection();                
            return true;
        } catch (IOException e) {
            log(Level.ERROR, e, "Communication error with %s: %s", protocol.toString(), e);
        }
        return false;
    }

    /* (non-Javadoc)
     * @see jrds.Starter#stop()
     */
    @Override
    public void stopConnection() {
        // close can be slow, do it in a separate thread
        // but don' try to create a new one each time
        final JMXConnector current = connector;
        closer.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    current.close();
                } catch (IOException e) {
                    log(Level.ERROR, e, "JMXConnector to %s close failed because of: %s", this, e );
                }            
            }
        });
        connection = null;                
    }

    /* (non-Javadoc)
     * @see jrds.Starter#toString()
     */
    @Override
    public String toString() {
        if (url == null) {
            try {
                return protocol.getURL(this).toString();
            } catch (MalformedURLException e) {
                return "";
            }
        }
        else {
            return url.toString();
        }
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
        this.protocol = PROTOCOL.valueOf(protocol.trim().toLowerCase());
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

}
