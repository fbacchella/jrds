package jrds.factories;

import java.io.File;
import java.io.FileFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.HashSet;

import org.apache.log4j.Logger;

import jrds.PropertiesManager;

public class ExtensionClassLoader extends ClassLoader {
	static final private Logger logger = Logger.getLogger(ExtensionClassLoader.class);

	URLClassLoader cl;

	final FileFilter filter = new  FileFilter(){
		public boolean accept(File file) {
			return  (! file.isHidden()) && file.isFile() && file.getName().endsWith(".jar");
		}
	};

	public ExtensionClassLoader(PropertiesManager pm) {
		init(pm);
	}

	void init(PropertiesManager pm) {
		Collection<URL> urls = new HashSet<URL>();

		String extensionPath = pm.extensiondir;

		if(extensionPath != null) {
			logger.debug("Setting class directories to: " + extensionPath);

			File path = new File(extensionPath);

			if(path.isDirectory()) {
				for(File f: path.listFiles(filter)) {
					try {
						urls.add(f.toURL());
					} catch (MalformedURLException e) {
					}
				}
			}
		}

		for(URL u: pm.libspath) {
			urls.add(u);
		}

		URL[] arrayUrl = new URL[urls.size()];
		urls.toArray(arrayUrl);
		cl = new URLClassLoader(arrayUrl, this.getClass().getClassLoader());
	}


	/* (non-Javadoc)
	 * @see java.lang.ClassLoader#loadClass(java.lang.String)
	 */
	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		logger.trace("Loading class: " + name);
		Class<?> c;
		c = cl.loadClass(name);
		return c;
	}

}
