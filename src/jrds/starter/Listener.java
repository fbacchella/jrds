package jrds.starter;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import jrds.probe.PassiveProbe;

import org.apache.log4j.Level;

public abstract class Listener<Message, KeyType> extends Starter {

    Thread listenerThread = null;
    private final Map<String, Map<String,PassiveProbe<KeyType>>> probes = new HashMap<String, Map<String,PassiveProbe<KeyType>>>();

    public boolean start() {
        listenerThread = new Thread() {
            @Override
            public void run() {
                long lastsleep = 500;
                while (Listener.this.isStarted() && ! isInterrupted()) {
                    Date startListen = new Date();
                    try {
                        Listener.this.listen();
                    } catch (InterruptedException e) {
                        // Normal exception, just exit
                        break;
                    } catch (Exception e) {
                        Date failedListen = new Date();
                        if ( (failedListen.getTime() - startListen.getTime() < lastsleep))
                            lastsleep *= 2;
                        else 
                            lastsleep = 500;
                        try {
                            Thread.sleep(lastsleep);
                        } catch (InterruptedException e1) {
                            break;
                        }
                        log(Level.ERROR, e, "Listener thread failed: %s", e.getMessage());
                    }
                }
            }
        };
        listenerThread.setDaemon(true);
        listenerThread.setName(String.format("Listener/%s", this));
        listenerThread.start();
        return true;
    }

    public void stop() {
        if(listenerThread != null) {
            listenerThread.interrupt();
            listenerThread = null;
        }
    }

    public void register(PassiveProbe<KeyType> p) {
        log(Level.DEBUG, "adding %s", p);
        String hostname = getHost(p);
        if(! probes.containsKey(p.getHost().getDnsName())) {
            probes.put(hostname, new HashMap<String, PassiveProbe<KeyType>>());
        }
        probes.get(hostname).put(p.getName(), p);        
    }

    protected abstract void listen() throws Exception;

    protected PassiveProbe<KeyType> findProbe(Message message) {
        String hostname = identifyHost(message);
        String probename = identifyProbe(message);
        log(Level.DEBUG, "looking for %s in %s", message, probes);
        if(! probes.containsKey(hostname)) {
            log(Level.WARN, "unregistered sender: %s", hostname);
            return null;
        }
        PassiveProbe<KeyType> pp = probes.get(hostname).get(probename);
        if( pp == null) {
            log(Level.WARN, "unregistered probe: %s", probename);
            return null;
        }
        return pp;
    }

    protected abstract String identifyHost(Message message);

    protected abstract String identifyProbe(Message message);

    protected abstract String getHost(PassiveProbe<KeyType> pp);

    public abstract String getSourceType();

}
