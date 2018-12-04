package jrds.mockobjects;

import jrds.ProbeDesc;
import jrds.factories.ProbeBean;

@ProbeBean({ "hostInfo" })
public class MokeProbeBean extends MokeProbe<String, Number> {
    String HostInfo;

    public MokeProbeBean() {
        super();
    }

    public MokeProbeBean(ProbeDesc<String> pd) {
        super(pd);
    }

    public MokeProbeBean(String probeType) {
        super(probeType);
    }

    /**
     * @return the hostInfo
     */
    public String getHostInfo() {
        return HostInfo;
    }

    /**
     * @param hostInfo the hostInfo to set
     */
    public void setHostInfo(String hostInfo) {
        HostInfo = hostInfo;
    }
}
