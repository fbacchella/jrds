package jrds.configuration;

import jrds.Probe;

public class ProbeClassResolver {

    protected final ClassLoader classLoader;

    public ProbeClassResolver(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @SuppressWarnings("unchecked")
    public Class<? extends Probe<?, ?>> getClassByName(String className) throws ClassNotFoundException {
        return (Class<? extends Probe<?, ?>>) classLoader.loadClass(className);
    }

}
