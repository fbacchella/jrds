package jrds.configuration;

import jrds.ProbeDesc;
import jrds.PropertiesManager;
import jrds.factories.xml.JrdsNode;

public class GeneratorHelper {
    static final public ProbeDesc getProbeDesc(JrdsNode node) throws Exception {
        ProbeDescBuilder builder = new ProbeDescBuilder();
        builder.setPm(new PropertiesManager());
        return builder.makeProbeDesc(node);
    }
}
