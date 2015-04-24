package jrds.standalone;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;

import jrds.jmx.Management;

import org.apache.log4j.Logger;

public class JMX extends CommandStarterImpl {

    static private final Logger logger = Logger.getLogger(Jetty.class);

    int port = 8080;
    String propFileName = "jrds.properties";

    public void configure(Properties configuration) {
        logger.debug("Configuration: " + configuration);

        port = jrds.Util.parseStringNumber((String) configuration.getProperty("jmx.port"), port).intValue();
        propFileName =  configuration.getProperty("propertiesFile", propFileName);
    }

    @Override
    public void start(String[] args) throws Exception {
        System.setProperty("java.awt.headless","true");

        jrds.JrdsLoggerConfiguration.initLog4J();

        Properties pm = new Properties();
        File propFile = new File(propFileName);
        if(propFile.isFile())
            pm.load(new FileReader(propFile));
        pm.setProperty("withjmx", "yes");
        jrds.Configuration.configure(pm);
        
        jrds.JrdsLoggerConfiguration.configure(jrds.Configuration.get().getPropertiesManager());

        if(jrds.Configuration.get().getPropertiesManager().withjmx) {
            doJmx(jrds.Configuration.get().getPropertiesManager());
            Management.register(propFile);
        }

        //Make it wait on himself to wait forever
        try {
            Thread.currentThread().join();
            System.out.print("joined");
        } catch (InterruptedException e) {
        }

    }

    /* (non-Javadoc)
     * @see jrds.standalone.CommandStarterImpl#help()
     */
    @Override
    public void help() {
        System.out.println("Run jrds without a web server");
        System.out.print("The default listening port is " + port);
        System.out.println(". It can be specified using the property jmx.port");
        System.out.println("The jrds configuration file is specified using the property propertiesFile");
    }

}
