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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import jrds.xmlResources.ResourcesLocator;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

/**
 * This class is used to generate a class from an object.
 *
 * @author Fabrice Bacchella
 * @version $Revision$
 */
public class GraphFactory {
	static private final Logger logger = Logger.getLogger(GraphFactory.class);
	static final private List graphPackages = new ArrayList(2);
	static final private Map graphDescMap = Collections.synchronizedMap(new HashMap());
	static final Pattern p = Pattern.compile(".*.xml");
	static final FileFilter filter = new  FileFilter(){
		public boolean accept(File pathname) {
			return  ! pathname.isHidden() && ( p.matcher(pathname.getName()).matches() || pathname.isDirectory() );
		}
	};

	static {
		graphPackages.add("jrds.graph.");
	}
	
	/**
	 * Private constructor
	 */
	private GraphFactory() {
	}
	
	public static void init() {
		scanProbeDir(new File(PropertiesManager.getInstance().graphlibpath));
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
					logger.debug("graph " + file + " found");
					InputStream xmlStream = new FileInputStream(file);
					GraphDesc gd = makeGraphDesc(xmlStream);
					xmlStream.close();
					graphDescMap.put(gd.getName(), gd);
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
	 * This method is used to generate a class from an object. It can be :
	 * 
	 * <ul>
	 * <li>a xml file, found by is URL ;</li>
	 * <li>a GraphDesc Object ;</li>
	 * <li>a RdsGraph Class</li>
	 * </ul>
	 * 
	 * @param className the object from which the Graph will be created
	 * @param probe the probe the graph will take data from
	 * @return a new instanciated Graph
	 */
	public final static RdsGraph makeGraph(Object className, Probe probe) {
		RdsGraph retValue = null;
		
		//Simple case, the handler is already known
		if(graphDescMap.containsKey(className)) {
			GraphDesc gd = (GraphDesc) graphDescMap.get(className);
			retValue = new RdsGraph(probe, gd);
		}
		//A class was used as a param
		//We only need to instanciate it
		else if (className instanceof Class) {
			Class graphClass = (Class) className;
			
			try {
				if (RdsGraph.class.isAssignableFrom(graphClass)) {
					Class[] probeClassArray = new Class[] {Probe.class};
					Object[] args = new Object[] {probe};
					
					Constructor co = graphClass.getConstructor(probeClassArray);
					retValue = (RdsGraph) co.newInstance(args);
				}
				else {
					logger.warn("didn't get a RdsGraph but a " +
							graphClass.getClass().getName());
				}
			}
			catch (Exception ex) {
				logger.warn("Error during RdsGraph creation of type " + className +
						": " + ex, ex);
			}
		}
		//We get a GraphDesc
		//We need to instanciate a Graph using this description
		else if(className instanceof GraphDesc ) {
			retValue = new RdsGraph(probe, (GraphDesc) className);
		}
		//We get a String
		//it's an URL to xml description of the graph
		else if(className instanceof String) {
			String xmlRessourceName = (String)className;
			InputStream xmlStream = ResourcesLocator.getResource(xmlRessourceName);
			if(xmlStream == null) {
				xmlStream = probe.getClass().getResourceAsStream(xmlRessourceName);
			}
			if(xmlStream == null) {
				logger.error("Unable to find ressource " + xmlRessourceName + " for probe " + probe);
			}
			else {
				try {
					GraphDesc gd = makeGraphDesc(xmlStream);
					retValue = new RdsGraph(probe, gd);
					graphDescMap.put(gd.getName(), gd);
				} catch (IOException e) {
					logger.error("Unable to get the file for " + className);
				} catch (SAXException e) {
					logger.error("Unable to parse xml file " + className);
				}
			}
		}
		return retValue;
	}
	
	static public GraphDesc makeGraphDesc(InputStream xmlStream) throws IOException, SAXException {
		Digester digester = new Digester();
		digester.setValidating(false);
		digester.addObjectCreate("graphdesc", jrds.GraphDesc.class);
		digester.addSetProperties("graphdesc");
		digester.addCallMethod("graphdesc/name", "setName", 0);
		digester.addCallMethod("graphdesc/filename", "setGraphName", 0);
		digester.addCallMethod("graphdesc/graphName", "setGraphName", 0);
		digester.addCallMethod("graphdesc/verticalLabel", "setVerticalLabel", 0);
		digester.addCallMethod("graphdesc/graphTitle", "setGraphTitle", 0);
		digester.addCallMethod("graphdesc/upperLimit", "setUpperLimit", 0);
		digester.addCallMethod("graphdesc/add","add",7);
		digester.addCallParam("graphdesc/add/name",0);
		digester.addCallParam("graphdesc/add/dsName",1);
		digester.addCallParam("graphdesc/add/rpn",2);
		digester.addCallParam("graphdesc/add/graphType",3);
		digester.addCallParam("graphdesc/add/color",4);
		digester.addCallParam("graphdesc/add/legend",5);
		digester.addCallParam("graphdesc/add/cf",6);
		digester.addObjectCreate("graphdesc/hosttree", java.util.ArrayList.class);
		digester.addSetNext("graphdesc/hosttree", "setHostTree");
		digester.addObjectCreate("graphdesc/viewtree", java.util.ArrayList.class);
		digester.addSetNext("graphdesc/viewtree", "setViewTree");
		digester.addRule("*/pathelement", new Rule() {
			public void body (String namespace, String name, String text) {
				List tree = (List) getDigester().peek();
				tree.add(GraphDesc.resolvPathElement(text));
			}	
		}
		);
		digester.addRule("*/pathstring", new Rule() {
			public void body (String namespace, String name, String text) {
				List tree = (List) getDigester().peek();
				tree.add(text);
			}	
		}
		);
		return (GraphDesc) digester.parse(xmlStream);
	}
}
