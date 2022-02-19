package jrds.probe.munin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.event.Level;

import jrds.factories.xml.JrdsElement;
import jrds.webapp.Discover.ProbeDescSummary;
import jrds.webapp.DiscoverAgent;

public class MuninDiscoverAgent extends DiscoverAgent {

    Set<String> muninProbes = Collections.emptySet();
    int port = MuninConnection.DEFAULTMUNINPORT;

    public MuninDiscoverAgent() throws ClassNotFoundException {
        super("Munin", jrds.probe.munin.Munin.class);
    }

    @Override
    public List<FieldInfo> getFields() {
        FieldInfo fi1 = new FieldInfo();
        fi1.dojoType = DojoType.TextBox;
        fi1.id = "discoverMuninPort";
        fi1.label = "Munin listening port";

        return Arrays.asList(fi1);
    }

    @Override
    public boolean exist(String hostName, HttpServletRequest request) {
        port = jrds.Util.parseStringNumber(request.getParameter("discoverMuninPort"), MuninConnection.DEFAULTMUNINPORT);
        try (Socket muninSocket = new Socket(hostName, port)) {
            muninSocket.setTcpNoDelay(true);
            try (PrintWriter out = new PrintWriter(muninSocket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(muninSocket.getInputStream()))){
                //Drop the  welcome line
                in.readLine();
                out.println("list");
                muninProbes = new HashSet<String>();
                for (String p: in.readLine().split(" ")) {
                    muninProbes.add(p);
                    log(Level.TRACE, "Munin probe found : %s", p);
                }
                out.println("quit");
                log(Level.DEBUG, "Munin probes found: %s", muninProbes);
                return true;
            }
        } catch (IOException e) {
            log(Level.ERROR, e, "Unable to connect: ", e);
            return false;
        }
    }

    @Override
    public void addConnection(JrdsElement hostElement,
            HttpServletRequest request) {
        JrdsElement cnx = hostElement.addElement("connection", "type=jrds.probe.munin.MuninConnection");
        if(port != MuninConnection.DEFAULTMUNINPORT) {
            cnx.addElement("attr", "name=port").setTextContent(Integer.toString(port));
        }
    }

    @Override
    public boolean isGoodProbeDesc(ProbeDescSummary summary) {
        String fetch = summary.specifics.get("fetch");
        if(fetch == null || fetch.isEmpty()) {
            return false;
        }
        return true;
    }

    @Override
    public void addProbe(JrdsElement hostElement, ProbeDescSummary summary,
            HttpServletRequest request) {
        String name = summary.name;

        String fetch = summary.specifics.get("fetch");

        if(summary.isIndexed) {
            Pattern indexedFetch = Pattern.compile(fetch.replace("${index}", "([^_]+)"));
            for(String mp: muninProbes) {
                Matcher m = indexedFetch.matcher(mp);
                if(m.matches()) {
                    String index = m.group(1);
                    Map<String, String> beans = Collections.singletonMap("index", index);
                    log(Level.TRACE, "index found: %s for probe %s, with pattern %s and munin probe %s", index, name, indexedFetch.pattern(), mp);
                    addProbe(hostElement, name, null, null, beans);
                }
            }
        }
        else{
            if(! muninProbes.contains(fetch)) {
                return;
            }
            muninProbes.remove(fetch);
            addProbe(hostElement, name, null, null, null);
        }
    }
}
