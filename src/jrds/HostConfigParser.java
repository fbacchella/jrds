//----------------------------------------------------------------------------
//$Id: HostConfigParser.java 237 2006-03-03 14:57:55 +0100 (ven., 03 mars 2006) fbacchella $
package jrds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jrds.snmp.SnmpStarter;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;
import org.apache.log4j.Logger;
import org.xml.sax.Attributes;


/**
 * Used to parse config.xml
 * @author Fabrice Bacchella
 * @version $Revision: 237 $
 */
public class HostConfigParser  extends DirXmlParser {

	private final Logger logger = Logger.getLogger(HostConfigParser.class);
	private Map<String, Macro> macroList = new HashMap<String, Macro>();
	ProbeFactory pf;

	private final class Tag {
		String tag;

		public final String getTag() {
			return tag;
		}

		public final void setTag(String tag) {
			this.tag = tag;
		}
	};

	public HostConfigParser(ProbeFactory pf) {
		super();
		this.pf = pf;
	}

	void init() {
		digester.setValidating(false);
		defineSumDigester(digester);
		defineSnmp(digester);
		defineMacro(digester);
		defineArg(digester);
		defineProbe(digester);
		defineHost(digester);
		defineTag(digester);
		FilterXml.addToDigester(digester);
	}

	private void defineHost(Digester digester) {
		//How to create an host
		Rule cleanStack = new Rule() {
			public void begin(String namespace, String name, Attributes attributes) throws Exception {
				String hostName = attributes.getValue("name");
				RdsHost host = new RdsHost(hostName);
				HostsList.getRootGroup().addHost(host);
				digester.push(host);
			}
			public void end(String namespace, String name) throws Exception {
				Set<Tag> tags = new HashSet<Tag>();
				Set<Starter> starters = new HashSet<Starter>();
				Object o = null;
				while(digester.getCount() != 0 && ! ((o = digester.pop()) instanceof RdsHost)) {
					if(o instanceof Tag) {
						tags.add((Tag) o);
					}
					else if(o instanceof Starter) {
						starters.add((Starter) o);
					}
				}
				if( o != null) {
					RdsHost host = (RdsHost) o;
					for(Tag tg: tags)
						host.addTag(tg.getTag());
					for(Starter s: starters) {
						host.addStarter(s);
					}
				}
				else
					logger.error("hitting empty digester stack for an host, internal error");
			}			
		};
		digester.addRule("host",cleanStack);


		//End
		/*hostsCollection.add(lastHost);
		 lastHost = null;
		 */
	}

	private void defineProbe(Digester digester) {
		Rule makeProbe = new Rule() {
			public void begin(String namespace, String name, Attributes attributes) throws Exception {
				//We will need the type latter, so keep it on the stack
				digester.push(attributes.getValue("type"));
				digester.push(new ArrayList(0));
			}

			public void end(String namespace, String name) throws Exception {
				List argsListValue = null;
				Set<Starter> starters = new HashSet<Starter>();
				Set<Tag> tags = new HashSet<Tag>();

				//All the informations for the probe were kept on the stack
				//The delimiter is a string (the type of the prope)
				//Should it be a specific null delimiter ?
				Object o = null;
				while(digester.getCount() != 0 && ! ((o = digester.pop()) instanceof String)) {
					if(o instanceof Starter) {
						starters.add((Starter) o);
					}
					else if(o instanceof  ArrayList) {
						argsListValue = (List) o;
					}
					else if(o instanceof Tag) {
						tags.add((Tag) o);
					}
				}
				if(o != null) {
					String probeType = (String) o;
					for(int i = 0; i < digester.getCount() && !((o = digester.peek(i)) instanceof RdsHost) ; i++);
					RdsHost host = (RdsHost) o;
					Probe newProbe = pf.makeProbe(probeType, host, argsListValue);
					if(newProbe != null) {
						for(Starter s: starters) {
							newProbe.addStarter(s);
						}
						for(Tag tg: tags)
							newProbe.addTag(tg.getTag());
						HostsList.getRootGroup().addProbe(newProbe);
						host.addProbe(newProbe);
					}
				}
				else
					logger.error("hitting empty digester stack for a probe, internal error");
			}
		};

		digester.addRule("host/probe/", makeProbe);
		digester.addRule("host/rrd/", makeProbe);
	}


	/**
	 * What to do with a snmp element
	 * @param digester
	 */
	void defineSnmp(Digester digester) {
		Rule addSnmpStarterRule= new Rule() {
			public void begin(String namespace, String name, Attributes attributes) throws Exception {
				SnmpStarter starter = new SnmpStarter();
				starter.setCommunity(attributes.getValue("community"));
				starter.setVersion(attributes.getValue("version"));
				starter.setPort(attributes.getValue("port"));
				String hostName = attributes.getValue("host");
				if(hostName == null) {
					Object o = null;
					for(int i = 0; i< digester.getCount() && !((o = digester.peek(i)) instanceof RdsHost) ; i++);
					RdsHost host = (RdsHost) o;
					hostName = host.getName();
				}
				starter.setHostname(hostName);
				digester.push(starter);
			}
		};

		//the target is just pushed on the stack
		//It will be used at the end of the probe or the host, if it exists
		digester.addRule("host/snmp", addSnmpStarterRule);
		digester.addRule("host/rrd/snmp", addSnmpStarterRule);
		digester.addRule("host/probe/snmp", addSnmpStarterRule);
	}

