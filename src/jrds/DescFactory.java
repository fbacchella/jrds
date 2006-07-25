package jrds;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import jrds.probe.SumProbe;
import jrds.snmp.SnmpRequester;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;
import org.apache.log4j.Logger;
import org.snmp4j.smi.OID;
import org.xml.sax.SAXException;

public class DescFactory {
	static private final Logger logger = Logger.getLogger(DescFactory.class);
	static final Pattern p = Pattern.compile(".*.xml");
	static final FileFilter filter = new  FileFilter(){
		public boolean accept(File file) {
			if(! file.isHidden()) {
				if(file.isDirectory())
					scanProbeDir(file);
				else if(file.isFile() && file.getName().endsWith(".xml")) {
					try {
						InputStream xmlStream = new FileInputStream(file);
						digester.parse(xmlStream);
						xmlStream.close();
					} catch (FileNotFoundException e) {
						logger.error("File  "+ file + " cannot be read: " + e);
					} catch (IOException e) {
						logger.error("File  "+ file + " cannot be read: " + e);
					} catch (SAXException e) {
						logger.error("File  "+ file + " not parsable: " + e, e);
					}
				}
			}
			return  false;
		}
	};
	static public Digester digester; 

	private DescFactory() {
	}

	public static void init() {
		digester = new Digester();
		addProbeDigester(digester);
		addGraphDigester(digester);
		FilterXml.addToDigester(digester);
		HostConfigParser.addDigester(digester);
		SumProbe.addDigester(digester);
		Macro.addToDigester(digester);

		String probepath = DescFactory.class.getResource("/probe").toString();
		logger.debug("probes jar path: " + probepath);
		//Quick hack for Gentoo's tomcat 5
		String graphpath = DescFactory.class.getResource("/graph").toString();
		logger.debug("graphs jar path: " + graphpath);
		String filterpath = DescFactory.class.getResource("/filter").toString();
		logger.debug("filter jar path: " + filterpath);
		String path = probepath;
		while(path != null) {
			String [] urlelems = path.split("[:!]");
			if("file".equals(urlelems[0]))
				scanProbeDir(new File(urlelems[1]));
			else if("jar".equals(urlelems[0]))
				importJar(urlelems[2]);
			if(graphpath != probepath) {
				path = graphpath;
				graphpath = probepath;
			}
			else if(filterpath != probepath) {
				path = filterpath;
				filterpath = probepath;
			}
			else
				path = null;
		}
	}


