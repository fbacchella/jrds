package jrds.snmp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import jrds.factories.xml.JrdsDocument;
import jrds.factories.xml.JrdsElement;
import jrds.webapp.Discover.ProbeDescSummary;
import jrds.webapp.DiscoverAgent;

import org.apache.log4j.Level;
import org.snmp4j.CommunityTarget;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SnmpDiscoverAgent extends DiscoverAgent {
    //Used to check if snmp is on
    static private final OID sysObjectID = new OID("1.3.6.1.2.1.1.2.0");

    static private class LocalSnmpStarter extends SnmpStarter {
        Snmp snmp;
        Target target;
        @Override
        public final boolean start() {
            try {
                snmp = new Snmp(new DefaultUdpTransportMapping());
                snmp.listen();
            } catch (IOException e) {
            }
            return true;
        }
        @Override
        public final void stop() {
            try {
                snmp.close();
            } catch (IOException e) {
            }
        }
        @Override
        public final Snmp getSnmp() {
            return snmp;
        }
        @Override
        public final Target getTarget() {
            return target;
        }
        @Override
        public final boolean isStarted() {
            return true;
        }
    }

    private Target hosttarget;
    private LocalSnmpStarter active;
    //Sort descriptions
    private final LinkedList<String> sortedProbeName = new LinkedList<String>();
    private final Map<String, ProbeDescSummary> summaries = new HashMap<String, ProbeDescSummary>();

    public SnmpDiscoverAgent() {
        super("SNMP", jrds.probe.snmp.SnmpProbe.class);
    }

    private Target makeSnmpTarget(HttpServletRequest request) throws UnknownHostException{
        String hostname = request.getParameter("host");
        String community = request.getParameter("discoverSnmpCommunity");
        if(community == null) {
            community = "public";
        }
        int port = jrds.Util.parseStringNumber(request.getParameter("discoverSnmpPort"), 161);
        IpAddress addr = new UdpAddress(InetAddress.getByName(hostname), port);
        Target hosttarget = new CommunityTarget(addr, new OctetString(community));
        hosttarget.setVersion(SnmpConstants.version2c);
        return hosttarget;
    }

    @Override
    public void discoverPost(String hostname, JrdsElement hostEleme,
            Map<String, JrdsDocument> probdescs, HttpServletRequest request) {

        boolean withOid = false;
        String withOidStr = request.getParameter("discoverWithOid");
        if(withOidStr != null && "true".equals(withOidStr.toLowerCase()))
            withOid = true;

        Set<String> done = new HashSet<String>();

        log(Level.TRACE, "Will search for probes %s", sortedProbeName);
        for(String name: sortedProbeName) {
            ProbeDescSummary summary = summaries.get(name);
            name = summary.name;
            if(done.contains(name))
                continue;
            log(Level.TRACE, "Trying to discover probe %s", name);
            String doesExistOid = summary.specifics.get("existOid");
            String index =  summary.specifics.get("indexOid");

            try {
                if(index != null && ! "".equals(index) ) {
                    log(Level.TRACE, "Found probe %s with index %s", name, index);
                    String labelOid = summary.specifics.get("labelOid");
                    log(Level.TRACE, "label OID for %s: %s", name, labelOid);
                    try {
                        if(enumerateIndexed(hostEleme, active, name, index, labelOid, withOid) > 0) {
                            done.add(name);
                            String hides = summary.specifics.get("hides");
                            if(hides != null && ! hides.isEmpty())
                                done.add(hides);
                        }
                    } catch (Exception e1) {
                        log(Level.ERROR, e1, "Error discoverer %s for index %s: %s", name, index, e1);
                    }
                }
                else if(doesExistOid != null && ! "".equals(doesExistOid)) {
                    doesExist(hostEleme, active, name, doesExistOid);
                }
                else {
                    log(Level.DEBUG, "undiscoverable probe: %s", name);
                }
            } catch (Exception e1) {
                log(Level.ERROR, e1, "Error detecting %s: %s" , name, e1);
            }

        }
        active.doStop();
    }

    private void doesExist(JrdsElement hostEleme, SnmpStarter active, String name, String doesExistOid) throws IOException {
        OID OidExist = new OID(doesExistOid);
        String label = getLabel(active, Collections.singletonList(OidExist));
        if(label != null) {
            addProbe(hostEleme, name, null, null, null);
        }
        log(Level.TRACE, "%s does exist: %s", name, label);
    }

    private String getLabel(SnmpStarter active, List<OID> labelsOID) throws IOException {
        Map<OID, Object> ifLabel = SnmpRequester.RAW.doSnmpGet(active, labelsOID);
        for(Map.Entry<OID, Object> labelEntry: ifLabel.entrySet()) {
            String label = labelEntry.getValue().toString();
            if(label.length() >= 1)
                return label;
        }
        return null;
    }

    private int enumerateIndexed(JrdsElement hostEleme, SnmpStarter active, String name, String indexOid, String labelOid, boolean withOid ) throws IOException {
        int count = 0;
        log(Level.TRACE, "Will enumerate %s", indexOid);
        Set<OID> oidsSet = Collections.singleton(new OID(indexOid));
        Map<OID, Object> indexes = SnmpRequester.TREE.doSnmpGet(active, oidsSet);
        log(Level.TRACE, "Elements : %s", indexes);
        for(Map.Entry<OID, Object> e: indexes.entrySet()) {
            Map<String, String> beans = new HashMap<String, String>(2);
            count++;
            OID indexoid = e.getKey();
            String indexName = e.getValue().toString();
            int index = indexoid.last();
            beans.put("index", indexName);

            if(withOid) {
                beans.put("oid", Integer.toString(index));
            }
            JrdsElement rrdElem = addProbe(hostEleme, name, null, null, beans);

            //We try to auto-generate the label
            if (labelOid != null && ! "".equals(labelOid)) {
                for(String lookin: labelOid.split(",")) {
                    OID Oidlabel = new OID(lookin.trim() + "." + index);
                    String label = getLabel(active, Collections.singletonList(Oidlabel));
                    if(label != null) {
                        rrdElem.setAttribute("label", label);
                        break;
                    }
                }
            }
        }
        return count;
    }

    @Override
    public List<FieldInfo> getFields() {
        FieldInfo community = new FieldInfo();
        community.dojoType = DojoType.TextBox;
        community.id = "discoverSnmpCommunity";
        community.label = "SNMP community";

        FieldInfo port = new FieldInfo();
        port.dojoType = DojoType.TextBox;
        port.id = "discoverSnmpPort";
        port.label = "SNMP Port";

        FieldInfo keepOID = new FieldInfo();
        keepOID.dojoType = DojoType.ToggleButton;
        keepOID.id = "discoverWithOid";
        keepOID.label = " Keep index OID ";

        return Arrays.asList(community, port, keepOID);
    }

    @Override
    public boolean exist(String hostname, HttpServletRequest request) {
        try {
            hosttarget = makeSnmpTarget(request);
            active = new LocalSnmpStarter();
            active.target = hosttarget;
            active.doStart();
            if(SnmpRequester.RAW.doSnmpGet(active, Collections.singleton(sysObjectID)).size() < 0) {
                log(Level.INFO, "SNMP not active on host %s", hostname);
                return false;
            }
            return true;
        } catch (UnknownHostException e) {
            log(Level.INFO, "Host name %s unknown", hostname);
            return false;
        } catch (IOException e1) {
            log(Level.INFO, "SNMP not active on host %s", hostname);
            return false;
        }
    }

    @Override
    public void addConnection(JrdsElement hostElement,
            HttpServletRequest request) {
        Document hostDom = hostElement.getOwnerDocument();
        Element snmpElem = hostDom.createElement("snmp");
        if(hosttarget instanceof CommunityTarget) {
            CommunityTarget ct = (CommunityTarget) hosttarget;
            snmpElem.setAttribute("community", ct.getCommunity().toString());
        }
        snmpElem.setAttribute("version", Integer.toString( 1 + hosttarget.getVersion()));

        hostElement.appendChild(snmpElem);
    }

    @Override
    public boolean isGoodProbeDesc(ProbeDescSummary summary) {
        return true;
    }

    @Override
    public void addProbe(JrdsElement hostElement, ProbeDescSummary summary,
            HttpServletRequest request) {
        String name = summary.name;

        //Don't discover if asked to don't do
        if(summary.specifics.get("nodiscover") != null)
            return;

        summaries.put(summary.name, summary);
        String hides = summary.specifics.get("hides");
        if(hides != null) {
            //Both are new, just add them in the good order
            if(! sortedProbeName.contains(hides) && ! sortedProbeName.contains(name)) {
                sortedProbeName.add(name);
                sortedProbeName.add(hides);
            }
            //hidden exist but this one is new, add before
            else if(sortedProbeName.contains(hides) && ! sortedProbeName.contains(name)) {
                int pos = sortedProbeName.indexOf(hides);
                sortedProbeName.add(pos, name);
            }
            //hidden exist but this one is new, add after
            else if(! sortedProbeName.contains(hides) && sortedProbeName.contains(name)) {
                int pos = sortedProbeName.indexOf(name);
                sortedProbeName.add(pos + 1, hides);
            }
        }
        //No hide, just put
        else {
            sortedProbeName.add(name);
        }
        return;
    }

}
