package jrds.starter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import jrds.HostsList;
import jrds.PropertiesManager;
import lombok.Getter;
import lombok.Setter;

@SuppressWarnings("deprecation")
public abstract class StarterNode implements StartersSet {

    private Map<Object, Starter> allStarters = null;

    private HostsList root = null;
    private volatile boolean started = false;
    private StarterNode parent = null;

    /**
     * The time out during collect
     * 
     * @param timeout Timeout in seconds.
     * @return The timeout in seconds.
     */
    @Getter @Setter
    private int timeout = -1;

    /**
     * The interval between collect.
     * 
     * @param step Interval in seconds.
     * @return The collect interval in seconds.
     */
    @Getter @Setter
    private int step = -1;

    /**
     * The collect time that will generate a warning in logs
     * 
     * @param slowCollectTime The slow collect time.
     * @return The slow collect time.
     */
    @Getter @Setter
    private int slowCollectTime = -1;

    public StarterNode() {
        if(this instanceof HostsList) {
            root = (HostsList) this;
        }
    }

    public StarterNode(StarterNode parent) {
        setParent(parent);
    }

    public void setParent(StarterNode parent) {
        root = parent.root;
        this.parent = parent;
        this.timeout = this.timeout < 0 ? parent.getTimeout() : this.timeout;
        this.step = this.step < 0 ? parent.getStep() : this.step;
        this.slowCollectTime = this.slowCollectTime < 0 ? parent.getSlowCollectTime() : this.slowCollectTime;
    }

    public boolean isCollectRunning() {
        if (started) {
            if (Thread.interrupted()) {
                started = false;
                log(Level.TRACE, "Thread is stopped", this);
            } else if (parent != null && !parent.isCollectRunning()) {
                started = false;
            } 
        }
        return started;
    }

    public boolean startCollect() {
        if(parent != null && !parent.isCollectRunning()) {
            log(Level.TRACE, "parent preventing start", this);
            return false;
        }
        if(allStarters != null) {
            log(Level.DEBUG, "Starting %d starters", allStarters.size());
            for(Starter s: allStarters.values()) {
                // If collect is stopped while we're starting, drop it
                if(parent != null && !parent.isCollectRunning())
                    return false;
                try {
                    s.doStart();
                } catch (Exception e) {
                    log(Level.ERROR, e, "starting %s failed: %s", s, e);
                }
            }
        }
        started = true;
        log(Level.DEBUG, "Starting done");
        return isCollectRunning();
    }

    public synchronized void stopCollect() {
        started = false;
        getChildsStream().forEach(StarterNode::stopCollect);
        Optional.ofNullable(allStarters).orElse(Collections.emptyMap()).values().stream()
        .forEach(s -> {
            try {
                s.doStop();
            } catch (Exception e) {
                log(Level.ERROR, e, "Unable to stop timer %s: %s", s.getKey(), e);
            }
        });
    }

    /**
     * @param s the starter to register
     * @return the starter that will be used
     */
    public <S extends Starter> Starter registerStarter(S s) {
        Object key = s.getKey();
        @SuppressWarnings("unchecked")
        S parentStarter = (S) find(s.getClass(), key);
        if (parentStarter != null) {
            return parentStarter;
        } else {
            if (allStarters == null) {
                // Must be a linked hashed map, order of insertion might be
                // important
                allStarters = new LinkedHashMap<Object, Starter>(2);
            }
            if (! allStarters.containsKey(key) ) {
                // Attention, Starter.initialize can add Starters, don't call it inside the map
                s.initialize(this);
                allStarters.put(key, s);
                log(Level.DEBUG, "registering %s with key %s", s.getClass().getName(), key);
                return s;
            } else {
                return allStarters.get(key);
            }
        }
    }

