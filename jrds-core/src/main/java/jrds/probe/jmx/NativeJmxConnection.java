package jrds.probe.jmx;

import java.io.IOException;
import java.lang.management.RuntimeMXBean;
import java.net.MalformedURLException;
import java.rmi.ConnectIOException;
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
import javax.management.remote.generic.GenericConnector;
import javax.naming.CommunicationException;
import javax.naming.ServiceUnavailableException;

import org.slf4j.event.Level;

import jrds.PropertiesManager;
import jrds.factories.ProbeBean;
import jrds.jmx.JrdsSocketConnection;
import jrds.probe.JMXConnection;
import jrds.probe.JmxSocketFactory;
import jrds.starter.SocketFactory;

@ProbeBean({ "url", "protocol", "port", "path", "user", "password" })
public class NativeJmxConnection extends AbstractJmxConnection<MBeanServerConnection, NativeJmxSource> {

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

    protected JMXConnector connector;
    private NativeJmxSource connection;

    public NativeJmxConnection(JMXConnection parent) {
        super(parent);
        path = "/jmxrmi";
    }

    @Override
    public void configure(PropertiesManager pm) {
        super.configure(pm);
        if(url == null) {
            try {
                url = protocol.getURL(this);
            } catch (MalformedURLException e) {
                throw new RuntimeException(String.format("Invalid jmx URL %s: %s", protocol.toString(), e.getMessage()), e);
            }
        }
        // connector is always set, so close in Stop() always works
        Map<String, ?> dummy = Collections.emptyMap();
        try {
            connector = JMXConnectorFactory.newJMXConnector(url, dummy);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Invalid jmx URL %s: %s", protocol.toString(), e.getMessage()), e);
        }
    }

    @Override
    public NativeJmxSource getConnection() {
        return connection;
    }

    /**
     * Resolve a mbean interface, given the interface and it's name
     * 
     * @param name
     * @param interfaceClass
     * @return
     */
    public <T> T getMBean(String name, Class<T> interfaceClass) {
        MBeanServerConnection mbsc = getConnection().connection;
        try {
            ObjectName mbeanName = new ObjectName(name);
            return javax.management.JMX.newMBeanProxy(mbsc, mbeanName, interfaceClass, true);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException("wrong mbean name: " + name, e);
        }
    }

    @Override
    public long setUptime() {
        try {
            MBeanServerConnection mbsc = getConnection().connection;
            RuntimeMXBean mxbean = javax.management.JMX.newMBeanProxy(mbsc, startTimeRequestsParams.mbeanName, RuntimeMXBean.class, true);
            if(mxbean != null) {
                return mxbean.getUptime() / 1000;
            }
        } catch (Exception e) {
            log(Level.ERROR, e, "Uptime error for %s: %s", this, e);
        }
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jrds.Starter#start()
     */
    @Override
    public boolean startConnection() {
        try {
            log(Level.TRACE, "connecting to %s", url);
            Map<String, Object> attributes = new HashMap<String, Object>();
            if (user != null && password != null) {
                String[] credentials = new String[] { user, password };
                attributes.put("jmx.remote.credentials", credentials);
            }
            attributes.put("jmx.remote.x.request.timeout", getTimeout() * 1000);
            attributes.put("jmx.remote.x.server.side.connecting.timeout", getTimeout() * 1000);
            attributes.put("jmx.remote.x.client.connected.state.timeout", getTimeout() * 1000);
            if (protocol == JmxProtocol.rmi) {
                attributes.put("sun.rmi.transport.tcp.responseTimeout", getTimeout() * 1000);
                attributes.put("com.sun.jndi.rmi.factory.socket", getLevel().find(JmxSocketFactory.class).getFactory());
            } else if (protocol == JmxProtocol.jmxmp) {
                Object sc = JrdsSocketConnection.create(url, getLevel().find(SocketFactory.class));
                attributes.put(GenericConnector.MESSAGE_CONNECTION, sc);
            }
            // connect can hang in a read !
            // So separate creation from connection, and then it might be
            // possible to do close
            // on a connecting probe
            connector = JMXConnectorFactory.newJMXConnector(url, attributes);
            connector.connect();
            connection = new NativeJmxSource(connector.getMBeanServerConnection());
            return true;
        } catch (IOException e) {
            // The exception needs to be cleaned, the stack is too depth
            Throwable cause = e;
            while (cause.getCause() instanceof IOException
                            || cause.getCause() instanceof ConnectIOException 
                            || cause.getCause() instanceof CommunicationException
                            || cause.getCause() instanceof ServiceUnavailableException) {
                cause = cause.getCause();
            }
            log(Level.ERROR, e, "Communication error with %s: %s", url, cause);
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jrds.Starter#stop()
     */
    @Override
    public void stopConnection() {
        // close can be slow, do it in a separate thread
        // but don' try to create a new one each time
        closer.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    connector.close();
                } catch (IOException e) {
                    log(Level.ERROR, e, "JMXConnector to %s close failed because of: %s", this, e);
                }
            }
        });
        connection = null;
    }

    /*
     * (non-Javadoc)
     * 
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
        } else {
            return url.toString();
        }
    }

}
