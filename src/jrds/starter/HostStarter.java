package jrds.starter;

import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Level;

import jrds.HostInfo;
import jrds.Probe;

public class HostStarter extends StarterNode {
    private HostInfo host;
    private final Set<Probe<?,?>> allProbes = new TreeSet<Probe<?,?>>();

    public HostStarter(HostInfo host) {
        super();
        this.host = host;
    }
    
    public void addProbe(Probe<?,?> p){
        allProbes.add(p);
    }
    
    public void collectAll() {
        log(Level.DEBUG, "Starting collect for %s", this);
        long start = System.currentTimeMillis();
        startCollect();
        for(Probe<?,?> currrd: allProbes) {
            if(! isCollectRunning() )
                break;
            long duration = (System.currentTimeMillis() - start) / 1000 ;
            if(duration > (currrd.getStep() / 2 )) {
                log(Level.ERROR, "Collect too slow: %ds", duration);
                break;
            }
            currrd.collect();
        }
        stopCollect();
        long end = System.currentTimeMillis();
        float elapsed = (end - start)/1000f;
        log(Level.DEBUG, "Collect time for %s: %fs", host.getName(), elapsed);
    }

    public String toString() {
        return host.toString();
    }

    public int compareTo(HostStarter arg0) {
        return String.CASE_INSENSITIVE_ORDER.compare(host.getName(),arg0.getHost().getName() );
    }

    /**
     * @return the host
     */
    public HostInfo getHost() {
        return host;
    }

}
