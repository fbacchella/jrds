package jrds;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import jrds.starter.ConnectionInfo;
import jrds.starter.HostStarter;
import jrds.starter.StarterNode;

public class HostInfo {
    static final private Logger logger = Logger.getLogger(HostInfo.class);

    private String name = null;
    private String dnsName = null;
    private Set<String> tags = null;
    private File hostdir = null;
    private boolean hidden = false;
    private final Map<String, ConnectionInfo> cnxList = new HashMap<String, ConnectionInfo>();
    private final Set<Probe<?,?>> probesList = new HashSet<Probe<?,?>>();

    public HostInfo(String name) {
        super();
        this.name = name;
    }

    public HostInfo(String name, String dnsName) {
        super();
        this.name = name;
        this.dnsName = dnsName;
    }

    /**
     * @param hostdir the hostdir to set
     */
    public void setHostDir(File hostdir) {
        this.hostdir = hostdir;
    }

    public File getHostDir() {
        return hostdir;
    }

    public void addTag(String tag) {
        if(tags == null)
            tags = new HashSet<String>();
        tags.add(tag);
    }

    public Set<String> getTags() {
        Set<String> temptags = tags;
        if(tags == null)
            temptags = new HashSet<String>();
        return temptags;
    }

    /**
     * @return the dnsName
     */
    public String getDnsName() {
        if(dnsName != null)
            return dnsName;
        else
            return name;
    }

    /**
     * @param dnsName the dnsName to set
     */
    public void setDnsName(String dnsName) {
        this.dnsName = dnsName;
    }

    /**
     * Is the host to be shown in the host list ?
     * @return the hidden
     */
    public boolean isHidden() {
        return hidden;
    }

    /**
     * @param hidden the hidden to set
     */
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    public void addConnection(ConnectionInfo cnx) {
        cnxList.put(cnx.getName(), cnx);
    }
    
    public Iterable<ConnectionInfo> getConnections() {
        return new Iterable<ConnectionInfo>() {
            @Override
            public Iterator<ConnectionInfo> iterator() {
                return cnxList.values().iterator();
            }
        };        
    }
    
    public ConnectionInfo getConnection(String name) {
        return cnxList.get(name);
    }
    
    public void addProbe(Probe<?,?> p) {
        probesList.add(p);
    }
    
    public Iterable<Probe<?,?>> getProbes() {
        return new Iterable<Probe<?,?>>() {
            @Override
            public Iterator<Probe<?,?>> iterator() {
                return probesList.iterator();
            }
        };
    }
    
    public int getNumProbes() {
        return probesList.size();
    }

    public HostStarter makeHost(StarterNode top) {
        HostStarter hs = new HostStarter(this);
        hs.setParent(top);
        for(ConnectionInfo cnx: cnxList.values()) {
            try {
                cnx.register(hs);
            } catch (InvocationTargetException e) {
                logger.error("Can instantiate connection " + cnx.getName());
            }
        }
        return hs;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return name;
    }
    
}
