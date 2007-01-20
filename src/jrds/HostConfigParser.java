//----------------------------------------------------------------------------
//$Id: HostConfigParser.java 237 2006-03-03 14:57:55 +0100 (ven., 03 mars 2006) fbacchella $
package jrds;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jrds.probe.IndexedProbe;
import jrds.probe.SumProbe;
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
	ArgFactory af;

	private final class Tag {
		String tag;

		public final String getTag() {
			return tag;
		}

		public final void setTag(String tag) {
			this.tag = tag;
		}
	};

	public HostConfigParser(ProbeFactory pf, ArgFactory af) {
		super();
		this.pf = pf;
		this.af = af;
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
	}

	private void defineProbe(Digester digester) {
		Rule makeProbe = new Rule() {
			public void begin(String namespace, String name, Attributes attributes) throws Exception {
				String type = attributes.getValue("type");
				if(type == null) {
					logger.error("No type found at " + digester.getCurrentElementName());
				}
				else {
					//We will need the atrributes latter, so keep them on the stack
					Map<String, String> attrMap = new HashMap<String, String>(attributes.getLength());
					for(int i=attributes.getLength() - 1; i >= 0; i--) {
						attrMap.put(attributes.getQName(i), attributes.getValue(i));
					}
					digester.push(attrMap);
					digester.push(new ArrayList(0));
				}
			}

			@SuppressWarnings("unchecked")
			public void end(String namespace, String name) throws Exception {
				List argsListValue = null;
				Set<Starter> starters = new HashSet<Starter>();
				Set<Tag> tags = new HashSet<Tag>();
				Map<String, String> attributes = null;

				//All the informations for the probe were kept on the stack
				//The delimiter is a Map 
				Object o = null;
				while(digester.getCount() != 0 && ! ((o = digester.pop()) instanceof Map)) {
					if(o instanceof Starter) {
						starters.add((Starter) o);
					}
					else if(o instanceof  ArrayList) {
						argsListValue = (List) o;
					}
					else if(o instanceof Tag) {
						tags.add((Tag) o);
					}
					else {
						logger.error("found " + o + " on the stack, what do i do with that ? ");
						if(o != null)
							digester.push(o);
						break;
					}
				}
				if(o != null && o instanceof Map) {
					attributes = (Map) o;
					String probeType = attributes.get("type");
					for(int i = 0; i < digester.getCount() && !((o = digester.peek(i)) instanceof RdsHost) ; i++);
					RdsHost host = (RdsHost) o;
					Probe newProbe = pf.makeProbe(probeType, argsListValue);
					if(newProbe != null) {
						for(Starter s: starters) {
							newProbe.addStarter(s);
						}
						for(Tag tg: tags)
							newProbe.addTag(tg.getTag());
						String label = attributes.get("label");
						if(newProbe instanceof IndexedProbe && label != null) {
							logger.debug("Adding label " + label + " to "  + newProbe);
							((IndexedProbe)newProbe).setLabel(label);
						}
						host.addProbe(newProbe);
						HostsList.getRootGroup().addProbe(newProbe);
					}
				}
				else {
					logger.error("hitting invalid digester stack for a probe, internal error " + digester);
					StringBuffer stack = new StringBuffer();
					for(int i = 0; i < digester.getCount(); i++) {
						stack.append(digester.peek(i).toString());
						stack.append("\n");
					}
					logger.error("Stack dump:\n" + stack);
				}
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

				//If we are on a host, the starter must be registered right now
				for(int i = 0; i< digester.getCount() ; i++){
					Object o = digester.peek(i);
					if(o instanceof RdsHost) {
						((RdsHost) o).addStarter(starter);
						break;
					}
					else if(Probe.class.isAssignableFrom(o.getClass())) {
						digester.push(starter);
						break;
					}
				}

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
				Object arg = af.makeArg(type, value);
				if(arg != null) {
					Object o = null;
					for(int i = 0; i< digester.getCount() && !((o = digester.peek(i)) instanceof List) ; i++);
					List<Object> argsListValue = (List<Object>) o;
					argsListValue.add(arg);
				}
				else {
					logger.error("Object of type " + type  + " and value" + value + " can't be instancied");
				}
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
						m.put(p);
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
				//We will need the attributes latter, so keep them on the stack
				Map<String, String> attrMap = new HashMap<String, String>(attributes.getLength());
				for(int i=attributes.getLength() - 1; i >= 0; i--) {
					attrMap.put(attributes.getQName(i), attributes.getValue(i));
				}
				digester.push(attrMap);
				digester.push(new ArrayList(0));
			}

			@SuppressWarnings("unchecked")
			public void end(String namespace, String name) throws Exception {
				List argsListValue = (List)digester.pop();
				Map <String, String> attrs = (Map) digester.pop();
				Object[] l = new Object[] {attrs, argsListValue};
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
				Collection<String> l = (Collection<String>) digester.pop();
				String sumName = (String) digester.pop();
				List argsListValue = new ArrayList(2);
				argsListValue.add(sumName);
				argsListValue.add(l);
				SumProbe newProbe = (SumProbe) pf.makeProbe(SumProbe.class.getName(), argsListValue);
				jrds.HostsList.getRootGroup().addSum(newProbe);
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
