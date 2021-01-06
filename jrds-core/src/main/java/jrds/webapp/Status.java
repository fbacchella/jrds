package jrds.webapp;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;

import jrds.HostInfo;
import jrds.HostsList;
import jrds.starter.Timer.Stats;

/**
 * A few stats for jrds inner status
 * 
 * @author Fabrice Bacchella
 */
@ServletSecurity
public class Status extends JrdsServlet {

    /**
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        HostsList hl = getHostsList();

        ParamsBean params = new ParamsBean(req, hl);
        if(!allowed(params, ACL.ALLOWEDACL, req, res)) {
            return;
        }

        Collection<HostInfo> hosts = hl.getHosts();
        int numHosts = hosts.size();
        int numProbes = 0;
        int generation = hl.getGeneration();
        for(HostInfo h: hosts) {
            numProbes += h.getNumProbes();
        }
        Map<String, Stats> stats = new HashMap<String, Stats>();
        hl.getChildsStream().forEach(t -> stats.put(t.getName(), t.getStats()));

        if(params.getValue("json") != null) {
            JrdsJSONWriter writer = new JrdsJSONWriter(res);
            try {
                writer.object();
                writer.key("Hosts").value(numHosts);
                writer.key("Probes").value(numProbes);
                writer.key("Timers");
                writer.array();
                for(Map.Entry<String, Stats> e: stats.entrySet()) {
                    writer.object();
                    long lastCollectAgo = (System.currentTimeMillis() - e.getValue().getLastCollect().getTime()) / 1000;
                    writer.key("Name").value(e.getKey());
                    writer.key("LastCollect").value(lastCollectAgo);
                    writer.key("LastDuration").value(e.getValue().getDuration());
                    writer.endObject();
                }
                writer.endArray();
                writer.key("Generation").value(generation);
                writer.endObject();
                writer.flush();
            } catch (JSONException e) {
            }
        } else {
            res.setContentType("text/plain");
            res.addHeader("Cache-Control", "no-cache");
            PrintWriter writer = res.getWriter();
            writer.println("Hosts: " + numHosts);
            writer.println("Probes: " + numProbes);
            for(Map.Entry<String, Stats> e: stats.entrySet()) {
                long lastCollectAgo = (System.currentTimeMillis() - e.getValue().getLastCollect().getTime()) / 1000;
                writer.println("Timer name: " + e.getKey());
                writer.println("    Last collect: " + lastCollectAgo + "s ago (" + lastCollectAgo + ")");
                writer.println("    Last running duration: " + e.getValue().getDuration() / 1000 + "s");
            }
            writer.flush();
        }
    }
}
