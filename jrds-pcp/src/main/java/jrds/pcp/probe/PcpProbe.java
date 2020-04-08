package jrds.pcp.probe;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.slf4j.event.Level;

import fr.jrds.pcp.PCPException;
import jrds.ProbeConnected;

public class PcpProbe extends ProbeConnected<String, Number, PcpConnexion> {

    public PcpProbe() {
        super(PcpConnexion.class.getName());
    }

    @Override
    public Map<String, Number> getNewSampleValuesConnected(PcpConnexion cnx) {
        try {
            return cnx.getValue(getCollectMapping().keySet());
        } catch (InterruptedException e) {
            log(Level.WARN, e, "Collect interrupted");
            return Collections.emptyMap();
        } catch (PCPException e) {
            log(Level.ERROR, e, "PCP protocol error: %s", e);
            return Collections.emptyMap();
        } catch (IOException e) {
            log(Level.ERROR, e, "IO exception error: %s", e);
            return Collections.emptyMap();
        }
    }

    @Override
    public String getSourceType() {
        return "PCP";
    }

}
