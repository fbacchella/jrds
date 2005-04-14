package jrds.xmlResources;

//----------------------------------------------------------------------------
//$Id$


import java.io.InputStream;
import jrds.RdsGraph;
import jrds.JrdsLogger;
import org.apache.log4j.Logger;

/**
 * A almost empty class, just used to return resources stored in the same package.
 * @author Fabrice Bacchella
 * @version $Revision$
 */
public class ResourcesLocator {
    static final private Logger logger = JrdsLogger.getLogger(ResourcesLocator.class);
    /**
     * We don't want any constructor
     */
    private ResourcesLocator() {};

    /**
     * Return the URL of any resource stored in the same package
     * @param name
     * @return
     */
    public static final java.net.URL getResourceUrl(String name) {

        return ResourcesLocator.class.getResource(name);
    }

    /**
     * Return a resource stored in the same package as a InputStream
     * @param name
     * @return
     */
    public static final InputStream getResource(String name) {
        logger.debug("URL for " + name + ": " + getResourceUrl(name));
        return ResourcesLocator.class.getResourceAsStream(name);
    }

}
