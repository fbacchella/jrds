/*
 * Created on 28 déc. 2004
 *
 * TODO 
 */
package jrds;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;



/**
 * @author bacchell
 *
 * TODO 
 */
public class ProbeFactory {
	static private final Logger logger = JrdsLogger.getLogger(HostConfigParser.class);
	static final private List argPackages = new ArrayList(3);
	static final private List probePackages = new ArrayList(2);
	
	static {
		argPackages.add("java.lang.");
		argPackages.add("java.net.");
		argPackages.add("");
		
		probePackages.add("com.aol.jrds.probe.");
		probePackages.add("com.aol.jrds.probe.snmp.");
		probePackages.add("com.aol.jrds.probe.munins.");
		probePackages.add("");
		
	}
	
	public static Object makeArg(String className, String value)
	{
		Object retValue = null;
		Class classType = resolvClass(className, argPackages);
		if(classType != null) {
			Class[] argsType = { value.getClass() };
			Object[] args= { value };
			
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
	
	public static Probe makeProbe(String className, List constArgs)
	{
		Probe retValue = null;
		Class probeClass = resolvClass(className, probePackages);
		if(probeClass != null) {
			Object o = null;
			try {
				Class[] constArgsType = new Class[constArgs.size()];
				Object[] constArgsVal = new Object[constArgs.size()];
				int index = 0;
				for(Iterator i = constArgs.iterator(); i.hasNext() ; index++) {
					Object arg = i.next();
					constArgsType[index] = arg.getClass();
					constArgsVal[index] = arg;
				}
				Constructor theConst = probeClass.getConstructor(constArgsType);
				o = theConst.newInstance(constArgsVal);
				retValue = (Probe) o;
			}
			catch (ClassCastException ex) {
				logger.warn("didn't get a Probe but a " + o.getClass().getName());
			}
			catch (Exception ex) {
				logger.warn("Error during probe creation of type " + className + ": " + ex, ex);
			}
		}
		return  retValue;
	}
	
	static private Class resolvClass(String name, List packList)
	{
		Class retValue = null;
		for(Iterator i = packList.iterator() ; i.hasNext() && retValue == null ;) {
			try {
				String packageTry = (String) i.next();
				retValue = Class.forName(packageTry + name);
			} catch (ClassNotFoundException ex) {
			} catch(NoClassDefFoundError ex) {
			}
		}
		if(retValue == null)
			logger.warn("Class " + name + " not found");
		return retValue;
	}
	
}
