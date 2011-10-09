package jrds.snmp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import jrds.factories.xml.JrdsDocument;
import jrds.factories.xml.JrdsElement;
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
    //  private static final OID sysObjectID = new OID("1.3.6.1.2.1.1.2.0");
    //  private static final OID linuxOID = new OID("1.3.6.1.4.1.8072.3.2.10");
    //  private static final OID solarisOID = new OID("1.3.6.1.4.1.8072.3.2.3");
    //  private static final OID windowsNT_workstation_OID = new OID("1.3.6.1.4.1.311.1.1.3.1.1");
    //  private static final OID windowsNT_server_OID = new OID("1.3.6.1.4.1.311.1.1.3.1.2");
    //  private static final OID windowsNT_dc_OID = new OID("1.3.6.1.4.1.311.1.1.3.1.3");
    private static final OID sysORID = new OID("1.3.6.1.2.1.1.9.1.2");
    //  private static final OID ifMIB = new OID("1.3.6.1.2.1.31");
    //  private static final OID snmpMIB = new OID("1.3.6.1.6.3.1");
    private static final OID tcpMIB = new OID("1.3.6.1.2.1.49");
    private static final OID ip = new OID("1.3.6.1.2.1.4");
    private static final OID udpMIB = new OID("1.3.6.1.2.1.50");
    private static final Map<OID, String> OID2Probe;
    static {
        OID2Probe = new HashMap<OID, String>(5);
        OID2Probe.put(tcpMIB, "TcpSnmp");
        OID2Probe.put(ip, "IpSnmp");
        OID2Probe.put(udpMIB, "UdpSnmp");
    }

    static class LocalSnmpStarter extends SnmpStarter {
        Snmp snmp;
        Target target;
        @Override
        public boolean start() {
            try {
                snmp = new Snmp(new DefaultUdpTransportMapping());
                snmp.listen();
            } catch (IOException e) {
            }
            return true;
        }
        @Override
        public void stop() {
            try {
                snmp.close();
            } catch (IOException e) {
            }
        }
        @Override
        public Snmp getSnmp() {
            return snmp;
        }
        @Override
        public Target getTarget() {
            return target;
        }
        @Override
        public boolean isStarted() {
            return true;
        }
    }

    public SnmpDiscoverAgent() {
        super("SNMP");
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
    public void discover(String hostname, JrdsElement hostEleme,
            Map<String, JrdsDocument> probdescs, HttpServletRequest request) {
        Target hosttarget;
        try {
            hosttarget = makeSnmpTarget(request);
        } catch (UnknownHostException e) {
            throw new RuntimeException("Host name unknown",e);
        }

        Document hostDom = hostEleme.getOwnerDocument();
        boolean withOid = false;
        String withOidStr = request.getParameter("discoverWithOid");
        if(withOidStr != null && "true".equals(withOidStr.toLowerCase()))
            withOid = true;

        LocalSnmpStarter active = new LocalSnmpStarter();
        active.target = hosttarget;
        active.doStart();

        Element snmpElem = hostDom.createElement("snmp");
        if(hosttarget instanceof CommunityTarget) {
            CommunityTarget ct = (CommunityTarget) hosttarget;
            snmpElem.setAttribute("community", ct.getCommunity().toString());
        }
        snmpElem.setAttribute("version", Integer.toString( 1 + hosttarget.getVersion()));

        hostEleme.appendChild(snmpElem);

        for(JrdsDocument e: probdescs.values()) {
            JrdsElement root = e.getRootElement();
            JrdsElement buffer;
            
            buffer = root.getElementbyName("name");
            String name = buffer == null ? null : buffer.getTextContent();
            buffer = root.getElementbyName("index");
            String index = buffer == null ? null : buffer.getTextContent();
            buffer = e.findByPath("specific[@name='existOid']");
            String doesExistOid = buffer == null ? null : buffer.getTextContent();

            try {
                if(index != null && ! "".equals(index) ) {
                    log(Level.TRACE, "Found probe %s with index %s", name, index);
                    buffer = root.findByPath("specific[@name='labelOid']");
                    String labelOid = buffer == null ? null : buffer.getTextContent();
                    log(Level.TRACE, "label OID for %s: %s", name, labelOid);
                    try {
                        enumerateIndexed(hostEleme, active, name, index, labelOid, withOid);
                    } catch (Exception e1) {
                        log(Level.ERROR, e1, "Error discoverer %s for index %s: %s", name, index, e1);
                    }
                }
                else if(! "".equals(doesExistOid)) {
                    doesExist(hostEleme, active, name, doesExistOid);
                }
            } catch (Exception e1) {
                log(Level.ERROR, e1, "Error detecting %s: %s" , name, e1);
            }
        }
        active.doStop();
    }

    private void doesExist(Element hostEleme, SnmpStarter active, String name, String doesExistOid) throws IOException {
        OID OidExist = new OID(doesExistOid);
        String label = getLabel(active, Collections.singletonList(OidExist));
        if(label != null) {
            addProbe(hostEleme, name, null, null);
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

    private void enumerateIndexed(Element hostEleme, SnmpStarter active, String name, String indexOid, String labelOid, boolean withOid ) throws IOException {
        log(Level.TRACE, "Will enumerate %s", indexOid);
        Set<OID> oidsSet = Collections.singleton(new OID(indexOid));
        Map<OID, Object> indexes= (Map<OID, Object>) SnmpRequester.TREE.doSnmpGet(active, oidsSet);
        log(Level.TRACE, "Elements : %s", indexes);
        for(Map.Entry<OID, Object> e: indexes.entrySet()) {
            OID indexoid = e.getKey();
            String indexName = e.getValue().toString();
            int index = indexoid.last();
            List<String> argsTypes = new ArrayList<String>(1);
            argsTypes.add("String");
            List<String> argsValues = new ArrayList<String>(1);
            argsValues.add(indexName);

            if(withOid) {
                argsTypes.add("OID");
                argsValues.add(Integer.toString(index));
            }
            Element rrdElem = addProbe(hostEleme, name, argsTypes, argsValues);

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
    }

    @SuppressWarnings("unused")
    private void walksysORID(Element hostEleme, SnmpStarter active) throws IOException {
        log(Level.TRACE, "Will enumerate " + sysORID);
        Set<OID> oidsSet = Collections.singleton(new OID(sysORID));
        Map<OID, Object> indexes= (Map<OID, Object>) SnmpRequester.TREE.doSnmpGet(active, oidsSet);
        log(Level.TRACE, "Elements: %s", indexes);
        for(Object value: indexes.values()) {
            if(value instanceof OID) {
                String probe = OID2Probe.get(value);
                if(probe != null) {
                    Element rrdElem = hostEleme.getOwnerDocument().createElement("probe");
                    rrdElem.setAttribute("type", probe);
                    hostEleme.appendChild(rrdElem);         
                }
            }
        }

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

}
