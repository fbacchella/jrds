/*
 * Created on 22 nov. 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package jrds.probe;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.security.MessageDigest;

import javax.mail.MessagingException;
import javax.mail.internet.MimeUtility;

import jrds.JrdsLogger;
import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.graphe.HttpGraph;

import org.apache.log4j.Logger;


/**
 * @author bacchell
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public final class HttpResponseTimeRrd extends ExternalCmdProbe implements UrlProbe {
	static final private Logger logger = JrdsLogger.getLogger(HttpResponseTimeRrd.class.getPackage().getName());
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
		pd.add("Connect", ProbeDesc.GAUGE);
		pd.add("First Byte", ProbeDesc.GAUGE);
		pd.add("Last Byte", ProbeDesc.GAUGE);
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
		ByteArrayOutputStream digestOs = new ByteArrayOutputStream(digestval.length * 2);
		try {
			MimeUtility.encode(digestOs, "base64").write(digestval);
			retval = "url-" + digestOs.toString();
			retval = retval.replace('/', '_');
		} catch (IOException e) {
		} catch (MessagingException e) {
		}
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
