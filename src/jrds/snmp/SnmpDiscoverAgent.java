package jrds.snmp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import jrds.factories.xml.CompiledXPath;
import jrds.factories.xml.JrdsNode;
import jrds.webapp.DiscoverAgent;

import org.apache.log4j.Logger;
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
    static final private Logger logger = Logger.getLogger(SnmpDiscoverAgent.class);

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
    
    //Works in progress, does nothing now
    //private static final Map<String, String> hides;
    //static {
    //  hides = new HashMap<String, String>();
    //  hides.put("IfXSnmp", "IfSnmp");
    //  hides.put("DiskIo64", "DiskIo");
    //}

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

    private Target makeSnmpTarget(HttpServletRequest request) throws UnknownHostException{
        String hostname = request.getParameter("host");
        String community = request.getParameter("community");
        if(community == null) {
            community = "public";
        }
        IpAddress addr = new UdpAddress(InetAddress.getByName(hostname), 161);
        Target hosttarget = new CommunityTarget(addr, new OctetString(community));
        hosttarget.setVersion(SnmpConstants.version2c);
        return hosttarget;
    }

    @Override
    public void discover(String hostname, Document hostDom,
            Collection<JrdsNode> probdescs, HttpServletRequest request) {
        Target hosttarget;
        try {
            hosttarget = makeSnmpTarget(request);
        } catch (UnknownHostException e) {
            throw new RuntimeException("Host name unknown",e);
        }

        boolean withOid = false;
        String withOidStr = request.getParameter("withoid");
        if(withOidStr != null)
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

        Element hostEleme = hostDom.getDocumentElement();

        hostEleme.appendChild(snmpElem);

        for(JrdsNode e: probdescs) {
            String name = e.evaluate(CompiledXPath.get("/probedesc/name"));
            String index = e.evaluate(CompiledXPath.get("/probedesc/index"));
            String doesExistOid = e.evaluate(CompiledXPath.get("/probedesc/specific[@name='existOid']"));
//          if(logger.isTraceEnabled()) {
//              String className = e.evaluate(CompiledXPath.get("/probedesc/probeClass"));
//              String classFileName = '/' + className.replace('.', '/') + ".class";
//              URL url = this.getPropertiesManager().extensionClassLoader.getResource(classFileName);
//              logger.trace("Probe " + classFileName + " class found in " + url);
//          }

            try {
                if(index != null && ! "".equals(index) ) {
                    logger.trace(jrds.Util.delayedFormatString("Found probe %s with index %s", name, index));
                    String labelOid = e.evaluate(CompiledXPath.get("/probedesc/specific[@name='labelOid']"));
                    logger.trace(jrds.Util.delayedFormatString("label OID for %s: %s", name, labelOid));
                    try {
                        enumerateIndexed(hostEleme, active, name, index, labelOid, withOid);
                    } catch (Exception e1) {
                        logger.error("Error discoverer " + name + " for index " + index + ": " +e1);
                    }
                }
                else if(! "".equals(doesExistOid)) {
                    doesExist(hostEleme, active, name, doesExistOid);
                }
            } catch (Exception e1) {
                logger.error("Error detecting " + name + ": " +e1);
            }
        }
        active.doStop();

        //walksysORID(hostEleme, active);
        
    }
    private void doesExist(Element hostEleme, SnmpStarter active, String name, String doesExistOid) throws IOException {
        OID OidExist = new OID(doesExistOid);
        String label = getLabel(active, Collections.singletonList(OidExist));
        if(label != null) {
            Element rrdElem = hostEleme.getOwnerDocument().createElement("probe");
            rrdElem.setAttribute("type", name);
            hostEleme.appendChild(rrdElem);         
        }
        logger.trace(name + " does exist: " + label);
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
        logger.trace("Will enumerate " + indexOid);
        Set<OID> oidsSet = Collections.singleton(new OID(indexOid));
        Map<OID, Object> indexes= (Map<OID, Object>) SnmpRequester.TREE.doSnmpGet(active, oidsSet);
        logger.trace("Elements :"  + indexes);
        for(Map.Entry<OID, Object> e: indexes.entrySet()) {
            OID indexoid = e.getKey();
            String indexfName = e.getValue().toString();
            int index = indexoid.last();
            Element rrdElem = hostEleme.getOwnerDocument().createElement("probe");
            rrdElem.setAttribute("type", name);
            Element arg1 = hostEleme.getOwnerDocument().createElement("arg");
            arg1.setAttribute("type", "String");
            arg1.setAttribute("value", indexfName.toString());
            rrdElem.appendChild(arg1);

            if(withOid) {
                Element arg2 = hostEleme.getOwnerDocument().createElement("arg");
                arg2.setAttribute("type", "OID");
                arg2.setAttribute("value", Integer.toString(index));
                rrdElem.appendChild(arg2);
            }

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
            hostEleme.appendChild(rrdElem);
        }
    }

    @SuppressWarnings("unused")
    private void walksysORID(Element hostEleme, SnmpStarter active) throws IOException {
        logger.trace("Will enumerate " + sysORID);
        Set<OID> oidsSet = Collections.singleton(new OID(sysORID));
        Map<OID, Object> indexes= (Map<OID, Object>) SnmpRequester.TREE.doSnmpGet(active, oidsSet);
        if(logger.isTraceEnabled())
            logger.trace("Elements :"  + indexes);
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


    //  private String operatingSystem(SnmpStarter active) throws IOException {
    //      Map<OID, Object> osType = SnmpRequester.RAW.doSnmpGet(active, Collections.singletonList(sysObjectID));
    //      OID  identity = (OID)osType.get(sysObjectID);
    //      
    //      return null;
    //  }


}
