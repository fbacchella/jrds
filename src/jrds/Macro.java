package jrds;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;
import org.apache.log4j.Logger;
import org.xml.sax.Attributes;

public class Macro {
    static private final Logger logger = Logger.getLogger(Macro.class);
    Set<Object[]> probeList = new HashSet<Object[]>();
	private final Set<String> tags = new HashSet<String>();

	private static final class Tag {
		String tag;

		public String getTag() {
			return tag;
		}

		public void setTag(String tag) {
			this.tag = tag;
		}
	};

	public void populate(RdsHost host) {
		for(Object[] l: probeList) {
			String className = (String) l[0];
			List constArgs = (List) l[1];
			Probe newRdsRrd = ProbeFactory.makeProbe(className, host, constArgs);
			if(newRdsRrd != null) {
				HostsList.getRootGroup().addProbe(newRdsRrd);
				host.addProbe(newRdsRrd);
			}
		}
		for(String tag: tags) {
			host.addTag(tag);
		}
	}

	public void put(String className, List constArgs) {
		Object[] l = new Object[] {className, constArgs};
		probeList.add(l);
	}

	public void addTag(String tag) {
		tags.add(tag);
	}

	static void defineArg(Digester digester) {
		Rule storeArg = new Rule() {
			@Override
			public String toString() {
				return "MacroProbeArgAdd";
			}

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

		digester.addRule("macrodef/probe/arg", storeArg);
		digester.addRule("macrodef/rrd/arg", storeArg);
	}
	
	private static void defineProbe(Digester digester) {
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
	
	static void defineTag(Digester d) {
		Rule tagRule =  new Rule() {
			public void body(java.lang.String namespace, java.lang.String name, java.lang.String text) {
				Macro.Tag t = new Macro.Tag();
				t.setTag(text);
				digester.push(t);
			}
			@Override
			public String toString() {
				return "MacroTag";
			}
		};
		d.addRule("macrodef/tag", tagRule);
	}

	static void defineMacro(Digester d) {
		d.addRule("macrodef", new Rule() {
			public void begin (String namespace, String name, Attributes attributes) {
				String macroName = attributes.getValue("name");
				Macro m = new Macro();
				HostsList.getRootGroup().getMacroList().put(macroName, m);
				digester.push(m);
			}	
			public void end(String namespace, String name) throws Exception {
				Set<Tag> tags = new HashSet<Tag>();
				Set<Object[]> probes = new HashSet<Object[]>();
				Object o = null;
				while(digester.getCount() != 0 && ! ((o = digester.pop()) instanceof Macro)) {
					if(o instanceof Macro.Tag) {
						tags.add((Macro.Tag) o);
					}
					else if(o instanceof Object[]) {
						probes.add((Object[]) o);
					}
				}
				if( o instanceof Macro) {
					Macro m = (Macro) o;
					for(Macro.Tag tg: tags)
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
	}

	public static void addToDigester(Digester digester) {
		//digester.register("-//jrds//DTD View//EN", digester.getClass().getResource("/view.dtd").toString());
		defineArg(digester);
		defineMacro(digester);
		defineProbe(digester);
		defineTag(digester);
	}

	@Override
	public String toString() {
		StringBuffer ret =new StringBuffer();
		ret.append("[");
		for(Object[] probes: probeList) {
			ret.append(probes[0]);
			ret.append(probes[1]);
			ret.append(",");
		}
		ret.setCharAt(ret.length()-1, ']');
		return "Macro"+ ret  ;
	}
}
