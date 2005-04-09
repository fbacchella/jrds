package jrds.xmlResources;

//----------------------------------------------------------------------------
//$Id$


import java.io.InputStream;

/**
 * A almost empty class, just used to return resources stored in the same package.
 * @author Fabrice Bacchella
 * @version $Revision$
 */
public class ResourcesLocator {
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
		return ResourcesLocator.class.getResourceAsStream(name);
	}

}
