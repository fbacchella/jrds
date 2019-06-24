package jrds.probe.jmx;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.jolokia.client.J4pClient;
import org.slf4j.event.Level;

import jrds.probe.HttpClientStarter;

public class JolokiaJmxConnection extends AbstractJmxConnection {

    private J4pClient j4pClient;
    private JolokiaJmxSource connection;

    public JolokiaJmxConnection() {
        super();
        path = "/jolokia/";
        port = -1;
    }

    @Override
    public JmxAbstractDataSource<?> getConnection() {
        return connection;
    }

    @Override
    public boolean startConnection() {
        HttpClientStarter httpstarter = getLevel().find(HttpClientStarter.class);
        if (!httpstarter.isStarted()) {
            return false;
        }
        try {
            String protocol = ssl ? "https" : "http";
            int resolvedport = port > 0 ? port : ssl ? 443 : 80;
            URL url = new URL(protocol, getHostName(), resolvedport, path);
            j4pClient = new J4pClient(url.toString(), httpstarter.getHttpClient());
            connection = new JolokiaJmxSource(j4pClient);
            return true;
        } catch (MalformedURLException e) {
            log(Level.ERROR, e, "can't build jolokia URL: %s", e.getMessage());
            return false;
        }
    }

    @Override
    public void stopConnection() {
        j4pClient = null;
        connection = null;
    }

    @Override
    public long setUptime() {
        try {
            ObjectName mbeanName = new ObjectName(startTimeObjectName);
            return connection.getValue(mbeanName, startTimeAttribue, new String[]{}).longValue();
        } catch (MalformedObjectNameException | InvocationTargetException e) {
            log(Level.ERROR, e, "Uptime error for %s: %s", this, e);
            return 0;
        }
    }

    @Override
    public <T> T getMBean(String name, Class<T> interfaceClass) {
        throw new UnsupportedOperationException("Can't get generic mbean class from jolokia");
    }

}
