/*
 * Created on 22 nov. 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package jrds.probe;

import java.net.URL;
import java.security.MessageDigest;

import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.Util;
import jrds.graphe.HttpGraph;

import org.apache.log4j.Logger;
import org.rrd4j.DsType;


/**
 * @author bacchell
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public final class HttpResponseTimeRrd extends ExternalCmdProbe implements UrlProbe {
	static final private Logger logger = Logger.getLogger(HttpResponseTimeRrd.class);
	private URL url;
	static MessageDigest md5digest;
	static jrds.PropertiesManager pm = jrds.PropertiesManager.getInstance();
	static {
		try {
			md5digest = java.security.MessageDigest.getInstance("MD5");
		}
		catch (java.security.NoSuchAlgorithmException ex) {
			logger.fatal("You should not see this message, MD5 not available");
		}
	}
	
	static final ProbeDesc pd = new ProbeDesc(3);
	static {
		pd.add("Connect", DsType.GAUGE);
		pd.add("First Byte", DsType.GAUGE);
		pd.add("Last Byte", DsType.GAUGE);
		pd.setGraphClasses(new Class[] {HttpGraph.class});
	}
	
	public HttpResponseTimeRrd(RdsHost thehost, URL url)
	{
		super(thehost, pd);
		this.url = url;
		this.setCmd(new String[] {pm.urlperfpath, url.toString()});
		setName(initName());
	}

	protected String initName()
	{
		String retval = null;
		md5digest.reset();
		byte[] digestval = md5digest.digest(url.toString().getBytes());
		retval =  "url-" + Util.toBase64(digestval);;
		return retval;
	}
	/**
	 * @return Returns the url.
	 */
	public URL getUrl() {
		return url;
	}

	/* (non-Javadoc)
	 * @see jrds.probe.UrlProbe#getUrlAsString()
	 */
	public String getUrlAsString() {
		return getUrl().toString();
	}
}
