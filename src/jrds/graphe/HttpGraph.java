/*
 * Created on 8 déc. 2004
 *
 * TODO 
 */
package jrds.graphe;

import java.awt.Color;
import java.security.MessageDigest;
import java.util.logging.Logger;

import jrds.GraphDesc;
import jrds.Probe;
import jrds.RdsGraph;


/**
 * @author bacchell
 *
 * TODO 
 */
public class HttpGraph extends RdsGraph {
	static final private Logger logger = Logger.getLogger(HttpGraph.class.getPackage().getName());
	static MessageDigest md5digest;
	static {
		try {
			md5digest = java.security.MessageDigest.getInstance("MD5");
		}
		catch (java.security.NoSuchAlgorithmException ex) {
			logger.severe("You should not see this message, MD5 not available");
		}
	}
	
	static final GraphDesc gd = new GraphDesc(3);
	static {
		gd.add("Connect", GraphDesc.AREA, Color.GREEN, "Connect");
		gd.add("First Byte", GraphDesc.STACK, Color.BLUE, "First Byte");
		gd.add("Last Byte", GraphDesc.STACK, Color.CYAN, "Last Byte");
		gd.setVerticalLabel("time (s)");
		gd.setGraphName("{4}");
		gd.setGraphTitle("Respons time for {3}");
		
		gd.setHostTree(new Object[] { GraphDesc.HOST, GraphDesc.SERVICES, GraphDesc.URL});
		gd.setViewTree(new Object[] { GraphDesc.SERVICES, GraphDesc.WEB, GraphDesc.URL});
	}

	/**
	 * @param theStore
	 */
	public HttpGraph(Probe theStore) {
		super(theStore, gd);
	}
}
