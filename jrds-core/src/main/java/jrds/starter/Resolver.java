package jrds.starter;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.event.Level;

import jrds.HostInfo;
import jrds.Probe;
import lombok.Data;
import lombok.Getter;

public class Resolver extends Starter {

    @Data
    public static final class ResolverKey {
        private final String hostname;
    }

    @Getter
    private final ResolverKey key;
    private InetAddress address = null;

    public Resolver(String hostname) {
        key = new ResolverKey(hostname);
        log(Level.DEBUG, "New dns resolver");
    }

    @Override
    public boolean start() {
        boolean started = false;
        try {
            address = InetAddress.getByName(key.hostname);
            started = true;
        } catch (UnknownHostException e) {
            log(Level.ERROR, e, "DNS host name %s can't be found", key.hostname);
        }
        return started;
    }

    @Override
    public void stop() {
        address = null;
    }

    public InetAddress getInetAddress() {
        return address;
    }

    public static Object makeKey(StarterNode node) {
        HostInfo host;
        if(node instanceof HostStarter)
            host = ((HostStarter) node).getHost();
        else if(node instanceof Probe<?, ?>) {
            Probe<?, ?> p = (Probe<?, ?>) node;
            host = p.getHost();
        } else {
            return null;
        }
        return new ResolverKey(host.getDnsName());
    }

    @Deprecated
    public static Object makeKey(String hostname) {
        return new ResolverKey(hostname);
    }

    public static Resolver register(StarterNode node, String name) {
        return node.registerStarter(
                Resolver.class, new Resolver.ResolverKey(name), () -> new Resolver(name)
        );
    }

}
