package jrds;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import org.junit.BeforeClass;
import org.junit.Test;

public class JarUrlPlay {
	static final private Logger logger = Logger.getLogger(JarUrlPlay.class);

	@BeforeClass
	static public void configure() throws IOException {
		Tools.configure();
		Tools.setLevel(logger, Level.TRACE);
	}

	@Test
	public void play() throws IOException, URISyntaxException {
		Object[] cList = new Object[]{ getClass(), String.class, "/DejaVuSansMono-Bold.ttf", logger.getClass()} ;
		for(Object o: cList) {
			String path = ressourcePath(o);
			URL rsrcUrl = this.getClass().getResource(path);
			String protocol = rsrcUrl.getProtocol();
			logger.debug(o);
			logger.debug("    found at " + rsrcUrl);
			logger.debug("    path:" + rsrcUrl.getPath());
			logger.debug("    query:" + rsrcUrl.getQuery());
			logger.debug("    ref:" + rsrcUrl.getRef());
			URL descUrl = null;
			URL rootUrl = null;
			if("jar".equals(protocol)) {
				JarURLConnection cnx = (JarURLConnection) rsrcUrl.openConnection();
				rootUrl = new URL("jar:" + cnx.getJarFileURL() +"!/");
				descUrl = new URL(rsrcUrl,"/desc");
			}
			else if("file".equals(protocol)) {
				rootUrl = new URL("file:" +  rsrcUrl.getFile().replaceFirst(path, ""));
				descUrl = new URL(rsrcUrl, "desc");
			}
			logger.debug("    Potential desc url: " + descUrl);
			logger.debug("    Root url: " + rootUrl);
			URLConnection cnx = rootUrl.openConnection();
			logger.debug("    Root connection: " + cnx);
		}
	}

	private String ressourcePath(Object o) {
		if(o instanceof Class<?>) {
			Class<?> c = (Class<?>) o;
			return "/".concat(c.getName().replace(".", "/").concat(".class"));
		}
		else if(o instanceof String) {
			return (String) o;
		}
		return "";
	}
	
	@Test
	public void loader() {
		String[] cList = new String[]{ "filter.dtd", "/filter.dtd", "jrds", "/jrds", "/DejaVuSansMono-Bold.ttf", "/desc", "desc", "/ressources/args.xml", "ressources/args.xml"} ;
		for(String ressource: cList) {
			logger.trace("Looking for " + ressource);
			logger.trace("    using class loader: " + getClass().getClassLoader().getResource(ressource));
			logger.trace("    using test class: " + getClass().getResource(ressource));
		}
	}
	
} 
