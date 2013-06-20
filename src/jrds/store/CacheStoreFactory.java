package jrds.store;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import jrds.Probe;

public class CacheStoreFactory extends AbstractStoreFactory<CacheStore> implements SampleCacheMBean  {
    private final Map<String, Map<String, Map<String, Number>>> cache = new HashMap<String, Map<String, Map<String, Number>>>();

    public CacheStoreFactory() {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name;
        try {
            name = new ObjectName("jrds:type=SampleCache");
            mbs.registerMBean(this, name);         
        } catch (MalformedObjectNameException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NullPointerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InstanceAlreadyExistsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (MBeanRegistrationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NotCompliantMBeanException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

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
