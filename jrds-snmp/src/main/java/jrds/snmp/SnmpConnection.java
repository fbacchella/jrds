package jrds.snmp;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.slf4j.event.Level;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TcpAddress;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.PDUFactory;
import org.snmp4j.util.TableEvent;
import org.snmp4j.util.TableUtils;

import jrds.factories.ProbeBean;
import jrds.starter.Connection;
import jrds.starter.Resolver;

@ProbeBean({ "community", "port", "version"})
public class SnmpConnection extends Connection<Target<? extends Address>> {
    static final String TCP = "tcp";
    static final String UDP = "udp";
    static final private OID hrSystemUptime = new OID(".1.3.6.1.2.1.25.1.1.0");
    static final private OID sysUpTimeInstance = new OID(".1.3.6.1.2.1.1.3.0");
    static final private PDUFactory pdufactory = new DefaultPDUFactory(PDU.GET);

    private int version = SnmpConstants.version2c;
    private String proto = UDP;
    private int port = 161;
    private String community = "public";
    // A default value for the uptime OID, from the HOST-RESSOURCES MIB
    private final OID uptimeOid = hrSystemUptime;
    private Target<? extends Address> snmpTarget;
    private final Map<OID, VariableBinding> toCollect = new HashMap<>();
    private final Map<OID, Map<Object, OID>> index = new HashMap<>();
    private final SnmpVars collected = new SnmpVars();

    public SnmpConnection() {
        toCollect.put(hrSystemUptime, new VariableBinding(hrSystemUptime));
        toCollect.put(sysUpTimeInstance, new VariableBinding(sysUpTimeInstance));
    }

    @Override
    public Target<? extends Address> getConnection() {
        return snmpTarget;
    }

    @Override
    public boolean startConnection() {
        Resolver resolver = getLevel().find(Resolver.class);
        if(!resolver.isStarted())
            return false;

        if(!getLevel().find(SnmpMainStarter.class).isStarted())
            return false;

        Address address;

        if (UDP.equalsIgnoreCase(proto)) {
            address = new UdpAddress(resolver.getInetAddress(), port);
        } else if (TCP.equalsIgnoreCase(proto)) {
            address = new TcpAddress(resolver.getInetAddress(), port);
        } else {
            return false;
        }
        if (community != null) {
            snmpTarget = new CommunityTarget<>(address, new OctetString(community));
        } else {
            log(Level.ERROR, "Only community-based security model supported");
            return false;
        }
        snmpTarget.setVersion(version);
        snmpTarget.setTimeout(getLevel().getTimeout() * 1000L / 2);
        snmpTarget.setRetries(1);
        try {
            doValueCache();
            return true;
        } catch (Exception e) {
            log(Level.ERROR, e, "Unable to reach host: %s", e);
            snmpTarget = null;
            return false;
        }
    }

    private void doValueCache() throws IOException {
        collected.clear();
        index.values().forEach(Map::clear);
        VariableBinding[] vbs = toCollect.values().toArray(VariableBinding[]::new);
        Map<OID, Object> wasCollected = SnmpRequester.populate(this, vbs);
        collected.putAll(wasCollected);
        if (! index.isEmpty()) {
            fillIndexes();
        }
    }

    private void fillIndexes() {
        TableUtils tableRet = new TableUtils(this.getSnmp(), getPdufactory());
        tableRet.setMaxNumColumnsPerPDU(30);
        tableRet.setMaxNumRowsPerPDU(20);
        OID[] oidTab = index.keySet().toArray(OID[]::new);
        for(TableEvent te: tableRet.getTable(snmpTarget, oidTab, null, null)) {
            if (te.isError()) {
                continue;
            }
            SnmpVars values = new SnmpVars();
            Arrays.stream(te.getColumns()).filter(Objects::nonNull).forEach(values::addVariable);
            for (var e: values.entrySet()) {
                OID indexOID = new OID(e.getKey());
                indexOID.trim(te.getIndex().size());
                index.computeIfAbsent(indexOID, o -> new HashMap<>()).put(e.getValue(), te.getIndex());
            }
        }
    }

    public boolean wasNotCollected(OID oid) {
        toCollect.computeIfAbsent(oid, VariableBinding::new);
        return ! collected.containsKey(oid);
    }

    public boolean wasNotIndexed(OID oid) {
        return index.computeIfAbsent(oid, o -> new HashMap<>()).isEmpty();
    }

    public Optional<int[]> findOidIndex(OID indexOid, Predicate<Object> filter) {
        for (Map.Entry<Object, OID> e: index.computeIfAbsent(indexOid, o -> new HashMap<>()).entrySet()) {
            if (filter.test(e.getKey())) {
                return Optional.of(e.getValue().getValue());
            }
        }
        return Optional.empty();
    }

    public Map<OID, Object> joinCollected(Set<OID> oidsSet, Map<OID, Object> wasCollected) {
        for (OID oid: oidsSet) {
            if (! wasCollected.containsKey(oid) && collected.containsKey(oid)) {
                wasCollected.put(oid, collected.get(oid));
            } else {
                OID newOid = (OID) oid.clone();
                toCollect.put(newOid, new VariableBinding(newOid));
            }
        }
        return wasCollected;
    }

    @Override
    public void stopConnection() {
        snmpTarget = null;
        collected.clear();
        index.values().forEach(Map::clear);
    }

    @Override
    public long setUptime() {
        Set<OID> upTimesOids = HashSet.newHashSet(2);
        upTimesOids.add(uptimeOid);
        // Fallback uptime OID, it should be always defined, from SNMPv2-MIB
        upTimesOids.add(sysUpTimeInstance);
        return readUptime(upTimesOids);
    }

    public long readUptime(Set<OID> upTimesOids) {
        try {
            for (Object value: SnmpRequester.RAW.doSnmpGet(this, upTimesOids).values()) {
                if (value instanceof Number) {
                    return ((Number) value).longValue();
                }
            }
        } catch (IOException e) {
            log(Level.ERROR, e, "Unable to get uptime: %s", e);
        }
        return 0;
    }

    public Snmp getSnmp() {
        return getLevel().find(SnmpMainStarter.class).snmp;
    }

    /**
     * @return the version
     */
    public Integer getVersion() {
        return version + 1;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(Integer version) {
        this.version = version - 1;
    }

    /**
     * @return the proto
     */
    public String getProto() {
        return proto;
    }

    /**
     * @param proto the proto to set
     */
    public void setProto(String proto) {
        this.proto = proto;
    }

    /**
     * @return the port
     */
    public Integer getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(Integer port) {
        this.port = port;
    }

    /**
     * @return the community
     */
    public String getCommunity() {
        return community;
    }

    /**
     * @param community the community to set
     */
    public void setCommunity(String community) {
        this.community = community;
    }

    /**
     * @return the pdufactory
     */
    public PDUFactory getPdufactory() {
        return pdufactory;
    }

    @Override
    public String toString() {
        return "snmp:" + proto + "://" + getHostName() + ":" + port;
    }

}
