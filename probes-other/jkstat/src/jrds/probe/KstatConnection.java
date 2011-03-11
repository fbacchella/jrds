package jrds.probe;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Level;

import jrds.starter.Connection;
import uk.co.petertribble.jkstat.api.JKstat;
import uk.co.petertribble.jkstat.api.Kstat;
import uk.co.petertribble.jkstat.client.RemoteJKstat;

public class KstatConnection extends Connection<JKstat> {
    public static final int DEFAULTPORT = 3000;
    int port;
    JKstat remoteJk = null;

    public KstatConnection() {
        this.port = DEFAULTPORT;
    }

    public KstatConnection(Integer port) {
        this.port = port;
    }

    @Override
    public JKstat getConnection() {
        return remoteJk;
    }

    @Override
    public long setUptime() {
        Kstat ks = remoteJk.getKstat("unix", 0, "system_misc");
        if(ks == null) {
            return 0;
        }
        Long uptime = (Long)ks.getData("boot_time");
        long now = System.currentTimeMillis() / 1000;
        return now - uptime.longValue();
    }

    @Override
    public boolean startConnection() {
        try {
            String hostName = getHostName();
            URL remoteUrl = new URL("http", hostName, port, "/");
            remoteJk = new RemoteJKstat(remoteUrl.toString());
            return true;
        } catch (MalformedURLException e) {
            this.log(Level.ERROR, "Malformed URL http://%s:%d/", getHostName(), port);
            return false;
        }
    }

    @Override
    public void stopConnection() {
        remoteJk = null;
    }

    public  static void main(String[] args) throws IOException {
        int port = 3000;
        String hostname = args[0];
        String[] kstatinfo = args[1].split(":");
        URL remoteUrl = new URL("http", hostname, port, "/");
        JKstat remoteJk = new RemoteJKstat(remoteUrl.toString());
        String module = kstatinfo[0];
        int instance = Integer.parseInt(kstatinfo[1]);
        String name = kstatinfo[2];
        Kstat active  = remoteJk.getKstat(module, instance, name);
        Set<java.lang.String> statistics = active.statistics();
        List<String> attr = new ArrayList<String>(statistics.size());
        attr.addAll(statistics);
        Collections.sort(attr, String.CASE_INSENSITIVE_ORDER);
        for(Object key: attr ) {
            String keyName = key.toString();
            System.out.println("<ds>");
            System.out.println("    <dsName>" + keyName + "</dsName>");
            System.out.println("    <dsType>counter</dsType>");
            if(keyName.length() > 20) {
                System.out.println("    <collect>" + keyName + "</collect>");
            }
            System.out.println("</ds>");
        }
    }
}
