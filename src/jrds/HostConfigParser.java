//----------------------------------------------------------------------------
//$Id: HostConfigParser.java 237 2006-03-03 14:57:55 +0100 (ven., 03 mars 2006) fbacchella $
package jrds;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
public class HostConfigParser {
	
	static private final Logger logger = Logger.getLogger(HostConfigParser.class);
	
	public HostConfigParser(File hostConfigFile)
	{
	}
	
	private static final class Tag {
		String tag;

		public String getTag() {
			return tag;
		}

		public void setTag(String tag) {
			this.tag = tag;
		}
	};
	
	private static void defineHost(Digester digester) {
		
		//How to create an host
		digester.addObjectCreate("host",jrds.RdsHost.class);
		digester.addSetProperties("host");

		Rule cleanStack = new Rule() {
			public void end(String namespace, String name) throws Exception {
				Set<Starter> starters = new HashSet<Starter>();
				Set<Macro> macros = new HashSet<Macro>();
				Set<Tag> tags = new HashSet<Tag>();
				Object o = null;
				while(digester.getCount() != 0 && ! ((o = digester.pop()) instanceof RdsHost)) {
					if(o instanceof Starter) {
						starters.add((Starter) o);
					}
					else if(o instanceof Macro) {
						macros.add((Macro) o);
					}
					else if(o instanceof Tag) {
						tags.add((Tag) o);
					}
				}
				if( o != null) {
					RdsHost host = (RdsHost) o;
					HostsList.getRootGroup().addHost(host);
					for(Starter s: starters) {
						host.addStarter(s);
					}
					for(Macro m: macros) {
						m.populate(host);
					}
					for(Tag tg: tags)
						host.addTag(tg.getTag());
					digester.push(host);
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
	
	private static void defineProbe(Digester digester) {
		Rule makeProbe = new Rule() {
			public void begin(String namespace, String name, Attributes attributes) throws Exception {
				//We will need the type latter, so keep it on the stack
				digester.push(attributes.getValue("type"));
				digester.push(new ArrayList(0));
			}

			public void end(String namespace, String name) throws Exception {
				List argsListValue = null;
				Starter s = null;
				Set<Tag> tags = new HashSet<Tag>();

				//All the informations for the probe were kept on the stack
				//The delimiter is a string (the type of the prope)
				//Should it be a specific null delimiter ?
				Object o = null;
				while(digester.getCount() != 0 && ! ((o = digester.pop()) instanceof String)) {
					if(o instanceof Starter) {
						s = (Starter) o;
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
					Probe newProbe = ProbeFactory.makeProbe(probeType, host, argsListValue);
					if(s != null) {
						newProbe.addStarter(s);
					}
					for(Tag tg: tags)
						newProbe.addTag(tg.getTag());
					host.addProbe(newProbe);
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
	static void defineSnmp(Digester digester) {
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
	
	static void defineArg(Digester digester) {
		Rule storeArg = new Rule() {
			@SuppressWarnings("unchecked")
			public void begin(String namespace, String name, Attributes attributes) throws Exception {
				String type = attributes.getValue("type");
				String value = attributes.getValue("value");
				Object arg = ProbeFactory.makeArg(type, value);
				Object o = null;
				for(int i = 0; i< digester.getCount() && !((o = digester.peek(i)) instanceof List) ; i++);
				List<Object> argsListValue = (List<Object>) o;
				argsListValue.add(arg);
			}
		};

		digester.addRule("host/probe/arg", storeArg);
		digester.addRule("host/rrd/arg", storeArg);
	}
	
	/**
	 * What to do when a macro definition is found
	 * @param d
	 */
	static void defineMacro(Digester d) {
		d.addRule("host/macro", new Rule() {
			public void begin (String namespace, String name, Attributes attributes) {
				String macroName = attributes.getValue("name");
				if(macroName != null) {
					Macro m = HostsList.getRootGroup().getMacroList().get(macroName);
					if (m != null) {
						digester.push(m);
					}
					else
						logger.error("Macro " + macroName + " not found");
				}
			}	
		}
		);
	}
	
	static void defineTag(Digester d) {
		Rule tagRule =  new Rule() {
			public void body(java.lang.String namespace, java.lang.String name, java.lang.String text) {
				HostConfigParser.Tag t = new HostConfigParser.Tag();
				t.setTag(text);
				digester.push(t);
			}
			@Override
			public String toString() {
				return "Tag";
			}
		};
		d.addRule("host/tag", tagRule);
		d.addRule("host/rrd/tag", tagRule);
		d.addRule("host/probe/tag", tagRule);
	}

	public static void addDigester(Digester digester) {
//		digester.register("-//jrds//DTD View//EN", digester.getClass().getResource("/view.dtd").toString());
		digester.setValidating(false);
		defineSnmp(digester);
		defineMacro(digester);
		defineArg(digester);
		defineProbe(digester);
		defineHost(digester);
		defineTag(digester);
	}
	
}
