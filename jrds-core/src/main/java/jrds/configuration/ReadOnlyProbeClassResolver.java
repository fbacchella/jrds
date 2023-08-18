package jrds.configuration;

import java.util.Collections;
import java.util.Map;

import jrds.Probe;
import jrds.factories.ProbeBean;
import jrds.probe.IndexedProbe;
import lombok.Getter;
import lombok.Setter;

public class ReadOnlyProbeClassResolver extends ProbeClassResolver {

    public static class DummyProbe extends Probe<Object, Object> {

        @Override
        public Map<Object, Object> getNewSampleValues() {
            return Collections.emptyMap();
        }

        @Override
        public String getSourceType() {
            return "DummyProbe";
        }

        public Boolean configure() {
            return true;
        }

        @Override
        public boolean checkStore() {
            return true;
        }

    }

    @ProbeBean({"index", "pattern" })
    public static class DummyProbeIndexed extends DummyProbe implements IndexedProbe {

        @Getter @Setter
        private String index = "NONE";

        @Getter @Setter
        private String pattern = "pattern";

        @Override
        public String getIndexName() {
            return index;
        }

        public Boolean configure(String index) {
            this.index = index;
            return true;
        }

    }

    public ReadOnlyProbeClassResolver(ClassLoader classLoader) {
        super(classLoader);
    }

    @SuppressWarnings("unchecked")
    public Class<? extends Probe<?, ?>> getClassByName(String className) throws ClassNotFoundException {
        Class<? extends Probe<?, ?>> originalClass = (Class<? extends Probe<?, ?>>) classLoader.loadClass(className);
        if (IndexedProbe.class.isAssignableFrom(originalClass)) {
            return DummyProbeIndexed.class;
        } else {
            return DummyProbe.class;
        }
    }

}
