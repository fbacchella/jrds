package jrds.starter;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import jrds.PropertiesManager;

public abstract class Starter {
    private StarterNode level;
    private final Logger namedLogger;

    private long uptime = Long.MAX_VALUE;
    volatile private boolean started = false;

    public Starter() {
        String[] classElements = getClass().getName().split("\\.");
        namedLogger = LoggerFactory.getLogger("jrds.Starter." + classElements[classElements.length - 1].replace("$", ".$"));
    }

    /**
     * This method is called when the started is really registred
     * <p>
     * It can be overriden to contains delayed initialization but it must begin
     * with a call to super.initialize(parent)
     * 
     * @param level
     */
    public void initialize(StarterNode level) {
        this.level = level;
    }

    /**
     * It's called after the starter registration but in host list configuration
     * A starter can uses it to tweaks it's configuration It can be overriden to
     * contains delayed initialization but it must begin with a call to
     * super.configuration(pm)
     * 
     * @param pm
     */
    public void configure(PropertiesManager pm) {
        log(Level.DEBUG, "registred to %s", getLevel());
    }

    public final void doStart() {
        log(Level.TRACE, "Starting");
        try {
            long begin = new Date().getTime();
            started = start();
            long end = new Date().getTime();
            log(Level.DEBUG, "Starting connection took %d ms", end - begin);
        } catch (Exception e) {
            log(Level.ERROR, e, "Error while starting: %s", e);
        } catch (NoClassDefFoundError e) {
            log(Level.ERROR, e, e.getMessage().replace('/', '.'));
        }
        if(!started) {
            log(Level.ERROR, "starting failed");
        }
    }

    public final void doStop() {
        log(Level.DEBUG, "Stopping");
        if(started) {
            try {
                stop();
            } catch (Exception e) {
                log(Level.ERROR, e, "Unmannaged error while stopping: %s", e);
            }
            started = false;
        }
    }

    public boolean start() {
        return true;
    }

    public void stop() {
    }

    public Object getKey() {
        return getClass().getName();
    }

    public StarterNode getLevel() {
        return level;
    }

    public boolean isStarted() {
        return started;
    }

    @Override
    public String toString() {
        String levelString = "''";
        String keyString;
        if(level != null)
            levelString = level.toString();
        Object key = getKey();
        if(key instanceof Class<?>) {
            keyString = ((Class<?>) key).getName();
        } else {
            keyString = key.toString();
        }
        return keyString + "@" + levelString;
    }

    /**
     * Return uptime for the starter, default value is max value
     * 
     * @return
     */
    public long getUptime() {
        return uptime;
    }

    public void setUptime(long uptime) {
        this.uptime = uptime;
    }

    /**
     * @deprecated Use getLevel instead.
     * @return the StarterNode for this starter
     */
    @Deprecated
    public StartersSet getParent() {
        return level;
    }

    public void log(Level l, Throwable e, String format, Object... elements) {
        jrds.Util.log(this, namedLogger, l, e, format, elements);
    }

    public void log(Level l, String format, Object... elements) {
        jrds.Util.log(this, namedLogger, l, null, format, elements);
    }

}
