/*##########################################################################
 _##
 _##  $Id$
 _##
 _##########################################################################*/

package jrds;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import jrds.snmp.SnmpRequester;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;
import org.apache.log4j.Logger;
import org.snmp4j.smi.OID;
import org.xml.sax.SAXException;

/**
 * A class to find probe by their names
 * @author Fabrice Bacchella 
 * @version $Revision$,  $Date$
 */
public class ProbeFactory {
	static private final Logger logger = Logger.getLogger(ProbeFactory.class);
	static final private List argPackages = new ArrayList(3);
	static final private List probePackages = new ArrayList(5);
	static final private Map probeDescMap = new HashMap();
	static final Pattern p = Pattern.compile(".*.xml");
	static final FileFilter filter = new  FileFilter(){
		public boolean accept(File pathname) {
			return  ! pathname.isHidden() && ( p.matcher(pathname.getName()).matches() || pathname.isDirectory() );
		}
	};
	
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
	
	public static void init() {
		scanProbeDir(new File(PropertiesManager.getInstance().probelibpath));
	}
	
	/**
	 * Recursively walk a directory tree and return a List of all
	 * Files found; the List is sorted using File.compareTo.
	 *
	 * @param aStartingDir is a valid directory, which can be read.
	 */
	static private void scanProbeDir( File aStartingDir ) {
		File[] filesAndDirs = aStartingDir.listFiles(filter);
		for(int i = 0 ; i < filesAndDirs.length; i++) {
			File file = filesAndDirs[i];
			if(file.isFile()) {
				try {
					logger.debug("probe " + file + " found");
					ProbeDesc pd = makeProbeDesc(new FileInputStream(file));
					probeDescMap.put(pd.getName(), pd);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SAXException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else if (file.isDirectory()) {
				scanProbeDir(file);
			}
			
		}
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
					Class[] constArgsType = new Class[constArgs.size() + 1];
					Object[] constArgsVal = new Object[constArgs.size() + 1];
					int index = 0;
					constArgsVal[index] = host;
					constArgsType[index] = constArgsVal[index].getClass();
					index++;
					for (Iterator i = constArgs.iterator(); i.hasNext(); index++) {
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
					logger.warn("Error during probe creation of type " + className +
							": " + ex, ex);
				}
			}
		}
		return retValue;
	}
	
	/**
	 * Return a class by is name and a list of possible package in which it can be
	 * found
	 * @param name the class name
	 * @param packList a List of package name
	 * @return
	 */
	static private Class resolvClass(String name, List packList) {
		Class retValue = null;
		for (Iterator i = packList.iterator(); i.hasNext() && retValue == null; ) {
			try {
				String packageTry = (String) i.next();
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
	
	static public ProbeDesc makeProbeDesc(InputStream xmlStream) throws IOException, SAXException {
		Digester digester = new Digester();
		digester.setValidating(false);
		digester.addObjectCreate("probedesc", jrds.ProbeDesc.class);

		digester.addRule("probedesc/probeClass", new Rule() {
			public void body(String namespace, String name, String text) {
				Class c = resolvClass(text.trim(),probePackages);
				ProbeDesc pd = (ProbeDesc) digester.peek();
				pd.setProbeClass(c);
			}
		});
		digester.addCallMethod("probedesc/probeName", "setProbeName", 0);
		digester.addCallMethod("probedesc/name", "setName", 0);
		
		Class[] params = new Class[] { Boolean.class};
		digester.addCallMethod("probedesc/uniq", "setUniqIndex", 0, params);
		
		params = new Class[] { String.class};
		digester.addCallMethod("probedesc/index", "setIndexOid", 0, params);
		
		digester.addObjectCreate("probedesc/ds", HashMap.class);
		digester.addSetNext("probedesc/ds", "add");
		digester.addRule("probedesc/ds/dsName", new Rule() {
			public void body(java.lang.String namespace, java.lang.String name, java.lang.String text) {
				Map m  = (Map) digester.peek();
				m.put("dsName", text);
			}
		});
		digester.addRule("probedesc/ds/dsType", new Rule() {
			public void body(String namespace, String name, String text) throws IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException {
				Map m  = (Map) digester.peek();
				m.put("dsType", ProbeDesc.class.getField(text.trim().toUpperCase()).get(null));
			}
		});
		digester.addRule("probedesc/ds/oid", new Rule() {
			public void body (String namespace, String name, String text) {
				Map m  = (Map) digester.peek();
				m.put("index", new OID(text.trim()));
			}	
		}
		);
		digester.addRule("probedesc/snmpRequester", new Rule() {
			public void body(java.lang.String namespace, java.lang.String name, java.lang.String text) throws IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException  {
				SnmpRequester req = (SnmpRequester) SnmpRequester.class.getField(text.toUpperCase()).get(null);
				ProbeDesc pd = (ProbeDesc) digester.peek();
				pd.setRequester(req);
			}
		});
		digester.addObjectCreate("probedesc/graphs", ArrayList.class);
		digester.addSetNext("probedesc/graphs","setGraphClasses");
		
		params = new Class[] { Object.class};
		digester.addCallMethod("probedesc/graphs/name", "add", 0, params);
		
		return (ProbeDesc) digester.parse(xmlStream);
	}
}
