package jrds.webapp;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import jrds.HostsList;
import jrds.PropertiesManager;
import jrds.Tools;
import jrds.mockobjects.GetMoke;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestParamsBean {
    static final private Logger logger = Logger.getLogger(TestParamsBean.class);

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
    
    @BeforeClass
    static public void configure() throws Exception {
        Tools.configure();
        Tools.setLevel(logger, Level.TRACE, "jrds.webapp.ParamsBean");
    }
    
    @Test
    public void testone() throws IOException {
        PropertiesManager pm = Tools.makePm(testFolder, "tabs=filtertab,customgraph,@,sumstab,servicestab,viewstab,hoststab,tagstab,adminTab");
        HostsList hl = new HostsList(pm);
        Map<String, String[]> props = Collections.emptyMap();
        @SuppressWarnings("unused")
        ParamsBean pb = new ParamsBean(GetMoke.getRequest(props, "Dummy//Host", "MockGraphInstance", "detailInfo"), hl, "host", "graphname", "detail");

    }
}
