package jrds.configuration;

import jrds.PropertiesManager;
import jrds.factories.xml.JrdsDocument;
import jrds.objects.probe.ProbeDesc;

public class GeneratorHelper {
    static final public ProbeDesc getProbeDesc(JrdsDocument node) throws Exception {
        ProbeDescBuilder builder = new ProbeDescBuilder();
        builder.setPm(new PropertiesManager());
        return builder.makeProbeDesc(node);
    }
}
