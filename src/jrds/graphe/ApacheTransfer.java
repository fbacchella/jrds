/*
 * Created on 10 janv. 2005
 *
 * TODO 
 */
package jrds.graphe;

import java.awt.Color;
import java.net.MalformedURLException;
import java.net.URL;

import jrds.GraphDesc;
import jrds.Probe;
import jrds.RdsGraph;
import jrds.probe.ApacheStatus;


/**
 * @author bacchell
 *
 * TODO 
 */
public class ApacheTransfer extends RdsGraph {
	
	static final GraphDesc gd = new GraphDesc(1);
	static {
		gd.add("Total kBytes", GraphDesc.LINE, Color.GREEN, "Bytes transfered");
		gd.setGraphName("apachetransfer");
		gd.setVerticalLabel("Bytes/s");
		gd.setHostTree(new Object[] { GraphDesc.HOST, GraphDesc.SERVICES, "Apache Activity", GraphDesc.URL, "Bytes transfered/s"});
		gd.setViewTree(new Object[] { GraphDesc.SERVICES,  "Apache", "Bytes transfered/s", GraphDesc.URL});
	}

	private URL url;

	/**
	 * @param theStore
	 */
	public ApacheTransfer(Probe theStore) {
		super(theStore, gd);
		url = ((ApacheStatus) theStore).getUrl();
		URL tmpUrl = url;
		try {
			tmpUrl = new URL(url.getProtocol(), url.getHost(), "/");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		setGraphTitle("HTTP bytes transfered on " + tmpUrl.toExternalForm());
	}
}
