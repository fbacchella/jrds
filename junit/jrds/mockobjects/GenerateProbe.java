package jrds.mockobjects;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jrds.HostInfo;
import jrds.Probe;
import jrds.ProbeDesc;
import jrds.starter.HostStarter;
import jrds.store.AbstractStoreFactory;

import org.junit.rules.TemporaryFolder;

public class GenerateProbe {
    static public final String FACTORYCONFIG = "factoryconfig";
    
    public static final class ChainedMap extends HashMap<String, Object> {
        private ChainedMap() {
            super();
        }

        private ChainedMap(int size) {
            super(size);
        }

        public static ChainedMap start() {
            return new ChainedMap();
        }

        public static ChainedMap start(int size) {
            return new ChainedMap(size);
        }

        public ChainedMap set(String key, Object value) {
            put(key, value);
            return this;
        }

        public ChainedMap set(Class clazz, Object value) {
            put(clazz.getCanonicalName(), value);
            return this;
        }

        @SuppressWarnings("unchecked")
        public <ValueType> ValueType get(Class<ValueType> c) {
            return (ValueType) get(c.getCanonicalName());
        }

        @SuppressWarnings("unchecked")
        public <ValueType> ValueType get(String key, Class<ValueType> c) {
            return (ValueType) get(key);
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
    public static final Probe<String, Number> quickProbe(TemporaryFolder folder) throws Exception {
        return (Probe<String, Number>) fillProbe(new EmptyProbe(), folder, new ChainedMap(0) );
    }
    
    public static final Probe<?, ?> fillProbe(Probe<?,?> p, TemporaryFolder folder, ChainedMap args) throws Exception {
        ProbeDesc pd = args.get(ProbeDesc.class);
        if(pd == null) {
            pd = new ProbeDesc();
        }
        p.setPd(pd);
        
        HostStarter hs = args.get(HostStarter.class);
        if(hs == null) {
            HostInfo hi = args.get(HostInfo.class);
            if(hi == null) {
                hi = new HostInfo("localhost");
                hi.setHostDir(folder.newFolder());
                hs = new HostStarter(hi);
            }
        }
        p.setHost(hs);
        
        String name = args.get("name", String.class);
        if(name == null) {
            name = "EmptyProbe";
        }
        p.setName(name);
        p.getPd().setName(name);
        
        AbstractStoreFactory<?> sf = args.get(AbstractStoreFactory.class);
        @SuppressWarnings("unchecked")
        Map<String, String> factoryArgs = (Map<String, String>) args.get(FACTORYCONFIG);
        if(sf == null) {
            sf = new jrds.store.RrdDbStoreFactory();
        }
        if(factoryArgs == null) {
            factoryArgs = Collections.emptyMap();
        }
        p.setMainStore(sf, factoryArgs);
       
          return p;
    }
}
