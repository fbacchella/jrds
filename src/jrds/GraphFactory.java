package jrds;

// ----------------------------------------------------------------------------
// $Id$

import java.util.List;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import java.util.Iterator;
import java.util.ArrayList;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;

import jrds.xmlResources.ResourcesLocator;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 * @author Fabrice Bacchella
 * @version $Revision$
 */
public class GraphFactory {
	static private final Logger logger = JrdsLogger.getLogger(GraphFactory.class);
	static final private List graphPackages = new ArrayList(2);
	
	static {
		graphPackages.add("jrds.graph.");
	}
	
	/**
	 * Private constructor
	 */
	private GraphFactory() {
	}
	
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
		else if(className instanceof String) {
			String xmlRessourceName = (String)className;
			java.net.URL url = ResourcesLocator.getResourceUrl(xmlRessourceName);
			InputStream xmlStream = ResourcesLocator.getResource(xmlRessourceName);
			Digester digester = new Digester();
			digester.setValidating(false);
			digester.addObjectCreate("graphdesc","jrds.GraphDesc");
			digester.addSetProperties("graphdesc");
			digester.addCallMethod("graphdesc/filename", "setFilename", 0);
			digester.addCallMethod("graphdesc/verticalLabel", "setVerticalLabel", 0);
			digester.addCallMethod("graphdesc/graphTitle", "setGraphTitle", 0);
			digester.addCallMethod("graphdesc/add","add",7);
			digester.addCallParam("graphdesc/add/name",0);
			digester.addCallParam("graphdesc/add/dsName",1);
			digester.addCallParam("graphdesc/add/rpn",2);
			digester.addCallParam("graphdesc/add/graphType",3);
			digester.addCallParam("graphdesc/add/color",4);
			digester.addCallParam("graphdesc/add/legend",5);
			digester.addCallParam("graphdesc/add/cf",6);
			digester.addObjectCreate("graphdesc/hosttree", "java.util.ArrayList");
			digester.addSetNext("graphdesc/hosttree", "setHostTree");
			digester.addObjectCreate("graphdesc/viewtree", "java.util.ArrayList");
			digester.addSetNext("graphdesc/viewtree", "setViewTree");
			digester.addRule("*/pathelement", new Rule() {
				public void body (String namespace, String name, String text) {
					List tree = (List) getDigester().peek();
					tree.add(GraphDesc.resolvPathElement(text));
					logger.debug(getDigester().peek());
				}	
			}
			);
			digester.addRule("*/pathstring", new Rule() {
				public void body (String namespace, String name, String text) {
					List tree = (List) getDigester().peek();
					tree.add(text);
					logger.debug(getDigester().peek());
				}	
			}
			);
			try {
				GraphDesc gd = (GraphDesc) digester.parse(xmlStream);
				retValue = new RdsGraph(probe, gd);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				xmlStream.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		return retValue;
	}
	
	static final private Class resolvClass(String name, List packList) {
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
	
}
