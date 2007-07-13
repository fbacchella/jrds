package jrds;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;
import org.apache.log4j.Logger;
import org.rrd4j.DsType;
import org.snmp4j.smi.OID;
import org.xml.sax.Attributes;

public class DescFactory extends DirXmlParser {
	private ArgFactory af;
	private Map<String, ProbeDesc> probesDescMap = new HashMap<String, ProbeDesc>();
	private Map<String, GraphDesc> graphDescMap = new HashMap<String, GraphDesc>();
	public class ProbeClassLoader extends URLClassLoader {
		public ProbeClassLoader(ClassLoader parent) {
			super(new URL[0], parent);
		}

		@Override
		public void addURL(URL arg0) {
			super.addURL(arg0);
		}
	}

	public final ProbeClassLoader probeLoader = new ProbeClassLoader(DescFactory.class.getClassLoader());
	private final Logger logger = Logger.getLogger(DescFactory.class);
	
	public DescFactory(ArgFactory af) {
		this.af = af;
	}

	void init() {
		addProbeDescDigester(digester);
		addGraphDescDigester(digester);
		FilterXml.addToDigester(digester);
		digester.setValidating(true);
	}

	public void importJar(String jarfile) throws IOException {
		probeLoader.addURL(new URL("file:" + jarfile));
		super.importJar(jarfile);
	}