    /**
     * Called in the host list configuration, used to finished the configuration
     * of the starters
     * 
     * @param pm the configuration
     */
    public void configureStarters(PropertiesManager pm) {
        if(allStarters == null)
            return;

        // needed because started can failed (and be removed) or add other
        // starters
        List<Map.Entry<Object, Starter>> buffer = new ArrayList<>(allStarters.entrySet());
        for(Map.Entry<Object, Starter> me: buffer) {
            try {
                me.getValue().configure(pm);
            } catch (Exception e) {
                allStarters.remove(me.getKey());
                log(Level.ERROR, e, "Starter %s failed to configure: %s", me.getValue(), e);
            }
        }
    }

    public <StarterClass extends Starter> StarterClass find(Class<StarterClass> sc) {
        Object key;
        try {
            Method m = sc.getMethod("makeKey", StarterNode.class);
            key = m.invoke(null, this);
        } catch (NoSuchMethodException e) {
            // Not an error, the key is the the class
            key = sc.getName();
        } catch (Exception e) {
            log(Level.ERROR, e, "Error for %s with %s: %s", this, sc, e);
            return null;
        }
        return find(sc, key);
    }

    @SuppressWarnings("unchecked")
    public <StarterClass extends Starter> StarterClass find(String key) {
        return (StarterClass) find(Starter.class, key);
    }

    /*
     * (non-Javadoc)
     * 
     * @see jrds.starter.StartersSet#find(java.lang.Object)
     */
    @Deprecated
    public Starter find(Object key) {
        return find(Starter.class, key);
    }

    @SuppressWarnings("unchecked")
    public <SC extends Starter> SC find(Class<SC> sc, Object key) {
        SC s = null;
        if(allStarters != null)
            log(Level.TRACE, "Looking for starter %s with key %s in %s", sc, key, allStarters);
        if(allStarters != null && allStarters.containsKey(key)) {
            Starter stemp = allStarters.get(key);
            if(sc.isInstance(stemp)) {
                s = (SC) stemp;
            } else {
                log(Level.ERROR, "Starter key error, got a %s expecting a %s", stemp.getClass(), sc);
                return null;
            }
        } else if(parent != null)
            s = parent.find(sc, key);
        else
            log(Level.DEBUG, "Starter class %s not found for key %s", sc.getName(), key);
        return s;
    }

    public boolean isStarted(Object key) {
        boolean s = false;
        Starter st = find(Starter.class, key);
        if(st != null)
            s = st.isStarted();
        return s;
    }

    public HostsList getHostList() {
        if(root == null && getParent() != null)
            root = getParent().getHostList();
        return root;
    }

    /**
     * @return the parent
     */
    public StarterNode getParent() {
        return parent;
    }

    public<C extends StarterNode> Stream<C> getChildsStream() {
        return Stream.empty();
    }

    // Compatibily code
    /**
     * @deprecated Useless method, it return <code>this</code>
     * @return
     */
    @Deprecated
    public StartersSet getStarters() {
        return this;
    }

    /**
     * @deprecated Useless method, it return <code>this</code>
     * @return
     */
    @Deprecated
    public StarterNode getLevel() {
        return this;
    }

    @Deprecated
    public void setParent(StartersSet s) {
        setParent((StarterNode) s);
    }

    /*
     * (non-Javadoc)
     * 
     * @see jrds.starter.StartersSet#registerStarter(jrds.starter.Starter,
     * jrds.starter.StarterNode)
     */
    @Deprecated
    public Starter registerStarter(Starter s, StarterNode parent) {
        return registerStarter(s);
    }

    /*
     * (non-Javadoc)
     * 
     * @see jrds.starter.StartersSet#find(java.lang.Class,
     * jrds.starter.StarterNode)
     */
    @Deprecated
    public <StarterClass extends Starter> StarterClass find(Class<StarterClass> sc, StarterNode nope) {
        return find(sc);
    }

    public void log(Level l, Throwable e, String format, Object... elements) {
        jrds.Util.log(this, LoggerFactory.getLogger(getClass()), l, e, format, elements);
    }

    public void log(Level l, String format, Object... elements) {
        jrds.Util.log(this, LoggerFactory.getLogger(getClass()), l, null, format, elements);
    }

}
