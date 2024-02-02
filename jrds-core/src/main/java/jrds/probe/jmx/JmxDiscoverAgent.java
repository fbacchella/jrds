package jrds.probe.jmx;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.event.Level;

import jrds.factories.xml.JrdsDocument;
import jrds.factories.xml.JrdsElement;
import jrds.probe.JMXConnection;
import jrds.webapp.Discover.ProbeDescSummary;
import jrds.webapp.DiscoverAgent;

public class JmxDiscoverAgent extends DiscoverAgent {

    private String hostname;

    private final JMXConnection cnx = new JMXConnection() {
        @Override
        public String getHostName() {
            return JmxDiscoverAgent.this.hostname;
        }

        @Override
        public int getTimeout() {
            return JmxDiscoverAgent.this.getTimeout();
        }

    };

    public JmxDiscoverAgent() {
        super("JMX", jrds.probe.JMX.class);
    }

    @Override
    public List<FieldInfo> getFields() {
        FieldInfo port = new FieldInfo();
        port.dojoType = DojoType.TextBox;
        port.id = "discoverJmxPort";
        port.label = "JMX Port";
        port.value = "";

        FieldInfo protocol = new FieldInfo();
        protocol.dojoType = DojoType.TextBox;
        protocol.id = "discoverJmxProtocol";
        protocol.label = "JMX protocol";
        protocol.value = "rmi";

        FieldInfo user = new FieldInfo();
        user.dojoType = DojoType.TextBox;
        user.id = "discoverJmxUser";
        user.label = "JMX user";
        user.value = "";

        FieldInfo password = new FieldInfo();
        password.dojoType = DojoType.TextBox;
        password.id = "discoverJmxPassword";
        password.label = "JMX password";
        password.value = "";

        return Arrays.asList(port, protocol, user, password);
    }

    private JmxAbstractDataSource<?> connect(String hostname, HttpServletRequest request) {
        this.hostname = hostname;
        String protocolName = request.getParameter("discoverJmxProtocol");
        if(protocolName != null && !protocolName.trim().isEmpty()) {
            cnx.setProtocol(JmxProtocol.valueOf(protocolName.trim()).toString());
        }
        Integer port = jrds.Util.parseStringNumber(request.getParameter("discoverJmxPort"), 0);
        if(port != 0) {
            cnx.setPort(port);
        }
        cnx.setUser(request.getParameter("discoverJmxUser"));
        cnx.setPassword(request.getParameter("discoverJmxPassword"));
        if(cnx.startConnection()) {
            return cnx.getConnection();
        } else {
            return null;
        }
    }

    @Override
    public boolean exist(String hostname, HttpServletRequest request) {
        return connect(hostname, request) != null;
    }

    @Override
    public void discoverPost(String hostname, JrdsElement hostEleme,
                             Map<String, JrdsDocument> probdescs, HttpServletRequest request) {
        cnx.stopConnection();
    }

    @Override
    public boolean isGoodProbeDesc(ProbeDescSummary summary) {
        JmxAbstractDataSource<?> mbean = cnx.getConnection();
        boolean valid = true;
        boolean enumerated = false;
        for(String name: summary.specifics.get("mbeanNames").split(" *; *")) {
            enumerated = true;
            try {
                Collection<ObjectName> mbeanNames = mbean.getNames(this, new ObjectName(name));
                log(Level.TRACE, "%s", "found mbeans %s for %s", mbeanNames, summary.name);
                if(mbeanNames.size() > 1 && !summary.isIndexed) {
                    log(Level.WARN, "not indexed probe %s return more than one mbean", summary.name);
                    valid = false;
                } else if(mbeanNames.size() > 0) {
                    valid &= true;
                } else {
                    valid = false;
                }
            } catch (MalformedObjectNameException e) {
                log(Level.WARN, "invalid name for auto discovery of probe %s: %s", summary.name, e);
            }
        }
        return valid && enumerated;
    }

    @Override
    public void addConnection(JrdsElement hostElement, HttpServletRequest request) {
        JrdsElement cnxElem = hostElement.addElement("connection", "type=jrds.probe.JMXConnection");
        cnxElem.addElement("attr", "name=protocol").setTextContent(cnx.getProtocol().toString());
        cnxElem.addElement("attr", "name=port").setTextContent(Integer.toString(cnx.getPort()));
    }

    private Set<String> enumerateIndexes(ProbeDescSummary summary) {
        JmxAbstractDataSource<?> mbean = cnx.getConnection();
        Set<String> indexes = new HashSet<>();
        for(String name: summary.specifics.get("mbeanNames").split(" *; *")) {
            try {
                Collection<ObjectName> mbeanNames = mbean.getNames(this, new ObjectName(name));
                Pattern p = Pattern.compile(summary.specifics.get("mbeanIndex"));
                for(ObjectName oneMbean: mbeanNames) {
                    log(Level.DEBUG, "%s", oneMbean.getCanonicalName());
                    Matcher m = p.matcher(oneMbean.toString());
                    if(m.matches() && !m.group(1).isEmpty()) {
                        log(Level.DEBUG, "index found: %s for %s", m.group(1), summary.name);
                        indexes.add(m.group(1));
                    }
                }
            } catch (MalformedObjectNameException e) {
                log(Level.WARN, "invalid name for auto discovery of probe %s: %s", summary.name, e);
            }
        }
        return indexes;
    }

    @Override
    public void addProbe(JrdsElement hostElement, ProbeDescSummary summary, HttpServletRequest request) {
        if(summary.isIndexed) {
            for(String index: enumerateIndexes(summary)) {
                hostElement.addElement("probe", "type=" + summary.name).addElement("attr", "name=index").setTextContent(index);

            }
        } else {
            hostElement.addElement("probe", "type=" + summary.name);
        }
    }

}
