package jrds.probe.jmx;

import java.net.MalformedURLException;

import javax.management.remote.JMXServiceURL;

public enum JmxProtocol {

    jolokia {
        @Override
        public JMXServiceURL getURL(NativeJmxConnection cnx) throws MalformedURLException {
            return new JMXServiceURL("jolokia", cnx.getHostName(), cnx.port, cnx.path);
        }
    },
    rmi {
        @Override
        public JMXServiceURL getURL(NativeJmxConnection cnx) throws MalformedURLException {
            return new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + cnx.getHostName() + ":" + cnx.port + cnx.path);
        }
    },
    iiop {
        @Override
        public JMXServiceURL getURL(NativeJmxConnection cnx) throws MalformedURLException {
            return new JMXServiceURL("service:jmx:iiop:///jndi/iiop://" + cnx.getHostName() + ":" + cnx.port + cnx.path);
        }
    },
    jmxmp {
        @Override
        public JMXServiceURL getURL(NativeJmxConnection cnx) throws MalformedURLException {
            return new JMXServiceURL("jmxmp", cnx.getHostName(), cnx.port);
        }
    };
    abstract public JMXServiceURL getURL(NativeJmxConnection cnx) throws MalformedURLException;

}
