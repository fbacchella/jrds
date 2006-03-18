/*##########################################################################
_##
_##  $Id$
_##
_##########################################################################*/

package jrds;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import jrds.xmlResources.ResourcesLocator;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;
import org.apache.log4j.Logger;

/**
 * This class is used to generate a class from an object.
 *
 * @author Fabrice Bacchella
 * @version $Revision$
 */
public class GraphFactory {
	static private final Logger logger = Logger.getLogger(GraphFactory.class);
	static final private List graphPackages = new ArrayList(2);
	
	static {
		graphPackages.add("jrds.graph.");
	}
	
	/**
	 * Private constructor
	 */
	private GraphFactory() {
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
		
		//A class was used as a param
		//We only need to instanciate it
		if (className instanceof Class) {
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
				Digester digester = new Digester();
				digester.setValidating(false);
				digester.addObjectCreate("graphdesc", jrds.GraphDesc.class);
				digester.addSetProperties("graphdesc");
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
				try {
					GraphDesc gd = (GraphDesc) digester.parse(xmlStream);
					retValue = new RdsGraph(probe, gd);
				} catch (Exception e) {
					logger.warn("Unable to parse graph description file "+ xmlRessourceName + " for probe " + probe, e);
					retValue = null;
				}
				try {
					xmlStream.close();
				} catch (IOException e1) {
					logger.error("You're busted" +  e1.getMessage());
				}
			}
		}
		return retValue;
	}
	
}
