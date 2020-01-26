package fr.jrds.pcp;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.BeforeClass;

public class Tester {

    @BeforeClass
    public static void setLog() {
        System.setProperty("Log4jContextSelector", "org.apache.logging.log4j.core.selector.BasicContextSelector");
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
        boolean inMaven = System.getProperty("surefire.real.class.path") != null || System.getProperty("surefire.test.class.path") != null;
        if (inMaven) {
            Configurator.setRootLevel(Level.OFF);
        } else {
            Configurator.setRootLevel(Level.TRACE);
        }
        Configurator.setLevel("javax.management.mbeanserver", Level.ERROR);

    }

}
