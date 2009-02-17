package jrds;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import org.apache.commons.digester.Digester;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public abstract class DirXmlParser {
	private final Logger logger = Logger.getLogger(DirXmlParser.class);

	final Pattern p = Pattern.compile(".*.xml");
	final FileFilter filter = new  FileFilter(){
		public boolean accept(File file) {
			if(! file.isHidden()) {
				if(file.isDirectory())
					importDir(file);
				else if(file.isFile() && file.getName().endsWith(".xml")) {
					try {
						logger.trace("Parsing " + file.getName());
						InputStream xmlStream = new FileInputStream(file);
						digester.parse(xmlStream);
						xmlStream.close();
					} catch (FileNotFoundException e) {
						logger.error("File  "+ file + " cannot be read: " + e);
					} catch (IOException e) {
						logger.error("File  "+ file + " cannot be read: " + e);
					} catch (SAXParseException e1) {
						logger.error(file.getName() + ": " +
								"Parse Error at line " + e1.getLineNumber() +
								" column " + e1.getColumnNumber() + ": " +
								e1.getMessage());
						digester.clear();
					} catch (SAXException e) {
						logger.error("File  "+ file + " not parsable: " + e, e);
					} catch (IllegalArgumentException e) {
						logger.error("File  "+ file + " not parsable: " + e, e);
					}
				}
			}
			return  false;
		}
	};
	public Digester digester = new Digester() {
		@Override
		public void error(SAXParseException exception) throws SAXException {
			/*logger.error("Parse error at line " + exception.getLineNumber() +
	                " column " + exception.getColumnNumber() + ": " +
	                exception.getMessage());*/
			throw exception;
		}

		@Override
		public void fatalError(SAXParseException exception) throws SAXException {
			log.error("Parse fatal error at line " + exception.getLineNumber() +
					" column " + exception.getColumnNumber() + ": " +
					exception.getMessage());
		}

		@Override
		public void warning(SAXParseException exception) throws SAXException {
			log.warn("Parse warning at line " + exception.getLineNumber() +
					" column " + exception.getColumnNumber() + ": " +
					exception.getMessage());
		}

	};

	public DirXmlParser() {
		init();
		digester.register("-//jrds//DTD Graph Description//EN", digester.getClass().getResource("/graphdesc.dtd").toString());
//		digester.register("-//jrds//DTD View//EN", digester.getClass().getResource("/view.dtd").toString());
		digester.register("-//jrds//DTD Probe Description//EN", digester.getClass().getResource("/probedesc.dtd").toString());
		digester.register("-//jrds//DTD Filter//EN", digester.getClass().getResource("/filter.dtd").toString());
	}

	public DirXmlParser(Digester d) {
		digester = d;
		init();
		digester.register("-//jrds//DTD Graph Description//EN", digester.getClass().getResource("/graphdesc.dtd").toString());
//		digester.register("-//jrds//DTD View//EN", digester.getClass().getResource("/view.dtd").toString());
		digester.register("-//jrds//DTD Probe Description//EN", digester.getClass().getResource("/probedesc.dtd").toString());
		digester.register("-//jrds//DTD Filter//EN", digester.getClass().getResource("/filter.dtd").toString());
	}

	abstract public void init();

	public boolean importDescUrl(URL ressourceUrl) throws IOException {
		logger.debug("Importing " + ressourceUrl);
		String path = ressourceUrl.toString();
		if(path != null) {
			String [] urlelems = path.split("[:!]");
			if("file".equals(urlelems[0])) {
				String fileName = urlelems[1];
				File imported = new File(fileName);
				if(imported.isDirectory())
					importDir(imported);
				else if(fileName.endsWith(".jar")) {
					importJar(imported, "");
					return true;
				}
			}
			else if("jar".equals(urlelems[0])) {
				String root= "";
				if(! "".equals(urlelems[3]))
					root = urlelems[3];
				importJar(new File(urlelems[2]), root);
				return true;
			}
		}
		else {
			logger.error("ressource " + ressourceUrl + "can't be loaded" );
		}
		return false;
	}

	/**
	 * Recursively walk a directory tree and return a List of all
	 * Files found; the List is sorted using File.compareTo.
	 *
	 * @param aStartingDir is a valid directory, which can be read.
	 */
	private void importDir(File filePath) {
		if(filePath.isDirectory()) 
			filePath.listFiles(filter);
	}

	private void importJar(File jarfile, String root) throws IOException {
		logger.trace("Importing jar " + jarfile);
		JarFile probesjar = new JarFile(jarfile);
		for(JarEntry je: Collections.list(probesjar.entries())) {
			if( !je.isDirectory() && je.getName().endsWith(".xml")) {
				InputStream xmlStream = probesjar.getInputStream(je);
				try {
					logger.trace("Parsing jar:" + jarfile.toURL() + "!/" + je);
					digester.parse(xmlStream);
				} catch (SAXParseException e1) {
					logger.error("jar:" + jarfile.toURL() + "!/" + je + ": " +
							"Parse Error at line " + e1.getLineNumber() +
							" column " + e1.getColumnNumber() + ": " +
							e1.getMessage());
					digester.clear();
				} catch (SAXException e) {
					logger.error("jar:" + jarfile.toURL() + "!/" + je + " not parsable: " + e, e);
				}
				xmlStream.close();
			}
		}
	}

}