	void defineArg(Digester digester) {
		Rule storeArg = new Rule() {
			@SuppressWarnings("unchecked")
			public void begin(String namespace, String name, Attributes attributes) throws Exception {
				String type = attributes.getValue("type");
				String value = attributes.getValue("value");
				Object arg = pf.makeArg(type, value);
				Object o = null;
				for(int i = 0; i< digester.getCount() && !((o = digester.peek(i)) instanceof List) ; i++);
				List<Object> argsListValue = (List<Object>) o;
				argsListValue.add(arg);
			}
		};

		digester.addRule("*/arg", storeArg);
	}

	/**
	 * What to do when a macro definition is found
	 * @param d
	 */
	void defineMacro(Digester d) {
		d.addRule("host/macro", new Rule() {
			public void begin (String namespace, String name, Attributes attributes) {
				String macroName = attributes.getValue("name");
				if(macroName != null) {
					Macro m = macroList.get(macroName);
					if (m != null) {
						Object o = null;
						for(int i = 0; i< digester.getCount() && !((o = digester.peek(i)) instanceof RdsHost) ; i++);
						RdsHost host = (RdsHost) o;
						m.populate(host);
					}
					else
						logger.error("Macro " + macroName + " not found");
				}
			}	
		}
		);
		d.addRule("macrodef", new Rule() {
			public void begin (String namespace, String name, Attributes attributes) {
				String macroName = attributes.getValue("name");
				Macro m = new Macro(pf);
				macroList.put(macroName, m);
				digester.push(m);
			}	
			public void end(String namespace, String name) throws Exception {
				Set<Tag> tags = new HashSet<Tag>();
				Set<Object[]> probes = new HashSet<Object[]>();
				Object o = null;
				while(digester.getCount() != 0 && ! ((o = digester.pop()) instanceof Macro)) {
					if(o instanceof Tag) {
						tags.add((Tag) o);
					}
					else if(o instanceof Object[]) {
						probes.add((Object[]) o);
					}
				}
				if( o instanceof Macro) {
					Macro m = (Macro) o;
					for(Tag tg: tags)
						m.addTag(tg.getTag());
					for(Object[] p: probes)
						m.put((String) p[0], (List)p[1]);
					digester.push(m);
				}
				else
					logger.error("hitting empty digester stack for an host, internal error");
			}			
		}
		);
		Rule makeProbe = new Rule() {
			@Override
			public String toString() {
				return "MacroProbeAdd";
			}

			public void begin(String namespace, String name, Attributes attributes) throws Exception {
				//We will need the type latter, so keep it on the stack
				digester.push(attributes.getValue("type"));
				digester.push(new ArrayList(0));
			}

			public void end(String namespace, String name) throws Exception {
				List argsListValue = (List)digester.pop();
				String macroType = (String)digester.pop();
				Object[] l = new Object[] {macroType, argsListValue};
				digester.push(l);
			}
		};
		digester.addRule("macrodef/probe/", makeProbe);
		digester.addRule("macrodef/rrd/", makeProbe);
	}

	void defineTag(Digester d) {
		Rule tagRule =  new Rule() {
			public void body(java.lang.String namespace, java.lang.String name, java.lang.String text) {
				Tag t = new HostConfigParser.Tag();
				t.setTag(text);
				digester.push(t);
			}
			@Override
			public String toString() {
				return "Tag";
			}
		};
		d.addRule("*/tag", tagRule);
	}
	
	private void defineSumDigester(Digester digester) {
//		digester.register("-//jrds//DTD View//EN", digester.getClass().getResource("/view.dtd").toString());
		digester.addRule("sum/", new Rule() {
			public void begin (String namespace, String name, Attributes attributes) {
				String sumName = attributes.getValue("name");
				digester.push(sumName);
				digester.push(new ArrayList());
			}
			@SuppressWarnings("unchecked")
			public void end(String namespace, String name) throws Exception {
				List<String> l = (List<String>) digester.pop();
				String sumName = (String) digester.pop();
				jrds.HostsList.getRootGroup().addSum(sumName, l);
			}
		});
		digester.addRule("sum/element/", new Rule() {
			@SuppressWarnings("unchecked")
			public void begin (String namespace, String name, Attributes attributes) {
				String elementName = attributes.getValue("name");
				List<String> l = (List<String>) digester.peek();
				l.add(elementName);
			}
		});
	}

}
