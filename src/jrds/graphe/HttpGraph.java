/*
 * Created on 8 déc. 2004
 *
 * TODO 
 */
package jrds.graphe;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.security.MessageDigest;
import java.util.logging.Logger;

import javax.mail.MessagingException;
import javax.mail.internet.MimeUtility;

import jrds.GraphDesc;
import jrds.Probe;
import jrds.RdsGraph;
import jrds.probe.HttpResponseTimeRrd;


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
		
		gd.setHostTree(new Object[] { GraphDesc.HOST, GraphDesc.SERVICES, GraphDesc.TITLE});
		gd.setViewTree(new Object[] { GraphDesc.SERVICES, GraphDesc.WEB, GraphDesc.TITLE});
	}

	/**
	 * @param theStore
	 */
	public HttpGraph(Probe theStore) {
		super(theStore, gd);
		URL url = ((HttpResponseTimeRrd) theStore).getUrl();
		setGraphTitle(url.toString());
		setGraphName(makeFileNamePrefix(url));
	}
	
	/* (non-Javadoc)
	 * @see com.aol.jrds.RdsGraph#initName()
	 */
	private String makeFileNamePrefix(URL url) {
		String retval = null;
		md5digest.reset();
		byte[] digestval = md5digest.digest(url.toString().getBytes());
		ByteArrayOutputStream digestOs = new ByteArrayOutputStream(30);
		try {
			MimeUtility.encode(digestOs, "base64").write(digestval);
			retval = "URL-" + digestOs.toString();
			retval = retval.replace('/', '_');
		} catch (IOException e) {
		} catch (MessagingException e) {
		}
		return retval;
	}
}
