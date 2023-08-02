package jrds.probe;

import java.net.URL;

/**
 * @author Fabrice Bacchella
 *
 */
public interface UrlProbe {
	String getUrlAsString();
	URL getUrl();
	Integer getPort();
}
