package jrds.standalone;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jrds.jmx.Management;

public class JMX extends CommandStarterImpl {

    static private final Logger logger = LoggerFactory.getLogger(Jetty.class);

    int port = 8080;
    String propFileName = "jrds.properties";

    public void configure(Properties configuration) {
        logger.debug("Configuration: " + configuration);

        port = jrds.Util.parseStringNumber((String) configuration.getProperty("jmx.port"), port);
        propFileName = configuration.getProperty("propertiesFile", propFileName);
    }

    @Override
    public void start(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");

        Properties pm = new Properties();
        File propFile = new File(propFileName);
        if(propFile.isFile()) {
            try (FileReader fr = new FileReader(propFile)) {
                pm.load(fr);
            }
        }
        pm.setProperty("withjmx", "yes");
        jrds.Configuration.configure(pm);

        if(jrds.Configuration.get().getPropertiesManager().withjmx) {
            doJmx(jrds.Configuration.get().getPropertiesManager());
            Management.register(propFile);
        }

        // Make it wait on himself to wait forever
        try {
            Thread.currentThread().join();
            System.out.print("joined");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see jrds.standalone.CommandStarterImpl#help()
     */
    @Override
    public void help() {
        System.out.println("Run jrds without a web server");
        System.out.print("The default listening port is " + port);
        System.out.println(". It can be specified using the property jmx.port");
        System.out.println("The jrds configuration file is specified using the property propertiesFile");
    }

    @Override
    public String getName() {
        return "jmxserver";
    }

}
