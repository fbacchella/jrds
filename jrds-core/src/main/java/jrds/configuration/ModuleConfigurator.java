package jrds.configuration;

import jrds.PropertiesManager;

public abstract class ModuleConfigurator {
    public abstract Object configure(PropertiesManager pm);
}
