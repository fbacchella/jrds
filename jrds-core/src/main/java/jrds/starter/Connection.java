package jrds.starter;

import java.io.IOException;
import java.net.Socket;
import java.util.Optional;

import org.slf4j.event.Level;

import jrds.Probe;
import lombok.Getter;
import lombok.Setter;

public abstract class Connection<ConnectedType> extends Starter {

    @Getter @Setter
    private String name;
    private long uptime;

    public abstract ConnectedType getConnection();

    Socket makeSocket(String host, int port) throws IOException {
        SocketFactory sf = getLevel().find(SocketFactory.class);
        return sf.getFactory().createSocket(host, port);
    }

    /*
     * (non-Javadoc)
     * 
     * @see jrds.Starter#getKey()
     */
    @Override
    public Object getKey() {
        return Optional.ofNullable(name).orElse(getClass().getName());
    }

    /**
     * Return the host name associated
     * 
     * @return
     */
    public String getHostName() {
        StarterNode level = getLevel();
        if(level instanceof HostStarter) {
            return ((HostStarter) level).getDnsName();
        }
        if(level instanceof Probe<?, ?>) {
            return ((Probe<?, ?>) level).getHost().getDnsName();
        }
        return null;
    }

    public Resolver getResolver() {
        Resolver r = getLevel().find(Resolver.class, Resolver.makeKey(getLevel()));
        if (r == null) {
            r = new Resolver(getHostName());
            getLevel().registerStarter(r);
        }
        return r;
    }

    /**
     * To get the default time out
     * 
     * @return the connection timeout in second
     */
    public int getTimeout() {
        return getLevel().getTimeout();
    }

    /*
     * (non-Javadoc)
     * 
     * @see jrds.Starter#start()
     */
    @Override
    public boolean start() {
        if (startConnection()) {
            uptime = setUptime();
            log(Level.DEBUG, "Uptime for %s = %ds", this, uptime);
            return super.start();
        } else {
            return false;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see jrds.Starter#stop()
     */
    @Override
    public void stop() {
        stopConnection();
    }

    public abstract boolean startConnection();

    public abstract void stopConnection();

    /**
     * Return the uptime of the end point of the connexion it's called once
     * after the connexion start It should be in seconds
     * 
     * @return
     */
    public abstract long setUptime();

    /**
     * @return the uptime
     */
    public long getUptime() {
        return uptime;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jrds.Starter#toString()
     */
    @Override
    public String toString() {
        return getKey() + "@" + getHostName();
    }

}
