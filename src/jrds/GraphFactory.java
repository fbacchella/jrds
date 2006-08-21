/*##########################################################################
 _##
 _##  $Id$
 _##
 _##########################################################################*/

package jrds;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * This class is used to generate a class from an object.
 *
 * @author Fabrice Bacchella
 * @version $Revision$
 */
public class GraphFactory {
	static private final Logger logger = Logger.getLogger(GraphFactory.class);

	final private List<String> graphPackages = new ArrayList<String>(2);
	private Map<String, GraphDesc> graphDescMap;
	boolean legacymode = false;

	/**
	 * Private constructor
	 * @param b 
	 */
	public GraphFactory(Map<String, GraphDesc> graphDescMap, boolean legacymode) {
		graphPackages.add("jrds.graph.");
		this.graphDescMap = graphDescMap;
		this.legacymode = legacymode;
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
	public final RdsGraph makeGraph(Object className, Probe probe) {
		RdsGraph retValue = null;
		
		//Simple case, the handler is already known
		if(graphDescMap.containsKey(className)) {
			GraphDesc gd = (GraphDesc) graphDescMap.get(className);
			retValue = new RdsGraph(probe, gd);
		}
		//A class was used as a param
		//We only need to instanciate it
		else if (legacymode && className instanceof Class) {
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
		else if(legacymode && className instanceof GraphDesc ) {
			retValue = new RdsGraph(probe, (GraphDesc) className);
		}
		//We get a String
		//it's an URL to xml description of the graph
		else if(legacymode && className instanceof String) {
			String xmlRessourceName = (String)className;
			InputStream xmlStream = probe.getClass().getResourceAsStream(xmlRessourceName);
			if(xmlStream == null) {
				logger.error("Unable to find ressource " + xmlRessourceName + " for probe " + probe);
			}
		}
		return retValue;
	}
	
}
