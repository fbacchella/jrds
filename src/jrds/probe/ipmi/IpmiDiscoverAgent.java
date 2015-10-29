package jrds.probe.ipmi;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import jrds.Util;
import jrds.factories.xml.JrdsDocument;
import jrds.factories.xml.JrdsElement;
import jrds.probe.ipmi.Handle.MutableInteger;
import jrds.webapp.Discover.ProbeDescSummary;
import jrds.webapp.DiscoverAgent;

import org.apache.log4j.Level;

import com.veraxsystems.vxipmi.api.async.ConnectionHandle;
import com.veraxsystems.vxipmi.api.sync.IpmiConnector;
import com.veraxsystems.vxipmi.coding.commands.IpmiVersion;
import com.veraxsystems.vxipmi.coding.commands.PrivilegeLevel;
import com.veraxsystems.vxipmi.coding.commands.sdr.GetSensorReadingResponseData;
import com.veraxsystems.vxipmi.coding.commands.sdr.ReserveSdrRepository;
import com.veraxsystems.vxipmi.coding.commands.sdr.ReserveSdrRepositoryResponseData;
import com.veraxsystems.vxipmi.coding.commands.sdr.record.FullSensorRecord;
import com.veraxsystems.vxipmi.coding.commands.sdr.record.SensorRecord;
import com.veraxsystems.vxipmi.coding.commands.sdr.record.SensorType;
import com.veraxsystems.vxipmi.coding.commands.sdr.record.SensorUnit;
import com.veraxsystems.vxipmi.coding.commands.session.GetChannelAuthenticationCapabilitiesResponseData;
import com.veraxsystems.vxipmi.coding.payload.CompletionCode;
import com.veraxsystems.vxipmi.coding.payload.lan.IPMIException;
import com.veraxsystems.vxipmi.coding.protocol.AuthenticationType;
import com.veraxsystems.vxipmi.coding.security.CipherSuite;
import com.veraxsystems.vxipmi.common.TypeConverter;

public class IpmiDiscoverAgent extends DiscoverAgent {

    private Handle jrdsHandle;
    private final Set<FullSensorRecord> sdr = new HashSet<>();

    public IpmiDiscoverAgent() {
        super("IPMI", IpmiProbe.class);
    }

    @Override
    public void addProbe(JrdsElement hostElement, ProbeDescSummary summary,
            HttpServletRequest request) {
        String sensorTypeSpecific = summary.specifics.get("SensorType");
        if(sensorTypeSpecific == null || sensorTypeSpecific.trim().isEmpty()) {
            return;
        }
        int sensorType = Util.parseStringNumber(sensorTypeSpecific, -1);
        SensorType type = SensorType.parseInt(sensorType);

        String BaseUnitSpecific = summary.specifics.get("BaseUnit");
        if(BaseUnitSpecific == null || BaseUnitSpecific.trim().isEmpty()) {
            return;
        }
        SensorUnit sensorUnit = SensorUnit.valueOf(BaseUnitSpecific);
        for(FullSensorRecord i: sdr) {
            if(i.getSensorType() != type || i.getSensorBaseUnit() != sensorUnit) {
                continue;
            }
            JrdsElement cnxElem = hostElement.addElement("probe", "type=" + summary.name, "label=" + i.getName().trim());
            cnxElem.addElement("attr", "name=index").setTextContent(Integer.toString(i.getId()));
        }
    }

    @Override
    public List<FieldInfo> getFields() {
        FieldInfo bmc = new FieldInfo();
        bmc.dojoType = DojoType.TextBox;
        bmc.id = "discoverIpmiBmcName";
        bmc.label = "IPMI bmc address";
        bmc.value = "";

        FieldInfo username = new FieldInfo();
        username.dojoType = DojoType.TextBox;
        username.id = "discoverIpmiUsername";
        username.label = "IPMI account";
        username.value = "admin";

        FieldInfo password = new FieldInfo();
        password.dojoType = DojoType.TextBox;
        password.id = "discoverIpmiPassword";
        password.label = "IPMI password";
        password.value = "";

        return Arrays.asList(bmc, username, password);
    }

