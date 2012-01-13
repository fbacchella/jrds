package jrds.mockobjects;

import jrds.factories.ProbeBean;

@ProbeBean({"hostInfo"})
public class MokeProbeBean<A,B> extends MokeProbe<A, B> {
    String HostInfo;

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
