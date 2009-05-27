/*
 * Created on 7 janv. 2005
 *
 * TODO 
 */
package jrds.probe;

import java.net.URL;


/**
 * @author bacchell
 *
 * TODO 
 */
public interface UrlProbe {
	public String getUrlAsString();
	public URL getUrl();
	public int getPort();
}