    @Override
    public boolean exist(String hostname, HttpServletRequest request) {
        makeHandle(hostname, request);      
        return jrdsHandle != null;
    }

    @Override
    public void addConnection(JrdsElement hostElement,
            HttpServletRequest request) {
        String bmcname = request.getParameter("discoverIpmiBmcName");
        String user = request.getParameter("discoverIpmiUsername");
        String password = request.getParameter("discoverIpmiPassword");

        JrdsElement cnxElem = hostElement.addElement("connection", "type=" + IpmiConnection.class.getCanonicalName());
        cnxElem.addElement("attr", "name=bmcname").setTextContent(bmcname);
        cnxElem.addElement("attr", "name=user").setTextContent(user);
        cnxElem.addElement("attr", "name=password").setTextContent(password);
    }

    @Override
    public boolean isGoodProbeDesc(ProbeDescSummary summary) {
        return true;
    }

    @Override
    public void discoverPre(String hostname, JrdsElement hostEleme,
            Map<String, JrdsDocument> probdescs, HttpServletRequest request) {
        super.discoverPre(hostname, hostEleme, probdescs, request);

        // Id 0 indicates first record in SDR. Next IDs can be retrieved from
        // records - they are organized in a list and there is no BMC command to
        // get all of them.
        MutableInteger nextRecId = new MutableInteger(0);

        // Some BMCs allow getting sensor records without reservation, so we try
        // to do it that way first
        int reservationId = 0;
        int lastReservationId = -1;

        // We get sensor data until we encounter ID = 65535 which means that
        // this record is the last one.
        while (nextRecId.value < Handle.MAX_REPO_RECORD_ID) {

            SensorRecord record = null;

            try {
                // Populate the sensor record and get ID of the next record in
                // repository (see #getSensorData for details).
                record = jrdsHandle.getSensorData(reservationId, nextRecId);
                String name;
                switch (record.getClass().getName()){
                case "com.veraxsystems.vxipmi.coding.commands.sdr.record.ManagementControllerDeviceLocatorRecord":
                    name = ((com.veraxsystems.vxipmi.coding.commands.sdr.record.ManagementControllerDeviceLocatorRecord) record).getName();
                    break;
                case "com.veraxsystems.vxipmi.coding.commands.sdr.record.CompactSensorRecord":
                    name = ((com.veraxsystems.vxipmi.coding.commands.sdr.record.CompactSensorRecord) record).getName();
                    break;
                case "com.veraxsystems.vxipmi.coding.commands.sdr.record.FullSensorRecord":
                    FullSensorRecord fsr = (FullSensorRecord) record;
                    name = fsr.getName().trim();
                    log(Level.DEBUG, "%s sensor number: %s", name, TypeConverter.byteToInt(fsr.getSensorNumber()));
                    log(Level.DEBUG, "%s rate unit: %s", name, fsr.getRateUnit().toString());
                    log(Level.DEBUG, "%s.getSensorType: %s", name, fsr.getSensorType().toString());
                    log(Level.DEBUG, "%s.getSensorBaseUnit: %s", name, fsr.getSensorBaseUnit().toString());
                    log(Level.DEBUG, "%s.getNominalReading: %f", name, fsr.getNominalReading());
                    int recordReadingId = TypeConverter.byteToInt(fsr.getSensorNumber());
                    GetSensorReadingResponseData data2 = jrdsHandle.getSensorReading(recordReadingId);
                    if(data2.isSensorStateValid()) {
                        sdr.add(fsr);                            
                    }
                    break;
                case "com.veraxsystems.vxipmi.coding.commands.sdr.record.OemRecord":
                    name = "";
                    //log(Level.DEBUG, "%s", new String(((com.veraxsystems.vxipmi.coding.commands.sdr.record.OemRecord)record).getOemData()));
                    break;
                case "com.veraxsystems.vxipmi.coding.commands.sdr.record.FruDeviceLocatorRecord":
                    name = ((com.veraxsystems.vxipmi.coding.commands.sdr.record.FruDeviceLocatorRecord) record).getName();
                    break;
                default:
                    name="";
                }
                log(Level.TRACE, "%s: %d %s", record, record.getId(), name);
            } catch (IPMIException e) {
                if(e.getCompletionCode() == CompletionCode.ReservationCanceled) {
                    lastReservationId = reservationId;
                    // If the cause of the failure was canceling of the
                    // reservation, we get new reservationId and retry. This can
                    // happen many times during getting all sensors, since BMC can't
                    // manage parallel sessions and invalidates old one if new one
                    // appears.
                    try {
                        reservationId = ((ReserveSdrRepositoryResponseData) jrdsHandle
                                .sendMessage(new ReserveSdrRepository(IpmiVersion.V20, jrdsHandle.getCipherSuite(),
                                        AuthenticationType.RMCPPlus))).getReservationId();
                    } catch (Exception e1) {
                        log(Level.ERROR, e, "general failure: %s", e.getMessage());
                        break;
                    }               
                    // If getting sensor data failed, we check if it already failed
                    // with this reservation ID.
                    if (lastReservationId == reservationId) {
                        log(Level.ERROR, "%s", e.getMessage());
                        break;
                    }
                    lastReservationId = reservationId;
                } else {
                    log(Level.ERROR, e, "IPMI failure: %s", e.getMessage());
                }
            } catch (Exception e) {
                log(Level.ERROR, e, "general failure: %s", e.getMessage());
                break;
            }
        }
    }

