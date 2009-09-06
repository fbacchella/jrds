package jrds.bootstrap;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class BootStrap {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		List<URL> classPath= new ArrayList<URL>();

		String path = ressourcePath(BootStrap.class);
		URL me = BootStrap.class.getResource(path);
		String protocol = me.getProtocol();
		URL rootUrl = null;
		if("jar".equals(protocol)) {
			JarURLConnection cnx = (JarURLConnection) me.openConnection();
			rootUrl = cnx.getJarFileURL();
		}
		else if("file".equals(protocol)) {
			rootUrl = me;
		}

		File file = new File(rootUrl.getFile());
		File baseClassPath = file.getParentFile();
		classPath.add(baseClassPath.toURI().toURL());

		FileFilter jarfilter = new  FileFilter(){
			public boolean accept(File file) {
				return file.isFile() && file.getName().endsWith(".jar");
			}
		};
		for(File f: baseClassPath.listFiles(jarfilter)) {
			classPath.add(f.toURI().toURL());
		}

		Map<String, String> configuration = new HashMap<String, String>();
		String propertiesFile = System.getProperty("propertiesFile");
		if (propertiesFile != null) {
			configuration.put("propertiesFile", propertiesFile);
		}
		
		//To remove WEB-INF/lib in the path and find the web root
		File webRoot = baseClassPath.getParentFile().getParentFile();
		if(webRoot.isDirectory()) {
			configuration.put("webRoot", webRoot.getAbsolutePath());
		}
		 
		try {
			ClassLoader cl = URLClassLoader.newInstance(classPath.toArray(new URL[classPath.size()]));
			Class<?>  jettyStarter = cl.loadClass("jrds.standalone.Jetty");
			CommandStarter cmd = (CommandStarter) jettyStarter.newInstance();
			cmd.configure(configuration);
			cmd.start();
		} catch (ClassNotFoundException e1) {
			System.err.println("JRDS installation not found");
			System.exit(1);
		} catch (IllegalArgumentException e) {
			e.getCause().printStackTrace();
		} catch (InstantiationException e) {
			e.getCause().printStackTrace();
		} catch (IllegalAccessException e) {
			e.getCause().printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (Exception e) {
			Throwable cause = e.getCause();
			if(cause != null) {
				System.err.println(e.getMessage() + ": " + cause.getMessage());
			}
			else
				e.printStackTrace();
		}
	}

	static private String ressourcePath(Object o) {
		if(o instanceof Class) {
			Class<?> c = (Class<?>) o;
			return "/".concat(c.getName().replace(".", "/").concat(".class"));
		}
		else if(o instanceof String) {
			return (String) o;
		}
		return "";
	}

}
