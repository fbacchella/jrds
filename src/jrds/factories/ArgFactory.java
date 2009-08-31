/*##########################################################################
 _##
 _##  $Id: ProbeFactory.java 373 2006-12-31 00:06:06Z fbacchella $
 _##
 _##########################################################################*/

package jrds.factories;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jrds.factories.xml.CompiledXPath;
import jrds.factories.xml.JrdsNode;

import org.apache.log4j.Logger;

/**
 * A class to build args from a string constructor
 * @author Fabrice Bacchella 
 * @version $Revision: 373 $,  $Date: 2006-12-31 01:06:06 +0100 (Sun, 31 Dec 2006) $
 */

public final class ArgFactory {
	static private final Logger logger = Logger.getLogger(ArgFactory.class);

	static private final String[] argPackages = new String[] {"java.lang.", "java.net.", "org.snmp4j.smi.", ""};

	public static List<Object> makeArgs(JrdsNode n) {
		return makeArgs(n, new Object[]{});
	}
	
	public static List<Object> makeArgs(JrdsNode n, Object... arguments) {
		List<Object> argsList = new ArrayList<Object>(5);
		for(JrdsNode argNode: n.iterate(CompiledXPath.get("arg"))) {
			Map<String,String> argMap = argNode.attrMap();
			String type = argMap.get("type");
			String value = argMap.get("value");
			value = jrds.Util.parseTemplate(value, arguments);
			Object o = ArgFactory.makeArg(type, value);
			argsList.add(o);
		}
		for(JrdsNode argNode: n.iterate(CompiledXPath.get("list"))) {
			argsList.add(makeArgs(argNode, arguments));
		}
		logger.trace("arg vector: " + argsList);
		return argsList;
	}

	/**
	 * Create an objet providing the class name and a String argument. So the class must have
	 * a constructor taking only a string as an argument.
	 * @param className
	 * @param value
	 * @return
	 */
	public static final Object makeArg(String className, String value) {
		Object retValue = null;
		Class<?> classType = resolvClass(className);
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

	private static final Class<?> resolvClass(String name) {
		Class<?> retValue = null;
		for (String packageTry: argPackages) {
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
