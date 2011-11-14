package jrds;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import jrds.starter.Resolver;
import jrds.starter.Starter;
import jrds.starter.StarterNode;

import org.apache.log4j.Level;

/**
 * @author Fabrice Bacchella
 *
 */
public class RdsHost  extends StarterNode implements Comparable<RdsHost> {

    private String name = null;
    private String dnsName = null;
    private final Set<Probe<?,?>> allProbes = new TreeSet<Probe<?,?>>();
    private Set<String> tags = null;
    private File hostdir = null;
    private boolean hidden = false;

    public RdsHost(String name)
    {
        super();
        this.name = name;
        registerStarter(new Resolver(name));
    }

    public RdsHost(String name, String dnsName)
    {
        super();
        this.name = name;
        this.dnsName = dnsName;
        registerStarter(new Resolver(dnsName));
    }

    /**
     * 
     */
    public RdsHost() {
    }

    public void setName(String name) {
        this.name = name;
        Starter resolver = new Resolver(name);
        registerStarter(resolver);
    }

    public String getName() {
        return name;
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

    public Collection<Probe<?,?>> getProbes() {
        return allProbes;

    }

    public void  collectAll() {
        log(Level.DEBUG, "Starting collect for %s", this);
        long start = System.currentTimeMillis();
        startCollect();
        for(Probe<?,?> currrd: allProbes) {
            if(! isCollectRunning() )
                break;
            long duration = (System.currentTimeMillis() - start) / 1000 ;
            if(duration > (currrd.getStep() / 2 )) {
                log(Level.ERROR, "Collectan too long: %ds", duration);
                break;
            }
            currrd.collect();
        }
        stopCollect();
        long end = System.currentTimeMillis();
        float elapsed = (end - start)/1000f;
        log(Level.DEBUG, "Collect time for %s: %fs", name, elapsed);
    }

    public String toString() {
        return name;
    }

    public int compareTo(RdsHost arg0) {
        return String.CASE_INSENSITIVE_ORDER.compare(name, arg0.toString());
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
        registerStarter(new Resolver(dnsName));
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
}
