package jrds.mockobjects;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.BeforeClass;
import org.rrd4j.DsType;

import jrds.HostInfo;
import jrds.Probe;
import jrds.ProbeDesc;
import jrds.Tools;
import jrds.starter.HostStarter;
import jrds.store.RrdDbStoreFactory;

public class DummyProbe extends Probe<String, Number> {

    Class<? extends Probe<?, ?>> originalProbe;

    public void configure(Class<? extends Probe<?, ?>> originalProbe) throws InvocationTargetException {
        this.originalProbe = originalProbe;
        configure();
    }

    public void configure() throws InvocationTargetException {
        Map<String, String> empty = Collections.emptyMap();
        setMainStore(new RrdDbStoreFactory(), empty);
        ProbeDesc<String> pd = new ProbeDesc<>();
        pd.setName("DummyProbe");
        pd.setProbeName("dummyprobe");
        setPd(pd);
        if(getHost() == null) {
            HostInfo host = new HostInfo("DummyHost");
            host.setHostDir(new File("tmp"));
            setHost(new HostStarter(host));
        }
        pd.add(ProbeDesc.getDataSourceBuilder("ds0", DsType.COUNTER).setCollectKey("/jrdsstats/stat[@key='a']/@value"));
        pd.add(ProbeDesc.getDataSourceBuilder("ds1", DsType.COUNTER).setCollectKey("/jrdsstats/stat[@key='b']/@value"));
        pd.add(ProbeDesc.getDataSourceBuilder("ds2", DsType.COUNTER));
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
    static public void prepare() throws ParserConfigurationException {
        Tools.configure();
        Tools.prepareXml();
    }

}
