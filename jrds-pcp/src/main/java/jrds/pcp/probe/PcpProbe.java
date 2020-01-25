package jrds.pcp.probe;

import java.util.Map;

import jrds.ProbeConnected;

public class PcpProbe extends ProbeConnected<String, Number, PcpConnexion> {

    public PcpProbe() {
        super(PcpConnexion.class.getName());
    }

    @Override
    public Map<String, Number> getNewSampleValuesConnected(PcpConnexion cnx) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getSourceType() {
        // TODO Auto-generated method stub
        return null;
    }

}
