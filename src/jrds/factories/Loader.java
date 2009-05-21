package jrds.factories;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpression;

import jrds.factories.xml.EntityResolver;
import jrds.factories.xml.JrdsNode;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class Loader {

	private static final EntityResolver urnResolver = new EntityResolver();

	public enum ConfigType {
		FILTER {
			final XPathExpression xpath = CompiledXPath.get("/filter/name");
			public boolean memberof(Node d) {
				return matchDocElement(d, "filter");
			}
			public XPathExpression getNameXpath() {
				return xpath;
			}
		},
		HOSTS {
			final XPathExpression xpath = CompiledXPath.get("/host/@name");
			public boolean memberof(Node d) {
				return matchDocElement(d, "host");
			}
			public XPathExpression getNameXpath() {
				return xpath;
			}
		},
		SUM {
			final XPathExpression xpath = CompiledXPath.get("/sum/@name");
			public boolean memberof(Node d) {
				return matchDocElement(d, "sum");
			}
			public XPathExpression getNameXpath() {
				return xpath;
			}
		},
		MACRODEF {
			final XPathExpression xpath = CompiledXPath.get("/macrodef/@name");
			public boolean memberof(Node d) {
				return matchDocElement(d, "macrodef");
			}
			public XPathExpression getNameXpath() {
				return xpath;
			}
		},
		GRAPH {
			final XPathExpression xpath = CompiledXPath.get("/graph/name");
			public boolean memberof(Node d) {
				return matchDocElement(d, "graph");
			}
			public XPathExpression getNameXpath() {
				return xpath;
			}
		},
		GRAPHDESC {
			final XPathExpression xpath = CompiledXPath.get("/graphdesc/name");
			public boolean memberof(Node d) {
				return matchDocElement(d, "graphdesc");
			}
			public XPathExpression getNameXpath() {
				return xpath;
			}
		},
		PROBEDESC {
			final XPathExpression xpath = CompiledXPath.get("/probedesc/name");
			public boolean memberof(Node d) {
				return matchDocElement(d, "probedesc");
			}
			public XPathExpression getNameXpath() {
				return xpath;
			}
		};

		public abstract boolean memberof(Node d);
		public abstract XPathExpression getNameXpath();

		private static boolean matchDocElement(Node d, String rootElement) {
			String root = d.getFirstChild().getNodeName();
			return rootElement.equals(root);
		}
	}
	static final private Logger logger = Logger.getLogger(Loader.class);

	private final FileFilter filter = new  FileFilter(){
		public boolean accept(File file) {
			return  (! file.isHidden()) && (file.isDirectory()) || (file.isFile() && file.getName().endsWith(".xml"));
		}
	};

	DocumentBuilder dbuilder = null;

	final private Map<ConfigType, Map<String, JrdsNode>> repositories = new HashMap<ConfigType, Map<String, JrdsNode>>(ConfigType.values().length);

	public Loader() throws ParserConfigurationException {
		DocumentBuilderFactory instance = DocumentBuilderFactory.newInstance();
		instance.setIgnoringComments(true);
		instance.setValidating(false);
		dbuilder = instance.newDocumentBuilder();
		dbuilder.setEntityResolver(urnResolver);

		for(ConfigType t: ConfigType.values()) {
			repositories.put(t, new  HashMap<String, JrdsNode>());
		}
	}

	public Map<ConfigType, Map<String, JrdsNode>> getRepositories(){
		return repositories;
	}

	public Map<String, JrdsNode> getRepository(ConfigType t) {
		return repositories.get(t);
	}

	public void loadPaths(List<URL> list) {
		for(URL u: list) {
			importUrl(u);
		}
	}

	public void importUrl(URL ressourceUrl) {
		logger.debug("Importing " + ressourceUrl);
		String protocol = ressourceUrl.getProtocol();
		if("file".equals(protocol)) {
			String fileName = ressourceUrl.getFile();
			File imported = new File(fileName);
			if(imported.isDirectory())
				importDir(imported);
			else if(fileName.endsWith(".jar"))
				importJar(imported);
		}
		else if("jar".equals(protocol)) {
			String [] urlelems = ressourceUrl.toString().split("[:!]");
			importJar(new File(urlelems[2]));
		}
		else {
			logger.error("ressource " + ressourceUrl + " can't be loaded" );
		}

	}

	public void importDir(File path) {
		if(! path.isDirectory()) {
			logger.warn(path + " is not a directory");
			return;
		}
		for(File f: path.listFiles(filter)) {
			if(f.isDirectory()) {
				importDir(f);
			}
			else {
				try {
					logger.trace("Will import " + f);
					if (! importStream(new FileInputStream(f)))
						logger.warn("Unknown type for " + f);
				} catch (FileNotFoundException e) {
					logger.error("File not found: " + f);
				} catch (SAXException e) {
					logger.error("Invalid xml document + " + f  + ": " + e);
				} catch (IOException e) {
					logger.error("IO error with " + f + ": " + e);
				}
			}
		}
	}

	public void importJar(File jarfile) {
		try {
			JarFile probesjar = new JarFile(jarfile);
			for(JarEntry je: Collections.list(probesjar.entries())) {
				String name = je.getName();
				if( !je.isDirectory() && name.endsWith(".xml") && (name.startsWith("desc/") || name.startsWith("graph/") || name.startsWith("probe/"))) {
					logger.trace("Will import " + je);
					try {
						if(! importStream(probesjar.getInputStream(je))) {
							logger.warn("Unknonw type " + je + " in jar " + jarfile);
						}
					} catch (SAXException e) {
						logger.error("Invalid xml document + " + je + " in " + jarfile + ": " + e);
					}
				}
			}
		} catch (IOException e) {
			logger.error("Invalid jar " + jarfile + ": " + e);
		}
	}

	boolean importStream(InputStream xmlstream) throws SAXException, IOException {
		boolean known = false;
		JrdsNode d = new JrdsNode(dbuilder.parse(xmlstream));
		for(ConfigType t: ConfigType.values()) {
			if(t.memberof(d)) {
				logger.trace("Found a " + t);
				known = true;
				//We check the Name
				JrdsNode n = d.getChild(t.getNameXpath());
				if(n != null && ! "".equals(n.getTextContent().trim())) {
					repositories.get(t).put(n.getTextContent().trim(), d);
					//A graph is also stored as a graphdesc
					//if(t == ConfigType.GRAPH) {
					//	repositories.get(ConfigType.GRAPHDESC).put(n.getTextContent().trim(), d);
					//}
				}
				break;
			}
		}
		return known;
	}
}
