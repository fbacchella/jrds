package jrds.mockobjects;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import jrds.HostInfo;
import jrds.Probe;
import jrds.ProbeDesc;
import jrds.PropertiesManager;
import jrds.Tools;
import jrds.starter.HostStarter;
import jrds.store.StoreFactory;

import org.junit.rules.TemporaryFolder;

public class GenerateProbe {
    static public final String FACTORYCONFIG = "factoryconfig";

    public static class ChainedMap<ValueClass> extends HashMap<String, ValueClass> {
        public static final class ChainedProperties extends ChainedMap<String> {

        };

        private ChainedMap() {
            super();
        }

        private ChainedMap(int size) {
            super(size);
        }

        public static <ValueClass> ChainedMap<ValueClass> start() {
            return new ChainedMap<ValueClass>();
        }

        public static <ValueClass> ChainedMap<ValueClass> start(int size) {
            return new ChainedMap<ValueClass>(size);
        }

        public ChainedMap<ValueClass> set(String key, ValueClass value) {
            put(key, value);
            return this;
        }

        public ChainedMap<ValueClass> set(Class<?> clazz, ValueClass value) {
            put(clazz.getCanonicalName(), value);
            return this;
        }

        public ValueClass get(Class<ValueClass> c) {
            return get(c.getCanonicalName());
        }

    }

    static public class EmptyProbe  extends Probe<String,Number> {
        @Override
        public Map<String, Number> getNewSampleValues() {
            return Collections.emptyMap();
        }

        @Override
        public String getSourceType() {
            return "fullmoke";
        }
    }

    @SuppressWarnings("unchecked")
    public static final Probe<String, Number> quickProbe(TemporaryFolder folder, ChainedMap<Object>... args) throws Exception {
        ChainedMap<Object> arg = new ChainedMap<Object>(0);
        for(int i=0; i< args.length; i++) {
            arg.putAll(args[i]);
        }
        Class<?> probeClass = (Class<Probe<?, ?>>) arg.get(Probe.class.getCanonicalName());
        if(probeClass == null) {
            probeClass = EmptyProbe.class;
        }
        Probe<?,?> probe  = (Probe<?, ?>) probeClass.getConstructor().newInstance();
        return (Probe<String, Number>) fillProbe(probe, folder, arg );
    }

    public static final Probe<?, ?> fillProbe(Probe<?,?> p, TemporaryFolder folder, ChainedMap<Object> args) throws Exception {

        PropertiesManager pm = (PropertiesManager) args.get(PropertiesManager.class.getCanonicalName());
        if(pm == null) {
            pm = Tools.makePm(folder);
        }

        ProbeDesc pd = (ProbeDesc) args.get(ProbeDesc.class.getCanonicalName());
        if(pd == null) {
            pd = new ProbeDesc();
        }
        p.setPd(pd);

        HostStarter hs = (HostStarter) args.get(HostStarter.class);
        if(hs == null) {
            HostInfo hi = (HostInfo) args.get(HostInfo.class);
            if(hi == null) {
                hi = new HostInfo("localhost");
                hi.setHostDir(folder.newFolder());
                hs = new HostStarter(hi);
            }
        }
        p.setHost(hs);

        String name = (String) args.get("name");
        if(name == null) {
            name = "EmptyProbe";
        }
        p.setName(name);
        p.setStep(pm.step);
        p.setTimeout(pm.timeout);

        StoreFactory sf = (StoreFactory) args.get(StoreFactory.class.getCanonicalName());
        if(sf == null) {
            sf = pm.defaultStore;
        }
        sf.configureStore(pm, new Properties());
        sf.start();

        @SuppressWarnings("unchecked")
        Map<String, String> factoryArgs = (Map<String, String>) args.get(FACTORYCONFIG);
        if(factoryArgs == null) {
            factoryArgs = Collections.emptyMap();
        }
        p.setMainStore(sf, factoryArgs);

        return p;
    }
}
