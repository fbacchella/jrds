package jrds.configuration;

import jrds.ProbeDesc;
import jrds.PropertiesManager;
import jrds.factories.xml.JrdsDocument;

public class GeneratorHelper {
    static final public ProbeDesc<?> getProbeDesc(JrdsDocument node) throws Exception {
        ProbeDescBuilder builder = new ProbeDescBuilder();
        builder.setPm(new PropertiesManager());
        return builder.makeProbeDesc(node);
    }
}
