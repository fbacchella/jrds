/*
 * Created on 22 nov. 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package jrds.probe;

import java.net.URL;

import jrds.objects.probe.UrlProbe;

/**
 * @author bacchell
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public final class HttpResponseTime extends ExternalCmdProbe implements UrlProbe {
	private URL url;
	
	public void configure(URL url)
	{
		this.url = url;
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

	/* (non-Javadoc)
	 * @see jrds.probe.UrlProbe#getPort()
	 */
	public int getPort() {
		return getUrl().getPort();
	}
}
