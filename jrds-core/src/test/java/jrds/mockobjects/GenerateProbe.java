package jrds.mockobjects;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.rules.TemporaryFolder;

import jrds.HostInfo;
import jrds.Probe;
import jrds.ProbeDesc;
import jrds.PropertiesManager;
import jrds.Tools;
import jrds.starter.HostStarter;
import jrds.store.StoreFactory;

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

    static public class EmptyProbe<T, V> extends Probe<T, V> {
        @Override
        public Map<T, V> getNewSampleValues() {
            return Collections.emptyMap();
        }

        @Override
        public String getSourceType() {
            return "fullmoke";
        }
    }

    @SafeVarargs
    @SuppressWarnings({ "unchecked"})
    public static final <P extends Probe<T, V>, T, V> P quickProbe(TemporaryFolder folder, ChainedMap<Object>... args) throws Exception {
        ChainedMap<Object> arg = new ChainedMap<Object>(0);
        for (ChainedMap<Object> objectChainedMap : args) {
            arg.putAll(objectChainedMap);
        }
        Class<P> probeClass = (Class<P>) arg.get(Probe.class.getCanonicalName());
        P probe;
        if(probeClass != null) {
            probe = (P) probeClass.getConstructor().newInstance();
        } else {
            probe = (P) new EmptyProbe<T,V>();
        }
        return fillProbe(probe, folder, arg);
    }

    @SuppressWarnings({ "unlikely-arg-type", "unchecked" })
    public static final <P extends Probe<T, V>, T, V> P fillProbe(P p, TemporaryFolder folder, ChainedMap<Object> args) throws Exception {

        PropertiesManager pm = (PropertiesManager) args.get(PropertiesManager.class.getCanonicalName());
        if(pm == null) {
            pm = Tools.makePm(folder);
        }

        ProbeDesc<T> pd = (ProbeDesc<T>) args.get(ProbeDesc.class.getCanonicalName());
        if(pd == null) {
            pd = (ProbeDesc<T>) args.get(ProbeDesc.class);
        }
        if(pd == null) {
            pd = new ProbeDesc<T>();
        }
        p.setPd(pd);

        HostStarter hs = (HostStarter) args.get(HostStarter.class.getCanonicalName());
        if(hs == null) {
            HostInfo hi = (HostInfo) args.get(HostInfo.class.getCanonicalName());
            if(hi == null) {
                hi = new HostInfo("localhost");
                hi.setHostDir(folder.newFolder());
                hs = new HostStarter(hi);
            }
        }
        hs.configureStarters(pm);
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

        Map<String, String> factoryArgs = (Map<String, String>) args.get(FACTORYCONFIG);
        if(factoryArgs == null) {
            factoryArgs = Collections.emptyMap();
        }
        p.setMainStore(sf, factoryArgs);
        p.configureStarters(pm);

        return p;
    }
}
