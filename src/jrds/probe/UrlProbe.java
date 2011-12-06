package jrds.probe;

import java.net.URL;


/**
 * @author Fabrice Bacchella
 *
 */
public interface UrlProbe {
	public String getUrlAsString();
	public URL getUrl();
	public Integer getPort();
}
