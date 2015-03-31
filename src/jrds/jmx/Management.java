package jrds.jmx;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import javax.servlet.ServletContext;

import jrds.Configuration;
import jrds.HostInfo;
import jrds.HostsList;
import jrds.Probe;
import jrds.PropertiesManager;
import jrds.webapp.StartListener;

public class Management extends StandardMBean implements ManagementMBean {
    static public final void register(File configfile) {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectName name = new ObjectName("jrds:type=Management");
            mbs.registerMBean(new Management(configfile), name);
        } catch (InstanceAlreadyExistsException e) {
            throw new RuntimeException("jrds mbean failed to register", e);
        } catch (MBeanRegistrationException e) {
            throw new RuntimeException("jrds mbean failed to register", e);
        } catch (NotCompliantMBeanException e) {
            throw new RuntimeException("jrds mbean failed to register", e);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException("jrds mbean failed to register", e);
        } catch (NullPointerException e) {
            throw new RuntimeException("jrds mbean failed to register", e);
        } 
    }

    static public final void register(ServletContext ctxt) {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectName name = new ObjectName("jrds:type=Management");
            mbs.registerMBean(new Management(ctxt), name);
        } catch (InstanceAlreadyExistsException e) {
            throw new RuntimeException("jrds mbean failed to register", e);
        } catch (MBeanRegistrationException e) {
            throw new RuntimeException("jrds mbean failed to register", e);
        } catch (NotCompliantMBeanException e) {
            throw new RuntimeException("jrds mbean failed to register", e);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException("jrds mbean failed to register", e);
        } catch (NullPointerException e) {
            throw new RuntimeException("jrds mbean failed to register", e);
        } 
    }

    static public final void unregister() {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name;
        try {
            name = new ObjectName("jrds:type=Management");
            mbs.unregisterMBean(name);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException("jrds mbean failed to unregister", e);
        } catch (MBeanRegistrationException e) {
            throw new RuntimeException("jrds mbean failed to unregister", e);
        } catch (InstanceNotFoundException e) {
            throw new RuntimeException("jrds mbean failed to unregister", e);
        }
    }

    private final File configfile;
    private final ServletContext ctxt;

    private Management(File configfile) {
        super(ManagementMBean.class, false);
        this.configfile = configfile;
        this.ctxt = null;
    }

    private Management(ServletContext ctxt) {
        super(ManagementMBean.class, false);
        this.configfile = null;
        this.ctxt = ctxt;
    }

    @Override
    public void reload() {
        PropertiesManager pm = new PropertiesManager();
        if(configfile!= null && configfile.isFile())
            pm.join(configfile);
        else if(ctxt != null) {
            StartListener sl = (StartListener) ctxt.getAttribute(StartListener.class.getName());
            Properties p = sl.readProperties(ctxt);
            pm.join(p);
        }
        pm.importSystemProps();
        Configuration.switchConf(pm);
    }

    @Override
    public int getHostsCount() {
        HostsList hl = Configuration.get().getHostsList();
        Collection<HostInfo> hosts = hl.getHosts();
        return hosts.size();
    }

    @Override
    public int getProbesCount() {
        Configuration c =  Configuration.get();
        HostsList hl = c.getHostsList();
        Collection<HostInfo> hosts = hl.getHosts();
        int numProbes = 0;
        for(HostInfo h: hosts) {
            numProbes += h.getNumProbes();
        }
        return numProbes;
    }

    @Override
    public int getGeneration() {
        return Configuration.get().getHostsList().getGeneration();
    }

    @Override
    public Map<String, Number> getLastValues(String host, String probeName) {
        Probe<?,?> p =  Configuration.get().getHostsList().getProbeByPath(host, probeName);
        return p.getLastValues();
    }

}
