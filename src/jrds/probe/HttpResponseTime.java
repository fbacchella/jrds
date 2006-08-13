/*
 * Created on 22 nov. 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package jrds.probe;

import java.net.URL;
import java.security.MessageDigest;
import java.util.Properties;

import jrds.Util;

import org.apache.log4j.Logger;


/**
 * @author bacchell
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public final class HttpResponseTime extends ExternalCmdProbe implements UrlProbe {
	static final private Logger logger = Logger.getLogger(HttpResponseTime.class);
	private URL url;
	static private MessageDigest md5digest;
	static {
		try {
			md5digest = java.security.MessageDigest.getInstance("MD5");
		}
		catch (java.security.NoSuchAlgorithmException ex) {
			logger.fatal("You should not see this message, MD5 not available");
		}
	}

	public HttpResponseTime(URL url)
	{
		this.url = url;
		setName(initName());
	}

	@Override
	public void readProperties(Properties p) {
		super.readProperties(p);
		setCmd(new String[] {p.getProperty("urlperfpath", "./HTTPTest"), url.toString()});
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
