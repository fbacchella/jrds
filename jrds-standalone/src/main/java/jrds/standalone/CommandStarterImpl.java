package jrds.standalone;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Properties;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import jrds.PropertiesManager;
import jrds.Util;
import jrds.bootstrap.CommandStarter;

public abstract class CommandStarterImpl implements CommandStarter {

    public void help() {
        System.out.println("Unimplemented help");
    }

    public void configure(Properties configuration) {
    }

    public abstract void start(String[] args) throws Exception;

    public void doJmx(PropertiesManager pm) {
        if(!pm.withjmx)
            return;

        String protocol = pm.jmxprops.remove("protocol");
        int port = Util.parseStringNumber(pm.jmxprops.remove("port"), 0);

        try {
            MBeanServer mbs;
            JMXServiceURL url;
            JMXConnectorServer cs;

            String path = "/";
            if(protocol.equals("rmi")) {
                java.rmi.registry.LocateRegistry.createRegistry(port);
                path = "/jndi/rmi://" + "0.0.0.0" + ":" + port + "/jmxrmi";
            }
            url = new JMXServiceURL(protocol, "0.0.0.0", port, path);
            mbs = ManagementFactory.getPlatformMBeanServer();
            cs = JMXConnectorServerFactory.newJMXConnectorServer(url, pm.jmxprops, mbs);
            cs.start();
            JMXServiceURL addr = cs.getAddress();
            JMXConnectorFactory.connect(addr);

            // Register the JMXConnectorServer in the MBeanServer
            ObjectName cntorServerName = ObjectName.getInstance("connectors:protocol=" + protocol);
            mbs.registerMBean(cs, cntorServerName);

        } catch (IOException | MalformedObjectNameException | 
                NullPointerException | InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException e) {
            throw new RuntimeException("jmx remote access failed to configure", e);
        }

    }
}
