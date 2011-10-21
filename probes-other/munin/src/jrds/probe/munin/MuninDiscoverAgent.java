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

import jrds.factories.xml.JrdsDocument;
import jrds.factories.xml.JrdsElement;
import jrds.probe.IndexedProbe;
import jrds.webapp.DiscoverAgent;

import org.apache.log4j.Level;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class MuninDiscoverAgent extends DiscoverAgent {
    
    public MuninDiscoverAgent() {
        super("Munin");
    }

    @Override
    public void discover(String hostName, JrdsElement hostElement, Map<String, JrdsDocument> probdescs, HttpServletRequest request) {
        try {
            Document hostDom = hostElement.getOwnerDocument();

            int port = jrds.Util.parseStringNumber(request.getParameter("discoverMuninPort"), new Integer(4949));
            Socket muninSocket = null;
            try {
                muninSocket = new Socket(hostName, port);
            } catch (IOException e) {
                log(Level.INFO, "Munin not running on %s", hostName);
                return;
            }
            muninSocket.setTcpNoDelay(true);

            PrintWriter out = null;
            BufferedReader in = null;
            out = new PrintWriter(muninSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(muninSocket.getInputStream()));
            //Drop the  welcome line
            in.readLine();
            out.println("list");
            Set<String> muninProbes = new HashSet<String>();
            for(String p: in.readLine().split(" ")) {
                muninProbes.add(p);
                log(Level.TRACE, "Munin probe found : %s", p);
            }

            log(Level.DEBUG, "Munin probes found: %s", muninProbes);

            Element cnxElement = hostDom.createElement("connection");
            cnxElement.setAttribute("type", "jrds.probe.munin.MuninConnection");
            hostDom.getDocumentElement().appendChild(cnxElement);

            ClassLoader cl = getClass().getClassLoader();
            Class<?> muninClass = cl.loadClass("jrds.probe.munin.Munin");
            for(JrdsDocument e: probdescs.values()) {
                JrdsElement root = e.getRootElement();
                JrdsElement buffer;

                buffer = root.getElementbyName("name");
                String name = buffer == null ? null : buffer.getTextContent();
                buffer = root.getElementbyName("probeClass");
                String probeClass = buffer == null ? null : buffer.getTextContent();
                Class<?> c = cl.loadClass(probeClass);
                buffer = e.findByPath("specific[@name='fetch']");
                String fetchValue = buffer == null ? null : buffer.getTextContent();
                if(fetchValue != null && ! "".equals(fetchValue) &&  muninClass.isAssignableFrom(c)) {
                    if( muninProbes.contains(fetchValue) ) {
                        muninProbes.remove(fetchValue);
                        addProbe(hostElement, name, null, null);
                    }

                    else if(IndexedProbe.class.isAssignableFrom(c)) {
                        Pattern indexedFetch = Pattern.compile(fetchValue.replace("${index}", "(.+)"));
                        for(String mp: muninProbes) {
                            Matcher m = indexedFetch.matcher(mp);
                            if(m.matches()) {
                                String index = m.group(1);
                                log(Level.TRACE, "index found: %s for probe %s, with pattern %s and munin probe %s", index, name, indexedFetch.pattern(), mp);
                                addProbe(hostElement, name, Collections.singletonList("String"), Collections.singletonList(index));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log(Level.ERROR, e, "Generation Failed: ",e);
        }
    }

    @Override
    public List<FieldInfo> getFields() {
        FieldInfo fi1 = new FieldInfo();
        fi1.dojoType = DojoType.TextBox;
        fi1.id = "discoverMuninPort";
        fi1.label = "Munin listening port";

        FieldInfo fi2 = new FieldInfo();
        fi2.dojoType = DojoType.TextBox;
        fi2.id = "discoverMuninTag";
        fi2.label = "Munin host tag";

        return Arrays.asList(fi1, fi2);
    }
}
