package jrds.store;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import jrds.Probe;

public interface StoreFactory {
    public abstract Store create(Probe<?,? > p);

    public Store configure(Probe<?,? > p, Map<String, String> properties) throws InvocationTargetException;
    
}
