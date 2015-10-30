package jrds.probe.ipmi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.veraxsystems.vxipmi.api.async.ConnectionHandle;
import com.veraxsystems.vxipmi.api.sync.IpmiConnector;
import com.veraxsystems.vxipmi.coding.commands.IpmiCommandCoder;
import com.veraxsystems.vxipmi.coding.commands.IpmiVersion;
import com.veraxsystems.vxipmi.coding.commands.PrivilegeLevel;
import com.veraxsystems.vxipmi.coding.commands.ResponseData;
import com.veraxsystems.vxipmi.coding.commands.sdr.GetSdr;
import com.veraxsystems.vxipmi.coding.commands.sdr.GetSdrResponseData;
import com.veraxsystems.vxipmi.coding.commands.sdr.GetSensorReading;
import com.veraxsystems.vxipmi.coding.commands.sdr.GetSensorReadingResponseData;
import com.veraxsystems.vxipmi.coding.commands.sdr.ReserveSdrRepository;
import com.veraxsystems.vxipmi.coding.commands.sdr.ReserveSdrRepositoryResponseData;
import com.veraxsystems.vxipmi.coding.commands.sdr.record.SensorRecord;
import com.veraxsystems.vxipmi.coding.commands.session.GetChannelAuthenticationCapabilitiesResponseData;
import com.veraxsystems.vxipmi.coding.payload.CompletionCode;
import com.veraxsystems.vxipmi.coding.payload.lan.IPMIException;
import com.veraxsystems.vxipmi.coding.protocol.AuthenticationType;
import com.veraxsystems.vxipmi.coding.security.CipherSuite;
import com.veraxsystems.vxipmi.common.TypeConverter;

public class Handle {

    public static final class MutableInteger {
        public int value;
        MutableInteger(int value) {
            this.value = value;
        }
    }

    static final int MAX_REPO_RECORD_ID = 65535;

    // Size of the initial GetSdr message to get record header and size
    private static final int INITIAL_CHUNK_SIZE = 8;
    /**
     * Chunk size depending on buffer size of the IPMI server. Bigger values will improve performance. If server is
     * returning "Cannot return number of requested data bytes." error during GetSdr command, CHUNK_SIZE should be
     * decreased.
     */
    private static final int CHUNK_SIZE = 16;

    /**
     * Size of SDR record header
     */
    private static final int HEADER_SIZE = 5;

    final ConnectionHandle handle;
    final IpmiConnector connector;
    final Map<String, Number> values;

    Handle(IpmiConnector connector, ConnectionHandle handle) {
        this.handle = handle;
        this.connector = connector;
        this.values = new HashMap<>();
    }

    CipherSuite getCipherSuite() {
        return handle.getCipherSuite();
    }

    int getHandle() {
        return handle.getHandle();
    }

    PrivilegeLevel getPrivilegeLevel() {
        return handle.getPrivilegeLevel();
    }

    void setCipherSuite(CipherSuite cipherSuite) {
        handle.setCipherSuite(cipherSuite);
    }

    void setPrivilegeLevel(PrivilegeLevel privilegeLevel) {
        handle.setPrivilegeLevel(privilegeLevel);
    }

    List<CipherSuite> getAvailableCipherSuites() throws Exception {
        return connector.getAvailableCipherSuites(handle);
    }

    GetChannelAuthenticationCapabilitiesResponseData getChannelAuthenticationCapabilities(
            CipherSuite cipherSuite,
            PrivilegeLevel requestedPrivilegeLevel) throws Exception {
        return connector.getChannelAuthenticationCapabilities(handle,
                cipherSuite, requestedPrivilegeLevel);
    }

    void openSession(String username,
            String password, byte[] bmcKey) throws Exception {
        connector.openSession(handle, username, password, bmcKey);
    }

    ResponseData sendMessage(IpmiCommandCoder arg1)
            throws Exception {
        return connector.sendMessage(handle, arg1);
    }

    void setTimeout(int timeout) {
        connector.setTimeout(handle, timeout);
    }

    void tearDown() {
        connector.tearDown();
    }

    SensorRecord getSensorData(int reservationId, MutableInteger nextRecId) throws Exception {
        try {
            // BMC capabilities are limited - that means that sometimes the
            // record size exceeds maximum size of the message. Since we don't
            // know what is the size of the record, we try to get
            // whole one first
            GetSdrResponseData data = (GetSdrResponseData) connector.sendMessage(handle, new GetSdr(IpmiVersion.V20,
                    handle.getCipherSuite(), AuthenticationType.RMCPPlus, reservationId, nextRecId.value));
            // If getting whole record succeeded we create SensorRecord from
            // received data...
            SensorRecord sensorDataToPopulate = SensorRecord.populateSensorRecord(data.getSensorRecordData());
            // ... and update the ID of the next record
            nextRecId.value = data.getNextRecordId();
            return sensorDataToPopulate;
        } catch (IPMIException e) {

            // The following error codes mean that record is too large to be
            // sent in one chunk. This means we need to split the data in
            // smaller parts.
            if (e.getCompletionCode() == CompletionCode.CannotRespond
                    || e.getCompletionCode() == CompletionCode.UnspecifiedError) {
                // First we get the header of the record to find out its size.
                GetSdrResponseData data = (GetSdrResponseData) connector.sendMessage(handle, new GetSdr(
                        IpmiVersion.V20, handle.getCipherSuite(), AuthenticationType.RMCPPlus, reservationId,
                        nextRecId.value, 0, INITIAL_CHUNK_SIZE));
                // The record size is 5th byte of the record. It does not take
                // into account the size of the header, so we need to add it.
                int recSize = TypeConverter.byteToInt(data.getSensorRecordData()[4]) + HEADER_SIZE;
                int read = INITIAL_CHUNK_SIZE;

                byte[] result = new byte[recSize];

                System.arraycopy(data.getSensorRecordData(), 0, result, 0, data.getSensorRecordData().length);

                // We get the rest of the record in chunks (watch out for
                // exceeding the record size, since this will result in BMC's
                // error.
                while (read < recSize) {
                    int bytesToRead = CHUNK_SIZE;
                    if (recSize - read < bytesToRead) {
                        bytesToRead = recSize - read;
                    }
                    GetSdrResponseData part = (GetSdrResponseData) connector.sendMessage(handle, new GetSdr(
                            IpmiVersion.V20, handle.getCipherSuite(), AuthenticationType.RMCPPlus, reservationId,
                            nextRecId.value, read, bytesToRead));

                    System.arraycopy(part.getSensorRecordData(), 0, result, read, bytesToRead);

                    read += bytesToRead;
                }

                // Finally we populate the sensor record with the gathered
                // data...
                SensorRecord sensorDataToPopulate = SensorRecord.populateSensorRecord(result);
                // ... and update the ID of the next record
                nextRecId.value = data.getNextRecordId();
                return sensorDataToPopulate;
            } else {
                throw e;
            }
        }
    }

    public void closeConnection() {
        connector.closeConnection(handle);
    }

    public GetSensorReadingResponseData getSensorReading(int recordReadingId) throws Exception {
        return (GetSensorReadingResponseData) sendMessage(new GetSensorReading(IpmiVersion.V20, getCipherSuite(),
                AuthenticationType.RMCPPlus, recordReadingId));
    }

    public int reserveSdrRepository() throws Exception {
        return ((ReserveSdrRepositoryResponseData) sendMessage(new ReserveSdrRepository(IpmiVersion.V20, handle.getCipherSuite(),
                AuthenticationType.RMCPPlus))).getReservationId();    
    }

}
