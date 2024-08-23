package jrds.store;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Properties;

import jrds.Probe;
import jrds.PropertiesManager;

public interface StoreFactory<SO> {

    String DEFAULTNAME = "_default";

    void configureStore(PropertiesManager pm, Properties props);

    void start();

    Store<SO> create(Probe<?, ?> p);

    Store<SO> configure(Probe<?, ?> p, Map<String, String> properties) throws InvocationTargetException;

    void stop();

}
