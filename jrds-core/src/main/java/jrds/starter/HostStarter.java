package jrds.starter;

import java.io.File;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import jrds.HostInfo;
import jrds.Probe;

public class HostStarter extends StarterNode {

    static final private Logger logger = LoggerFactory.getLogger(HostStarter.class);

    private HostInfo host;
    private final Set<Probe<?, ?>> allProbes = new TreeSet<Probe<?, ?>>();

    private String runningname;

    public HostStarter(HostInfo host) {
        super();
        this.host = host;
        this.runningname = host.getName() + ":notrunning";
        registerStarter(new Resolver(host.getDnsName()));
    }

    public void addProbe(Probe<?, ?> p) {
        host.addProbe(p);
        allProbes.add(p);
    }

    public Iterable<Probe<?, ?>> getAllProbes() {
        return allProbes;
    }

    public void collectAll() {
        log(Level.DEBUG, "Starting collect");
        Timer timer = (Timer) getParent();
        startCollect();
        String oldThreadName = Thread.currentThread().getName();
        long start = System.currentTimeMillis();
        for(Probe<?, ?> probe: allProbes) {
            if(!isCollectRunning())
                break;
            log(Level.TRACE, "Starting collect of probe %s", probe);
            setRunningname(oldThreadName + "/" + probe.getName());
            probe.collect();
            long duration = Math.floorDiv(System.currentTimeMillis() - start, 1000L);
            if (duration > (probe.getStep() / 2)) {
                log(Level.ERROR, "Collect too slow: %ds for time %s", duration, timer.getName());
                break;
            }
            setRunningname(oldThreadName + ":finished");
        }
        stopCollect();
        long end = System.currentTimeMillis();
        float elapsed = (end - start) / 1000f;
        log(Level.DEBUG, "Collect time: %fs", elapsed);
    }

    public String toString() {
        return host.toString();
    }

    public int compareTo(HostStarter arg0) {
        return String.CASE_INSENSITIVE_ORDER.compare(host.getName(), arg0.getHost().getName());
    }

    /**
     * @return the host
     */
    public HostInfo getHost() {
        return host;
    }

    public File getHostDir() {
        return host.getHostDir();
    }

    public String getName() {
        return host.getName();
    }

    public Set<String> getTags() {
        return host.getTags();
    }

    public String getDnsName() {
        return host.getDnsName();
    }

    public boolean isHidden() {
        return host.isHidden();
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + super.hashCode();
        result = prime * result + (getParent() == null ? 0 : getParent().hashCode());
        return result;
    }

    /**
     * Two hosts are equals if they are equals and their parents too
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof HostStarter)) {
            return false;
        }
        HostStarter other = (HostStarter) obj;
        boolean parentEquals;
        if (getParent() != null) {
            parentEquals = getParent().equals(other.getParent());
        } else {
            parentEquals = other.getParent() == null;
        }
        return host.equals(other.getHost()) && parentEquals;
    }

    public String getRunningname() {
        return runningname;
    }

    public void setRunningname(String runningname) {
        Thread.currentThread().setName(runningname);
        this.runningname = runningname;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Stream<Probe<?, ?>> getChildsStream() {
        return allProbes.stream();
    }

    @Override
    public Logger getInstanceLogger() {
        return logger;
    }
 
}
