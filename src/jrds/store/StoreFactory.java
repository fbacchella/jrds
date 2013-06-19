package jrds.store;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import jrds.Probe;
import jrds.PropertiesManager;

public interface StoreFactory {
    public void configureStore(PropertiesManager pm);
    public void start();

    public abstract Store create(Probe<?,? > p);

    public Store configure(Probe<?,? > p, Map<String, String> properties) throws InvocationTargetException;
    
    public void stop();
    
}
