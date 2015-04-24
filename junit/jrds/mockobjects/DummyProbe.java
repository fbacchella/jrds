package jrds.mockobjects;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import jrds.HostInfo;
import jrds.Probe;
import jrds.ProbeDesc;
import jrds.Tools;
import jrds.starter.HostStarter;
import jrds.store.RrdDbStoreFactory;

import org.junit.BeforeClass;
import org.rrd4j.DsType;

public class DummyProbe extends Probe<String, Number> {

    Class<? extends Probe<?,?>> originalProbe;

    public void configure(Class<? extends Probe<?,?>> originalProbe) throws InvocationTargetException {
        this.originalProbe = originalProbe;
        configure();
    }

    public void configure() throws InvocationTargetException {
        Map<String, String> empty = Collections.emptyMap();
        setMainStore( new RrdDbStoreFactory(), empty);
        ProbeDesc pd = new ProbeDesc();
        pd.setName("DummyProbe");
        pd.setProbeName("dummyprobe");
        setPd(pd);
        if(getHost() == null) {
            HostInfo host = new HostInfo("DummyHost");
            host.setHostDir(new File("tmp"));
            setHost(new HostStarter(host));
        }
        Map<String, Object> dsMap = new HashMap<String, Object>();
        dsMap.put("dsName", "ds0");
        dsMap.put("dsType", DsType.COUNTER);
        dsMap.put("collectKey", "/jrdsstats/stat[@key='a']/@value");
        pd.add(dsMap);
        dsMap = new HashMap<String, Object>();
        dsMap.put("dsName", "ds1");
        dsMap.put("dsType", DsType.COUNTER);
        dsMap.put("collectKey", "/jrdsstats/stat[@key='b']/@value");
        pd.add(dsMap);
        dsMap = new HashMap<String, Object>();
        dsMap.put("dsName", "ds2");
        dsMap.put("dsType", DsType.COUNTER);
        pd.add(dsMap);
    }

    @Override
    public Map<String, Number> getNewSampleValues() {
        return Collections.emptyMap();
    }

    @Override
    public String getSourceType() {
        return getClass().getName();
    }

    @BeforeClass
    static public void prepare() throws IOException, ParserConfigurationException {
        Tools.configure();
        Tools.prepareXml();
    }

}
