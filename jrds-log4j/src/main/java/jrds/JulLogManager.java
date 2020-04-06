package jrds;

import java.util.logging.LogManager;
import java.util.logging.Logger;

public class JulLogManager extends LogManager {

    @Override
    public boolean addLogger(Logger logger) {
        boolean added = super.addLogger(logger);
        JuliToLog4jHandler.catchLogger(logger);
        return added;
    }

}
