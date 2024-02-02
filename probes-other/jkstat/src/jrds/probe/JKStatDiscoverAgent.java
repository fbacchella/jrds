package jrds.probe;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;

import jrds.factories.xml.JrdsDocument;
import jrds.factories.xml.JrdsElement;
import jrds.webapp.Discover.ProbeDescSummary;
import jrds.webapp.DiscoverAgent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.petertribble.jkstat.api.JKstat;
import uk.co.petertribble.jkstat.api.Kstat;
import uk.co.petertribble.jkstat.client.KClientConfig;
import uk.co.petertribble.jkstat.client.RemoteJKstat;

public class JKStatDiscoverAgent extends DiscoverAgent {

    static private final Class<?> jkstatClass = jrds.probe.KstatProbe.class;

    private final Map<String, Kstat> byTriplet = new HashMap<String, Kstat>();
    private JKstat remoteJk;
    private int port;

    public JKStatDiscoverAgent() {
        super("JKStat", jkstatClass);
    }

    @Override
    public void discoverPre(String hostname, JrdsElement hostElement,
            Map<String, JrdsDocument> probdescs, HttpServletRequest request) {
        Map<String, Set<Kstat>> byClass = new HashMap<String, Set<Kstat>>();

        for(Kstat k : remoteJk.getKstats()) {
            String kclass = k.getKstatClass();
            if(! byClass.containsKey(kclass))
                byClass.put(kclass, new HashSet<Kstat>());
            byClass.get(kclass).add(k);
            byTriplet.put(k.getTriplet(), k);
        }
        log(Level.TRACE, "classes: %s", byClass);
        log(Level.TRACE, "triplets: %s", byTriplet);

        //We try to discover netcard, explicit check, the pattern is too complicated
        for(Kstat diskKstat: byClass.get("net")) {
            String module = diskKstat.getModule();
            String instance = diskKstat.getInstance();
            String kName = diskKstat.getName();
            if("statistics".equals(kName)) {
                addProbe(hostElement, "KstatNetstats2", Arrays.asList("String", "Integer"), Arrays.asList(module, instance), null);                   
                byTriplet.remove(diskKstat.getTriplet());
            }
            else if(kName.equals(module + instance)) {
                addProbe(hostElement, "KstatNetstats", Arrays.asList("String", "Integer"), Arrays.asList(module, instance), null);                   
                byTriplet.remove(diskKstat.getTriplet());
            }
        }
    }

    @Override
    public List<FieldInfo> getFields() {
        FieldInfo fi = new FieldInfo();
        fi.dojoType = DojoType.TextBox;
        fi.id = "discoverJKStatPort";
        fi.label = " JKstat listening port";
        return Collections.singletonList(fi);
    }

    @Override
    public boolean exist(String hostname, HttpServletRequest request) {
        port = jrds.Util.parseStringNumber(request.getParameter("discoverJKStatPort"), new Integer(KstatConnection.DEFAULTPORT));
        remoteJk = new RemoteJKstat(new KClientConfig("http://" + hostname + ":" + port));
        try {
            remoteJk.getKstat("unix", 0, "system_misc");
            return true;
        } catch (Exception e) {
            log(Level.INFO, "JKStat not running on %s", hostname);
            return false;
        }
    }

    @Override
    public void addConnection(JrdsElement hostElement,
            HttpServletRequest request) {
        Map<String, String> beans = new HashMap<String, String>(1);
        if(port != KstatConnection.DEFAULTPORT) {
            beans.put("port", Integer.toString(port));
        }
        addConnexion(hostElement, KstatConnection.class.getName(), null, null, beans);
    }

    @Override
    public boolean isGoodProbeDesc(ProbeDescSummary summary) {
        String module = summary.specifics.get("module");
        if(module == null || module.isEmpty())
            return false;
        return true;
    }

    @Override
    public void addProbe(JrdsElement hostElement, ProbeDescSummary summary,
            HttpServletRequest request) {

        String probe = summary.name;
        String module = summary.specifics.get("module");
        String name = summary.specifics.get("name");

        String instanceVal = summary.specifics.get("index");
        int instance = jrds.Util.parseStringNumber(instanceVal, new Integer(0));
        Kstat active  = remoteJk.getKstat(module, instance, name);
        if(active != null) {
            log(Level.DEBUG, "probe found: %s:%d:%s", module, instance, name);
            addProbe(hostElement, probe, null, null, null);
            byTriplet.remove(active.getTriplet());
        }
        else if(summary.isIndexed) {
            String triplet = String.format("%s:(.+):%s", module, name);
            Pattern indexedFetch = Pattern.compile(triplet.replace("${instance}", "([0-9]+)").replace("{", "\\{").replace("}", "\\}").replace("$", "\\$"));
            log(Level.TRACE, "Search pattern is %s", indexedFetch);
            for(String mp: byTriplet.keySet()) {
                Matcher m = indexedFetch.matcher(mp);
                if(m.matches()) {
                    String index = m.group(1);
                    log(Level.TRACE, "index found: %s for probe %s, with pattern %s and kstat probe %s", index, probe, indexedFetch.pattern(), mp);
                    addProbe(hostElement, probe, Collections.singletonList("Integer"), Collections.singletonList(index), null);
                }
            }
        }
    }
}
