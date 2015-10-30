package jrds.probe.ipmi;

import java.util.Collections;
import java.util.Map;

import jrds.ProbeConnected;
import jrds.Util;
import jrds.factories.ProbeBean;
import jrds.factories.ProbeMeta;
import jrds.probe.IndexedProbe;
import jrds.probe.ipmi.Handle.MutableInteger;

import org.apache.log4j.Level;

import com.veraxsystems.vxipmi.coding.commands.sdr.GetSensorReadingResponseData;
import com.veraxsystems.vxipmi.coding.commands.sdr.record.FullSensorRecord;
import com.veraxsystems.vxipmi.coding.commands.sdr.record.SensorRecord;
import com.veraxsystems.vxipmi.coding.payload.CompletionCode;
import com.veraxsystems.vxipmi.coding.payload.lan.IPMIException;
import com.veraxsystems.vxipmi.common.TypeConverter;


@ProbeMeta(
        timerStarter=jrds.probe.ipmi.IpmiConnectorStarter.class,
        discoverAgent=IpmiDiscoverAgent.class
        )
@ProbeBean({"index"})
public class IpmiProbe extends
ProbeConnected<String, Number, IpmiConnection> implements IndexedProbe {

    private int indexKey;

    public IpmiProbe() {
        super(IpmiConnection.class.getName());
    }

    @Override
    public Map<String, Number> getNewSampleValuesConnected(
            IpmiConnection cnx) {

        Handle handle = cnx.getConnection();

        // Some BMCs allow getting sensor records without reservation, so we try
        // to do it that way first
        int reservationId = 0;
        int lastReservationId = -1;

        Double value = Double.NaN;

        // Need to retry because of cancelled reservation
        while(value.isNaN() && isCollectRunning()) {
            try {
                // Populate the sensor record and get ID of the next record in
                // repository (see #getSensorData for details).
                if ( ! isCollectRunning()) {
                    break;
                }
                SensorRecord record = handle.getSensorData(reservationId, new MutableInteger(indexKey));

                FullSensorRecord fsr = (FullSensorRecord) record;
                int recordReadingId = TypeConverter.byteToInt(fsr.getSensorNumber());

                if ( ! isCollectRunning()) {
                    break;
                }
                GetSensorReadingResponseData data2 = handle.getSensorReading(recordReadingId);
                log(Level.TRACE, "read %f for %s", data2.getSensorReading(fsr), getLabel());
                value = data2.getSensorReading(fsr);
            } catch (InterruptedException e) {
                break;
            } catch (IPMIException e) {
                if(e.getCompletionCode() == CompletionCode.ReservationCanceled) {
                    lastReservationId = reservationId;
                    // If the cause of the failure was canceling of the
                    // reservation, we get new reservationId and retry. This can
                    // happen many times during getting all sensors, since BMC can't
                    // manage parallel sessions and invalidates old one if new one
                    // appears.
                    try {
                        if ( ! isCollectRunning()) {
                            break;
                        }
                        log(Level.DEBUG, "reservation cancelled");
                        reservationId = handle.reserveSdrRepository();
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
                    break;
                }
            } catch (Exception e) {
                log(Level.ERROR, e, "general failure: %s", e.getMessage());
                break;
            }
        }
        if(! value.isNaN()) {
            String dsName = getPd().getCollectMapping().values().iterator().next();
            return Collections.singletonMap(dsName, (Number) value);            
        } else {
            return Collections.emptyMap();            
        }
    }

    @Override
    public String getSourceType() {
        return "IPMI";
    }

    @Override
    public String getIndexName() {
        return Integer.toString(indexKey);
    }

    /**
     * @return the indexKey
     */
    public String getIndex() {
        return Integer.toString(indexKey);
    }

    /**
     * @param indexKey the indexKey to set
     */
    public void setIndex(String indexKey) {
        this.indexKey = Util.parseStringNumber(indexKey, -1);
    }

}