	private void addProbeDescDigester(Digester digester) {
		digester.register("-//jrds//DTD Probe Description//EN", digester.getClass().getResource("/probedesc.dtd").toString());
		digester.addObjectCreate("probedesc", jrds.ProbeDesc.class);
		digester.addRule("probedesc", new Rule() {
			@Override
			public void end(String namespace, String name) throws Exception {
				ProbeDesc pd = (ProbeDesc) digester.peek();
				probesDescMap.put(pd.getName(), pd);
			}
		});

		digester.addRule("probedesc/probeClass", new Rule() {
			public void body(String namespace, String name, String text) {
				ProbeDesc pd = (ProbeDesc) digester.peek();
				try {
					Class c = probeLoader.loadClass(text.trim());
					pd.setProbeClass(c);
				} catch (ClassNotFoundException e) {
					logger.debug("Invalid probe description for " + pd.getName() + ": class " + text + " not found");
					logger.debug("Looking in " + Arrays.asList(probeLoader.getURLs()));
				}
			}
		});
		digester.addCallMethod("probedesc/probeName", "setProbeName", 0);
		digester.addCallMethod("probedesc/name", "setName", 0);

		Class[] params = new Class[] { Boolean.class};
		digester.addCallMethod("probedesc/uniq", "setUniqIndex", 0, params);

		digester.addRule("probedesc/index", new Rule() {
			@Override
			public void body(String namespace, String name, String text) throws Exception {
				Object o = null;
				for(int i = 0; i< digester.getCount() && !((o = digester.peek(i)) instanceof ProbeDesc) ; i++);
				if(o instanceof ProbeDesc ) {
					ProbeDesc pd = (ProbeDesc) o;
					pd.addSpecific(jrds.probe.snmp.RdsIndexedSnmpRrd.INDEXOIDNAME, text.trim());
				}
				else {
					logger.error("No probe desc on stack");
				}
			}
		});

		digester.addRule("probedesc/defaultargs/arg", new Rule() {
			@SuppressWarnings("unchecked")
			public void begin(String namespace, String name, Attributes attributes) throws Exception {
				String type = attributes.getValue("type");
				String value = attributes.getValue("value");
				Object arg = af.makeArg(type, value);
				if(arg != null) {
					Object o = null;
					for(int i = 0; i< digester.getCount() && !((o = digester.peek(i)) instanceof ProbeDesc) ; i++);
					if(o instanceof ProbeDesc ) {
						ProbeDesc pd = (ProbeDesc) o;
						pd.addDefaultArg(arg);
					}
					else {
						logger.error("No probe desc on stack");
					}
				}
				else {
					logger.error("Object of type " + type  + " and value" + value + " can't be instancied");
				}
			}
		});

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
				DsType type = null;
				if( !"NONE".equals(text.trim().toUpperCase()))
					type = DsType.valueOf(text.trim().toUpperCase());
				m.put("dsType", type);
			}
		});
		digester.addRule("probedesc/ds/oid", new Rule() {
			@SuppressWarnings("unchecked")
			public void body (String namespace, String name, String text) {
				Map m  = (Map) digester.peek();
				m.put("collectKey", new OID(text.trim()));
			}	
		});
		digester.addRule("probedesc/ds/collect", new Rule() {
			@SuppressWarnings("unchecked")
			public void body (String namespace, String name, String text) {
				Map m  = (Map) digester.peek();
				m.put("collectKey", text.trim());
			}	
		}
		);
		digester.addRule("probedesc/snmpRequester", new Rule() {
			public void body(java.lang.String namespace, java.lang.String name, java.lang.String text) throws IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException  {
				ProbeDesc pd = (ProbeDesc) digester.peek();
				pd.addSpecific("requester", text.trim());
			}
		});
		digester.addRule("probedesc/specific", new Rule() {
			String specificName = "";
			public void begin(String namespace, String name, Attributes attributes) throws Exception {
				specificName = attributes.getValue("name");
			}
			public void body(java.lang.String namespace, java.lang.String name, java.lang.String text) throws IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException  {
				ProbeDesc pd = (ProbeDesc) digester.peek();
				pd.addSpecific(specificName, text.trim());
				logger.trace("Add specific " + specificName + ":<" + text.trim() +">");
				specificName = "";
			}
		});
		digester.addObjectCreate("probedesc/graphs", ArrayList.class);
		digester.addSetNext("probedesc/graphs","setGraphClasses");

		params = new Class[] { Object.class};
		digester.addCallMethod("probedesc/graphs/name", "add", 0, params);

	}

	private void addGraphDescDigester(Digester digester) {
		digester.register("-//jrds//DTD Graph Description//EN", digester.getClass().getResource("/graphdesc.dtd").toString());
		digester.addObjectCreate("graphdesc", jrds.GraphDesc.class);
		digester.addSetProperties("graphdesc");
		digester.addRule("graphdesc", new Rule() {
			@Override
			public void end(String namespace, String name) throws Exception {
				GraphDesc gd = (GraphDesc) digester.peek();
				graphDescMap.put(gd.getName(), gd);
			}
		});
		digester.addCallMethod("graphdesc/name", "setName", 0);
		digester.addCallMethod("graphdesc/graphName", "setGraphName", 0);
		digester.addCallMethod("graphdesc/verticalLabel", "setVerticalLabel", 0);
		digester.addCallMethod("graphdesc/graphTitle", "setGraphTitle", 0);
		digester.addCallMethod("graphdesc/upperLimit", "setUpperLimit", 0);
		digester.addCallMethod("graphdesc/lowerLimit", "setLowerLimit", 0);
		digester.addRule("graphdesc/unit/binary", new Rule() {
			@Override
			public void body(String namespace, String name, String text) {
				GraphDesc gd = (GraphDesc) digester.peek();
				gd.setSiUnit(false);
			}
		});
		digester.addRule("graphdesc/unit/SI", new Rule() {
			@Override
			public void body(String namespace, String name, String text) {
				GraphDesc gd = (GraphDesc) digester.peek();
				gd.setSiUnit(true);
			}
		});
		digester.addRule("graphdesc/unit/base", new Rule() {
			@SuppressWarnings("unchecked")
			public void body(String namespace, String name, String text) {
				GraphDesc gd = (GraphDesc) digester.peek();
				gd.setUnitExponent(text.trim());
			}	
		}
		);
		digester.addCallMethod("graphdesc/add","add",7);
		digester.addCallParam("graphdesc/add/name",0);
		digester.addCallParam("graphdesc/add/rpn",1);
		digester.addCallParam("graphdesc/add/graphType",2);
		digester.addCallParam("graphdesc/add/color",3);
		digester.addCallParam("graphdesc/add/legend",4);
		digester.addCallParam("graphdesc/add/cf",5);
		digester.addCallParam("graphdesc/add/reversed",6);
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

	public Map<String, ProbeDesc> getProbesDescMap() {
		return probesDescMap;
	}

	public Map<String, GraphDesc> getGraphDescMap() {
		return graphDescMap;
	}
}
