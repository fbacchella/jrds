package jrds.store;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import jrds.Probe;

public class CacheStoreFactory extends AbstractStoreFactory<CacheStore> implements SampleCacheMBean  {
    private final Map<String, Map<String, Map<String, Number>>> cache = new HashMap<String, Map<String, Map<String, Number>>>();
    @SuppressWarnings("unused")
    private final CacheMBean mbean;
    private final class CacheMBean extends StandardMBean implements SampleCacheMBean {
        protected CacheMBean() {
            super(SampleCacheMBean.class, false);
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            try {
                ObjectName name = new ObjectName("jrds:type=SampleCache");
                mbs.registerMBean(this, name);         
            } catch (Exception e) {
                throw new RuntimeException("Can't register cache mbean", e);
            }
        }

        @Override
        public Map<String, Number> getValues(String host, String probe) {
            return CacheStoreFactory.this.getValues(host, probe);
        } 
    }

    public CacheStoreFactory() {
        mbean = new CacheMBean();
    }

    @Override
    public CacheStore create(Probe<?, ?> p) {
        String hostname = p.getHost().getName();
        String probeName = p.getName();
        if(! cache.containsKey(hostname))
            cache.put(hostname, new HashMap<String, Map<String, Number>>());
        Map<String, Map<String, Number>> probes = cache.get(hostname);
        if(! probes.containsKey(probeName))
            probes.put(probeName, new HashMap<String, Number>(p.getPd().getDsDefs().length));

        return new CacheStore(p, probes.get(probeName));
    }

    @Override
    public Map<String, Number> getValues(String host, String probe) {
        return cache.get(host).get(probe);
    }

    @Override
    public void stop() {
        cache.clear();
    }

}
