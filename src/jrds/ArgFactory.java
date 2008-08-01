/*##########################################################################
 _##
 _##  $Id: ProbeFactory.java 373 2006-12-31 00:06:06Z fbacchella $
 _##
 _##########################################################################*/

package jrds;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * A class to build args from a string constructor
 * @author Fabrice Bacchella 
 * @version $Revision: 373 $,  $Date: 2006-12-31 01:06:06 +0100 (Sun, 31 Dec 2006) $
 */

public class ArgFactory {
	private final Logger logger = Logger.getLogger(ArgFactory.class);

	final private List<String> argPackages = new ArrayList<String>(3);

	public ArgFactory() {
		argPackages.add("java.lang.");
		argPackages.add("java.net.");
		argPackages.add("");
	}
	
	/**
	 * Create an objet providing the class name and a String argument. So the class must have
	 * a constructor taking only a string as an argument.
	 * @param className
	 * @param value
	 * @return
	 */
	public Object makeArg(String className, String value) {
		Object retValue = null;
		Class<?> classType = resolvClass(className, argPackages);
		if (classType != null) {
			Class<?>[] argsType = { String.class };
			Object[] args = { value };

			try {
				Constructor<?> theConst = classType.getConstructor(argsType);
				retValue = theConst.newInstance(args);
			}
			catch (Exception ex) {
				logger.warn("Error during of creation :" + className + ": ", ex);
			}
		}
		return retValue;
	}

	private Class<?> resolvClass(String name, List<String> listPackages) {
		Class<?> retValue = null;
		for (String packageTry: listPackages) {
			try {
				retValue = Class.forName(packageTry + name);
			}
			catch (ClassNotFoundException ex) {
			}
			catch (NoClassDefFoundError ex) {
			}
		}
		if (retValue == null)
			logger.warn("Class " + name + " not found");
		return retValue;
	}

}
