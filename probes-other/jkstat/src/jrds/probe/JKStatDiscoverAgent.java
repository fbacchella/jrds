package jrds.probe;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import jrds.factories.xml.JrdsDocument;
import jrds.factories.xml.JrdsElement;
import jrds.objects.probe.IndexedProbe;
import jrds.webapp.DiscoverAgent;

import org.apache.log4j.Level;

import uk.co.petertribble.jkstat.api.JKstat;
import uk.co.petertribble.jkstat.api.Kstat;
import uk.co.petertribble.jkstat.client.RemoteJKstat;

public class JKStatDiscoverAgent extends DiscoverAgent {

    public JKStatDiscoverAgent() {
        super("JKStat");
    }

    @Override
    public void discover(String hostname, JrdsElement hostElement,
            Map<String, JrdsDocument> probdescs, HttpServletRequest request) {
        int port = jrds.Util.parseStringNumber(request.getParameter("discoverJKStatPort"), new Integer(KstatConnection.DEFAULTPORT));
        try {
            String hostName = hostname;
            URL remoteUrl = new URL("http", hostName, port, "/");
            JKstat remoteJk = new RemoteJKstat(remoteUrl.toString());
            List<String> argsTypes = Collections.emptyList();
            List<String> argsValues = Collections.emptyList();

            if(port != KstatConnection.DEFAULTPORT) {
                argsTypes = Collections.singletonList("Integer");
                argsValues = Collections.singletonList(Integer.toString(port).toString());
            }
            addConnexion(hostElement, KstatConnection.class.getName(), argsTypes, argsValues);

            ClassLoader cl = getClass().getClassLoader();
            Class<?> kstatClass = jrds.probe.KstatProbe.class;
            Map<String, Set<Kstat>> byClass = new HashMap<String, Set<Kstat>>();
            Map<String, Kstat> byTriplet = new HashMap<String, Kstat>();
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
                    addProbe(hostElement, "KstatNetstats2", Arrays.asList("String", "Integer"), Arrays.asList(module, instance));                   
                    byTriplet.remove(diskKstat.getTriplet());
                }
                else if(kName.equals(module + instance)) {
                    addProbe(hostElement, "KstatNetstats", Arrays.asList("String", "Integer"), Arrays.asList(module, instance));                   
                    byTriplet.remove(diskKstat.getTriplet());
                }
            }
            for(JrdsDocument e: probdescs.values()) {
                JrdsElement root = e.getRootElement();
                JrdsElement buffer;

                buffer = root.getElementbyName("name");
                String probe = buffer == null ? null : buffer.getTextContent();
                buffer = root.getElementbyName("probeClass");
                String probeClass = buffer == null ? null : buffer.getTextContent();
                Class<?> c = cl.loadClass(probeClass);
                buffer = root.findByPath("specific[@name='module']");
                String module = buffer == null ? null : buffer.getTextContent();
                buffer = root.findByPath("specific[@name='name']");
                String name = buffer == null ? null : buffer.getTextContent();
                
                buffer = root.findByPath("specific[@name='index']");
                String instanceVal = buffer == null ? null : buffer.getTextContent();
                int instance = jrds.Util.parseStringNumber(instanceVal, new Integer(0));
                if(module != null && ! "".equals(module) &&  kstatClass.isAssignableFrom(c)) {
                    Kstat active  = remoteJk.getKstat(module, instance, name);
                    if(active != null) {
                        log(Level.DEBUG, "probe found: %s:%d:%s", module, instance, name);
                        addProbe(hostElement, probe, null, null);
                        byTriplet.remove(active.getTriplet());
                    }
                    else if(IndexedProbe.class.isAssignableFrom(c)) {
                        String triplet = String.format("%s:(.+):%s", module, name);
                        Pattern indexedFetch = Pattern.compile(triplet.replace("${instance}", "([0-9]+)").replace("{", "\\{").replace("}", "\\}").replace("$", "\\$"));
                        log(Level.TRACE, "Search pattern is %s", indexedFetch);
                        for(String mp: byTriplet.keySet()) {
                            Matcher m = indexedFetch.matcher(mp);
                            if(m.matches()) {
                                String index = m.group(1);
                                log(Level.TRACE, "index found: %s for probe %s, with pattern %s and kstat probe %s", index, probe, indexedFetch.pattern(), mp);
                                addProbe(hostElement, probe, Collections.singletonList("Integer"), Collections.singletonList(index));
                            }
                        }
                    }
                }
            }
        } catch (MalformedURLException e) {
            this.log(Level.ERROR, "Malformed URL http://%s:%d/", hostname, port);
        } catch (ClassNotFoundException e) {
            this.log(Level.ERROR, e.getMessage());
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
}