    private void makeHandle(String hostname, HttpServletRequest request) {
        try {
            String bmcname = request.getParameter("discoverIpmiBmcName");
            String user = request.getParameter("discoverIpmiUsername");
            String password = request.getParameter("discoverIpmiPassword");

            IpmiConnector connector = new IpmiConnector(0);
            ConnectionHandle handle = connector.createConnection(InetAddress.getByName(bmcname));
            connector.setTimeout(handle, getTimeout() * 1000);
            CipherSuite cs;
            // Get cipher suites supported by the remote host
            List<CipherSuite> suites = connector.getAllCipherSuites(handle);
            if (suites.size() > 3) {
                cs = suites.get(3);
            } else if (suites.size() > 2) {
                cs = suites.get(2);
            } else if (suites.size() > 1) {
                cs = suites.get(1);
            } else {
                cs = suites.get(0);
            }
            GetChannelAuthenticationCapabilitiesResponseData auth = connector.getChannelAuthenticationCapabilities(handle, cs, PrivilegeLevel.User);
            log(Level.DEBUG, "AuthenticationTypes: %s", auth.getAuthenticationTypes());
            log(Level.DEBUG, "ChannelNumber: %s", auth.getChannelNumber());
            log(Level.DEBUG, "OemId: %s", auth.getOemId());
            log(Level.DEBUG, "AnonymusLoginEnabled: %s", auth.isAnonymusLoginEnabled());
            log(Level.DEBUG, "Ipmiv20Support: %s", auth.isIpmiv20Support());
            log(Level.DEBUG, "KgEnabled: %s", auth.isKgEnabled());
            log(Level.DEBUG, "NonNullUsernamesEnabled: %s", auth.isNonNullUsernamesEnabled());
            log(Level.DEBUG, "NullUsernamesEnabled: %s", auth.isNullUsernamesEnabled());
            log(Level.DEBUG, "PerMessageAuthenticationEnabled: %s", auth.isPerMessageAuthenticationEnabled());
            log(Level.DEBUG, "UserLevelAuthenticationEnabled: %s", auth.isUserLevelAuthenticationEnabled());
            connector.openSession(handle, user, password, "".getBytes());
            jrdsHandle = new Handle(connector, handle);
        } catch (FileNotFoundException e) {
            jrdsHandle = null;
        } catch (UnknownHostException e) {
            jrdsHandle = null;
        } catch (IOException e) {
            jrdsHandle = null;
        } catch (Exception e) {
            jrdsHandle = null;
        }       
    }

    @Override
    public void discoverPost(String hostname, JrdsElement hostEleme,
            Map<String, JrdsDocument> probdescs, HttpServletRequest request) {
        super.discoverPost(hostname, hostEleme, probdescs, request);
        jrdsHandle.connector.tearDown();
    }
}
