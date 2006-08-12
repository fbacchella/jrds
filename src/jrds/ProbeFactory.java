/*##########################################################################
 _##
 _##  $Id$
 _##
 _##########################################################################*/

package jrds;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * A class to find probe by their names
 * @author Fabrice Bacchella 
 * @version $Revision$,  $Date$
 */
public class ProbeFactory {

	static private final Logger logger = Logger.getLogger(ProbeFactory.class);
	/*static {
		final String DEFAULT_PARSER_NAME = "org.apache.xerces.parsers.SAXParser";
		try {
			XMLReaderFactory.createXMLReader(DEFAULT_PARSER_NAME);
		} catch (SAXException e) {
			logger.fatal("xerces is mandatory, please install it");
		}
	}*/
	
	static final private List<String> argPackages = new ArrayList<String>(3);
	static final public Map<String, ProbeDesc> probeDescMap = new HashMap<String, ProbeDesc>();
	static final private List<String> probePackages = new ArrayList<String>(5);
	
	static {
		argPackages.add("java.lang.");
		argPackages.add("java.net.");
		argPackages.add("");

		probePackages.add("jrds.probe.");
		probePackages.add("jrds.probe.snmp.");
		probePackages.add("jrds.probe.munins.");
		probePackages.add("jrds.probe.rstat.");
		probePackages.add("jrds.probe.jdbc.");
		probePackages.add("jrds.probe.exalead.");
		probePackages.add("");

	}
	
	/**
	 * Private constructor
	 */
	private ProbeFactory() {
		
	}
	
	public static void addDesc(ProbeDesc p) {
		probeDescMap.put(p.getName(), p);
	}
	
	/**
	 * Create an objet providing the class name and a String argument. So the class must have
	 * a constructor taking only a string as an argument.
	 * @param className
	 * @param value
	 * @return
	 */
	public static Object makeArg(String className, String value) {
		Object retValue = null;
		Class classType = resolvClass(className, argPackages);
		if (classType != null) {
			Class[] argsType = { String.class };
			Object[] args = { value };
			
			try {
				Constructor theConst = classType.getConstructor(argsType);
				retValue = theConst.newInstance(args);
			}
			catch (Exception ex) {
				logger.warn("Error during of creation :" + className + ": ", ex);
			}
		}
		return retValue;
	}
	
	/**
	 * Create an probe, provided his Class and a list of argument for a constructor
	 * for this object. It will be found using the default list of possible package
	 * @param className the probe name
	 * @param constArgs
	 * @return
	 */
	public static Probe makeProbe(String className, RdsHost host, List constArgs) {
		Probe retValue = null;
		ProbeDesc pd = (ProbeDesc) probeDescMap.get(className);
		if( pd != null) {
			retValue = pd.makeProbe(host, constArgs);
		}
		else {
			Class probeClass = resolvClass(className, probePackages);
			if (probeClass != null) {
				Object o = null;
				try {
					Class[] constArgsType = new Class[constArgs.size()/* + 1*/];
					Object[] constArgsVal = new Object[constArgs.size()/* + 1*/];
					int index = 0;
					/*constArgsVal[index] = host;
					constArgsType[index] = constArgsVal[index].getClass();
					index++;*/
					for (Object arg: constArgs) {
						constArgsType[index] = arg.getClass();
						constArgsVal[index] = arg;
						index++;
					}
					Constructor theConst = probeClass.getConstructor(constArgsType);
					o = theConst.newInstance(constArgsVal);
					retValue = (Probe) o;
					retValue.setHost(host);
				}
				catch (ClassCastException ex) {
					logger.warn("didn't get a Probe but a " + o.getClass().getName());
				}
				catch (Exception ex) {
					logger.warn("Error during probe creation of type " + className +
							": " + ex, ex);
				}
			}
		}
		return retValue;
	}
	
	private static Class<?> resolvClass(String name, List<String> listPackages) {
		Class retValue = null;
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

	public static ProbeDesc getProbeDesc(String name) {
		return probeDescMap.get(name);
	}
}
