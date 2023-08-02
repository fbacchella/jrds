package jrds.webapp;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.event.Level;

import jrds.HostsList;
import jrds.Log4JRule;
import jrds.PropertiesManager;
import jrds.Tools;
import jrds.mockobjects.GetMoke;

public class TestParamsBean {

    @Rule
    public final TemporaryFolder testFolder = new TemporaryFolder();

    @Rule
    public final Log4JRule logrule = new Log4JRule(this);

    @BeforeClass
    static public void configure() {
        Tools.configure();
    }

    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, "jrds.webapp.ParamsBean");
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
