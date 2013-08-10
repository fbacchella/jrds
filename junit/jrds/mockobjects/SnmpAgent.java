package jrds.mockobjects;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Random;

import org.apache.log4j.Logger;
import org.snmp4j.TransportMapping;
import org.snmp4j.agent.BaseAgent;
import org.snmp4j.agent.CommandProcessor;
import org.snmp4j.agent.mo.MOTableRow;
import org.snmp4j.agent.mo.snmp.RowStatus;
import org.snmp4j.agent.mo.snmp.SnmpCommunityMIB;
import org.snmp4j.agent.mo.snmp.SnmpNotificationMIB;
import org.snmp4j.agent.mo.snmp.SnmpTargetMIB;
import org.snmp4j.agent.mo.snmp.StorageType;
import org.snmp4j.agent.mo.snmp.VacmMIB;
import org.snmp4j.agent.security.MutableVACM;
import org.snmp4j.mp.MPv3;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModel;
import org.snmp4j.security.USM;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.Variable;
import org.snmp4j.transport.TransportMappings;
import org.snmp4j.util.ThreadPool;

public class SnmpAgent extends BaseAgent {
    Logger logger = Logger.getLogger(SnmpAgent.class);
    int port = 0;
    int boot = 0;

    public SnmpAgent() throws IOException {
        super((String) null);
        agent = new CommandProcessor(new OctetString(MPv3.createLocalEngineID()));
        agent.setWorkerPool(ThreadPool.create("RequestPool", 4));
        init();
        getServer().addContext(new OctetString("public"));
        finishInit();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void addCommunities(SnmpCommunityMIB communityMIB) {
        Variable[] com2sec = new Variable[] {
                new OctetString("public"),              // community name
                new OctetString("cpublic"),              // security name
                getAgent().getContextEngineID(),        // local engine ID
                new OctetString("public"),              // default context name
                new OctetString(),                      // transport tag
                new Integer32(StorageType.nonVolatile),    // storage type
                new Integer32(RowStatus.active)         // row status
        };
        logger.debug("SnmpCommunityEntry: " + communityMIB.getSnmpCommunityEntry());
        @SuppressWarnings("rawtypes")
        MOTableRow row =
        communityMIB.getSnmpCommunityEntry().createRow(
                new OctetString("public2public").toSubIndex(true), com2sec);
        logger.debug("Row: " + row);
        communityMIB.getSnmpCommunityEntry().addRow(row);
        snmpCommunityMIB.setSourceAddressFiltering(false);
    }

    @Override
    protected void addNotificationTargets(SnmpTargetMIB targetMIB,
            SnmpNotificationMIB arg1) {
        targetMIB.addDefaultTDomains();

    }

    @Override
    protected void addUsmUser(USM arg0) {

    }

    @Override
    protected void addViews(VacmMIB vacm) {
        vacm.addGroup(SecurityModel.SECURITY_MODEL_SNMPv1,
                new OctetString("cpublic"),
                new OctetString("v1v2group"),
                StorageType.nonVolatile);
        vacm.addGroup(SecurityModel.SECURITY_MODEL_SNMPv2c,
                new OctetString("cpublic"),
                new OctetString("v1v2group"),
                StorageType.nonVolatile);
        vacm.addAccess(new OctetString("v1v2group"), new OctetString("public"),
                SecurityModel.SECURITY_MODEL_ANY,
                SecurityLevel.NOAUTH_NOPRIV,
                MutableVACM.VACM_MATCH_EXACT,
                new OctetString("fullReadView"),
                new OctetString("fullWriteView"),
                new OctetString("fullNotifyView"),
                StorageType.nonVolatile);
        vacm.addViewTreeFamily(new OctetString("fullReadView"), new OID("1.3"),
                new OctetString(), VacmMIB.vacmViewIncluded,
                StorageType.nonVolatile);
        vacm.addViewTreeFamily(new OctetString("fullWriteView"), new OID("1.3"),
                new OctetString(), VacmMIB.vacmViewIncluded,
                StorageType.nonVolatile);
        vacm.addViewTreeFamily(new OctetString("fullNotifyView"), new OID("1.3"),
                new OctetString(), VacmMIB.vacmViewIncluded,
                StorageType.nonVolatile);

        vacm.addViewTreeFamily(new OctetString("restrictedReadView"),
                new OID("1.3.6.1.2"),
                new OctetString(), VacmMIB.vacmViewIncluded,
                StorageType.nonVolatile);
        vacm.addViewTreeFamily(new OctetString("restrictedWriteView"),
                new OID("1.3.6.1.2.1"),
                new OctetString(),
                VacmMIB.vacmViewIncluded,
                StorageType.nonVolatile);
        vacm.addViewTreeFamily(new OctetString("restrictedNotifyView"),
                new OID("1.3.6.1.2"),
                new OctetString(), VacmMIB.vacmViewIncluded,
                StorageType.nonVolatile);
        vacm.addViewTreeFamily(new OctetString("restrictedNotifyView"),
                new OID("1.3.6.1.6.3.1"),
                new OctetString(), VacmMIB.vacmViewIncluded,
                StorageType.nonVolatile);

        vacm.addViewTreeFamily(new OctetString("testReadView"),
                new OID("1.3.6.1.2"),
                new OctetString(), VacmMIB.vacmViewIncluded,
                StorageType.nonVolatile);
        vacm.addViewTreeFamily(new OctetString("testReadView"),
                new OID("1.3.6.1.2.1.1"),
                new OctetString(), VacmMIB.vacmViewExcluded,
                StorageType.nonVolatile);
        vacm.addViewTreeFamily(new OctetString("testWriteView"),
                new OID("1.3.6.1.2.1"),
                new OctetString(),
                VacmMIB.vacmViewIncluded,
                StorageType.nonVolatile);
        vacm.addViewTreeFamily(new OctetString("testNotifyView"),
                new OID("1.3.6.1.2"),
                new OctetString(), VacmMIB.vacmViewIncluded,
                StorageType.nonVolatile);
    }

    @Override
    protected void registerManagedObjects() {
    }

    @Override
    protected void unregisterManagedObjects() {
    }

    protected void initTransportMappings() throws IOException {
        Random gen = new Random();

        transportMappings = new TransportMapping[1];
        port = gen.nextInt(32768) + 32768;
        Address addr = new UdpAddress(InetAddress.getByAddress(new byte[] {127,0,0,1}), port);
        @SuppressWarnings("rawtypes")
        TransportMapping tm =
        TransportMappings.getInstance().createTransportMapping(addr);
        transportMappings[0] = tm;
    }

    public int getPort() {
        return port;
    }

    /* (non-Javadoc)
     * @see org.snmp4j.agent.BaseAgent#initMessageDispatcher()
     */
    @Override
    protected void initMessageDispatcher() {
        super.initMessageDispatcher();
        dispatcher.addTransportMapping(transportMappings[0]);
    }

    /* (non-Javadoc)
     * @see org.snmp4j.agent.BaseAgent#getEngineBoots()
     */
    @Override
    protected int getEngineBoots() {
        return ++boot;
    }

    /* (non-Javadoc)
     * @see org.snmp4j.agent.BaseAgent#setEngineBoots(int)
     */
    @Override
    protected void setEngineBoots(int engineBoots) {
        this.boot = engineBoots;
    }

}
