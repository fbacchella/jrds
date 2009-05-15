/*##########################################################################
 _##
 _##  $Id$
 _##
 _##########################################################################*/

package jrds;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
	private Properties prop;
	private boolean legacymode;
	/**
	 * Private constructor
	 * @param b 
	 */
	public ProbeFactory(Map<String, ProbeDesc> probeDescMap, GraphFactory gf, Properties prop, boolean legacymode) {
		this.probeDescMap = probeDescMap;
		this.gf = gf;
		this.prop = prop;
		this.legacymode = legacymode;

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
		else if(legacymode ){
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
			retValue.readProperties(prop);
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
				logger.warn("Missing class for the creation of a probe " + ex);
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
			catch (Exception ex) {
				Throwable showException = ex;
				Throwable t = ex.getCause();
				if(t != null)
					showException = t;
				logger.warn("Error during probe creation of type " + pd.getName() + " with args " + constArgs +
						": ", showException);
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

	public ProbeDesc getProbeDesc(String name) {
		return probeDescMap.get(name);
	}
}
