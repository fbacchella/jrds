import java.io.IOException;
import java.util.List;

import jrds.Tools;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.snmp4j.AbstractTarget;
import org.snmp4j.CommunityTarget;
import org.snmp4j.Snmp;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.PDUFactory;
import org.snmp4j.util.TreeEvent;
import org.snmp4j.util.TreeUtils;


public class SnmpWalk {
    static final private Logger logger = Logger.getLogger(SnmpWalk.class);

    static class ActiveSnmp {
        Snmp snmp;
        static final PDUFactory factory = new DefaultPDUFactory();
        AbstractTarget target;
    }

    @BeforeClass
    static public void configure() throws IOException {
        Tools.configure();
        logger.setLevel(Level.DEBUG);
        Tools.setLevel(new String[] {"jrds"}, logger.getLevel());
        logger.addAppender(Logger.getLogger("jrds").getAppender("jrds"));
    }

    @Test
    public void testWalk() throws Exception {
        ActiveSnmp active = new ActiveSnmp();
        active.snmp = new Snmp(new DefaultUdpTransportMapping());
        active.snmp.listen();

        IpAddress ip = new UdpAddress(java.net.InetAddress.getByName("localhost"), 161);
        CommunityTarget snmpTarget = new CommunityTarget(ip, new OctetString("public"));
        snmpTarget.setVersion(SnmpConstants.version2c);
        active.target = snmpTarget;

        TreeUtils tree = new TreeUtils(active.snmp, ActiveSnmp.factory);
        List<TreeEvent> tempAllIfs = tree.getSubtree(active.target, new OID("1.3.6.1.2.1.2.2.1.2"));
        for(TreeEvent te: tempAllIfs) {
            logger.trace(te);
            for(VariableBinding var: te.getVariableBindings()) {
                logger.trace(var.getOid() + "=" + var.getVariable());
            }
        }
    }
}
