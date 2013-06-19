package jrds.store;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import jrds.Probe;
import jrds.PropertiesManager;
import jrds.factories.ArgFactory;

public abstract class AbstractStoreFactory<StoreType extends AbstractStore<?,?>> implements StoreFactory {

    public abstract StoreType create(Probe<?,? > p);
    
    public void configureStore(PropertiesManager pm) {
        
    }

    public StoreType configure(Probe<?,? > p, Map<String, String> properties) throws InvocationTargetException {
        StoreType s = create(p);
        for(PropertyDescriptor bean: ArgFactory.getBeanPropertiesMap(s.getClass(), AbstractStore.class).values()) {
            String beanName = bean.getName();
            if(properties.containsKey(beanName)) {
                String beanValue = properties.get(beanName);
                ArgFactory.beanSetter(s, bean.getName(), beanValue);                    
            }
        }
        return s;
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

}
