/*##########################################################################
 _##
 _##  $Id$
 _##
 _##########################################################################*/

package jrds.factories;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jrds.Probe;
import jrds.ProbeDesc;
import jrds.PropertiesManager;

import org.apache.log4j.Logger;

/**
 * A class to find probe by their names
 * @author Fabrice Bacchella 
 * @version $Revision$,  $Date$
 */
public class ProbeFactory {

	private final Logger logger = Logger.getLogger(ProbeFactory.class);
	final private List<String> probePackages = new ArrayList<String>(5);
	private Map<String, ProbeDesc> probeDescMap;
	private GraphFactory gf;
	private PropertiesManager pm;
	/**
	 * Private constructor
	 * @param b 
	 */
	public ProbeFactory(Map<String, ProbeDesc> probeDescMap, GraphFactory gf, PropertiesManager pm) {
		this.probeDescMap = probeDescMap;
		this.gf = gf;
		this.pm = pm;

		probePackages.add("");
	}

	/**
	 * Create an probe, provided his Class and a list of argument for a constructor
	 * for this object. It will be found using the default list of possible package
	 * @param className the probe name
	 * @param constArgs
	 * @return
	 */
	public Probe makeProbe(String className, List<?> constArgs) {
		Probe retValue = null;
		ProbeDesc pd = (ProbeDesc) probeDescMap.get(className);
		if( pd != null) {
			retValue = makeProbe(pd, constArgs);
		}
		else if(pm.legacymode ){
			Class<?> probeClass = resolvClass(className, probePackages);
			if (probeClass != null) {
				Object o = null;
				try {
					Class<?>[] constArgsType = new Class[constArgs.size()];
					Object[] constArgsVal = new Object[constArgs.size()];
					int index = 0;
					for (Object arg: constArgs) {
						constArgsType[index] = arg.getClass();
						constArgsVal[index] = arg;
						index++;
					}
					Constructor<?> theConst = probeClass.getConstructor(constArgsType);
					o = theConst.newInstance(constArgsVal);
					retValue = (Probe) o;
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
		else {
			logger.error("Probe named " + className + " not found");
		}

		//Now we finish the initialization of classes
		if(retValue != null) {
			retValue.initGraphList(gf);
		}
		return retValue;
	}

	/**
	 * Instanciate a probe using a probedesc
	 * @param constArgs
	 * @return
	 */
	public Probe makeProbe(ProbeDesc pd, List<?> constArgs) {
		Class<? extends Probe> probeClass = pd.getProbeClass();
		List<?> defaultsArgs = pd.getDefaultArgs();
		Probe retValue = null;
		if (probeClass != null) {
			Object o = null;
			try {
				if(defaultsArgs != null && constArgs != null && constArgs.size() <= 0)
					constArgs = defaultsArgs;
				Class<?>[] constArgsType = new Class[constArgs.size()];
				Object[] constArgsVal = new Object[constArgs.size()];
				int index = 0;
				for (Object arg: constArgs) {
					constArgsType[index] = arg.getClass();
					if(arg instanceof List) {
						constArgsType[index] = List.class;
					}
					constArgsVal[index] = arg;
					index++;
				}
				Constructor<? extends Probe> theConst = probeClass.getConstructor(constArgsType);
				o = theConst.newInstance(constArgsVal);
				retValue = (Probe) o;
				retValue.setPd(pd);
			}
			catch (ClassCastException ex) {
				logger.warn("didn't get a Probe but a " + o.getClass().getName());
			}
			catch (NoClassDefFoundError ex) {
				logger.warn("Missing class for the creation of a probe " + pd.getName());
			}
			catch(InstantiationException ex) {
				if(ex.getCause() != null)
					logger.warn("Instantation exception : " + ex.getCause().getMessage(),
							ex.getCause());
				else {
					logger.warn("Instantation exception : " + ex,
							ex);					
				}
			}
			catch (NoSuchMethodException ex) {
				logger.warn("ProbeDescription invalid " + pd.getName() + ": no constructor " + ex.getMessage() + " found");
			}
			catch (Exception ex) {
				Throwable showException = ex;
				Throwable t = ex.getCause();
				if(t != null)
					showException = t;
				logger.warn("Error during probe creation of type " + pd.getName() + " with args " + constArgs +
						": ", showException);
			}
		}
		if(pm != null) {
			logger.trace("Setting time step to " + pm.step + " for " + retValue);
			retValue.setStep(pm.step);
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

	public ProbeDesc getProbeDesc(String name) {
		return probeDescMap.get(name);
	}
}