	public static void importJar(String jarfile) {
		JarFile probesjar;
		try {
			probesjar = new JarFile(jarfile);
			Enumeration e = probesjar.entries();
			while(e.hasMoreElements()) {
				ZipEntry z = (ZipEntry) e.nextElement();
				if( !z.isDirectory() && p.matcher(z.getName()).matches()) {
					InputStream xmlStream = probesjar.getInputStream(z);
					try {
						digester.parse(xmlStream);
					} catch (Exception e1) {
						System.out.println(z.getName());
						e1.printStackTrace();
					}
					xmlStream.close();
				}

			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}
	/**
	 * Recursively walk a directory tree and return a List of all
	 * Files found; the List is sorted using File.compareTo.
	 *
	 * @param aStartingDir is a valid directory, which can be read.
	 */
	static public void scanProbeDir(File aStartingDir) {
		if(aStartingDir.isDirectory()) 
			aStartingDir.listFiles(filter);
	}

	private static void addProbeDigester(Digester digester) {
		digester.register("-//jrds//DTD Probe Description//EN", digester.getClass().getResource("/probedesc.dtd").toString());
		digester.addObjectCreate("probedesc", jrds.ProbeDesc.class);
		digester.addRule("probedesc", new Rule() {
			@Override
			public void end(String namespace, String name) throws Exception {
				ProbeDesc pd = (ProbeDesc) digester.peek();
				ProbeFactory.addDesc(pd);
			}
		});

		digester.addRule("probedesc/probeClass", new Rule() {
			public void body(String namespace, String name, String text) throws ClassNotFoundException {
				Class c = Class.forName(text.trim());
				ProbeDesc pd = (ProbeDesc) digester.peek();
				pd.setProbeClass(c);
			}
		});
		digester.addCallMethod("probedesc/probeName", "setProbeName", 0);
		digester.addCallMethod("probedesc/name", "setName", 0);

		Class[] params = new Class[] { Boolean.class};
		digester.addCallMethod("probedesc/uniq", "setUniqIndex", 0, params);

		params = new Class[] { String.class};
		digester.addCallMethod("probedesc/index", "setIndexOid", 0, params);

		digester.addObjectCreate("probedesc/ds", HashMap.class);
		digester.addSetNext("probedesc/ds", "add");
		digester.addRule("probedesc/ds/dsName", new Rule() {
			@SuppressWarnings("unchecked")
			public void body(java.lang.String namespace, java.lang.String name, java.lang.String text) {
				Map m  = (Map) digester.peek();
				m.put("dsName", text);
			}
		});
		digester.addRule("probedesc/ds/dsType", new Rule() {
			@SuppressWarnings("unchecked")
			public void body(String namespace, String name, String text) throws IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException {
				Map m  = (Map) digester.peek();
				m.put("dsType", ProbeDesc.class.getField(text.trim().toUpperCase()).get(null));
			}
		});
		digester.addRule("probedesc/ds/oid", new Rule() {
			@SuppressWarnings("unchecked")
			public void body (String namespace, String name, String text) {
				Map m  = (Map) digester.peek();
				m.put("index", new OID(text.trim()));
			}	
		}
		);
		digester.addRule("probedesc/snmpRequester", new Rule() {
			public void body(java.lang.String namespace, java.lang.String name, java.lang.String text) throws IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException  {
				SnmpRequester req = (SnmpRequester) SnmpRequester.class.getField(text.toUpperCase()).get(null);
				ProbeDesc pd = (ProbeDesc) digester.peek();
				pd.setRequester(req);
			}
		});
		digester.addRule("probedesc/rmiClass", new Rule() {
			public void body(java.lang.String namespace, java.lang.String name, java.lang.String text) throws IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException  {
				ProbeDesc pd = (ProbeDesc) digester.peek();
				pd.setRmiClass(text.trim());
			}
		});
		digester.addObjectCreate("probedesc/graphs", ArrayList.class);
		digester.addSetNext("probedesc/graphs","setGraphClasses");

		params = new Class[] { Object.class};
		digester.addCallMethod("probedesc/graphs/name", "add", 0, params);

	}

	private static void addGraphDigester(Digester digester) {
		digester.register("-//jrds//DTD Graph Description//EN", digester.getClass().getResource("/graphdesc.dtd").toString());
		digester.setValidating(false);
		digester.addObjectCreate("graphdesc", jrds.GraphDesc.class);
		digester.addSetProperties("graphdesc");
		digester.addRule("graphdesc", new Rule() {
			@Override
			public void end(String namespace, String name) throws Exception {
				GraphDesc gd = (GraphDesc) digester.peek();
				GraphFactory.addDesc(gd);
			}
		});
		digester.addCallMethod("graphdesc/name", "setName", 0);
		digester.addCallMethod("graphdesc/graphName", "setGraphName", 0);
		digester.addCallMethod("graphdesc/verticalLabel", "setVerticalLabel", 0);
		digester.addCallMethod("graphdesc/graphTitle", "setGraphTitle", 0);
		digester.addCallMethod("graphdesc/upperLimit", "setUpperLimit", 0);
		digester.addCallMethod("graphdesc/add","add",7);
		digester.addCallParam("graphdesc/add/name",0);
		digester.addCallParam("graphdesc/add/dsName",1);
		digester.addCallParam("graphdesc/add/rpn",2);
		digester.addCallParam("graphdesc/add/graphType",3);
		digester.addCallParam("graphdesc/add/color",4);
		digester.addCallParam("graphdesc/add/legend",5);
		digester.addCallParam("graphdesc/add/cf",6);
		digester.addObjectCreate("graphdesc/hosttree", java.util.ArrayList.class);
		digester.addSetNext("graphdesc/hosttree", "setHostTree");
		digester.addObjectCreate("graphdesc/viewtree", java.util.ArrayList.class);
		digester.addSetNext("graphdesc/viewtree", "setViewTree");
		digester.addRule("*/pathelement", new Rule() {
			@SuppressWarnings("unchecked")
			public void body (String namespace, String name, String text) {
				List tree = (List) getDigester().peek();
				tree.add(GraphDesc.resolvPathElement(text));
			}	
		}
		);
		digester.addRule("*/pathstring", new Rule() {
			@SuppressWarnings("unchecked")
			public void body (String namespace, String name, String text) {
				List tree = (List) getDigester().peek();
				tree.add(text);
			}	
		}
		);

	}
}
