package jrds.standalone;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.OutputKeys;

import org.apache.log4j.Logger;
import org.rrd4j.DsType;
import org.snmp4j.smi.OID;

import jrds.Probe;
import jrds.ProbeDesc;
import jrds.probe.snmp.RdsIndexedSnmpRrd;
import jrds.probe.snmp.RdsSnmpSimple;

public class DoSnmpProbe  extends CommandStarterImpl {
	static final private Logger logger = Logger.getLogger(DoSnmpProbe.class);
	static final Pattern oidPattern = Pattern.compile("^(.\\d+)+$");
	static final Pattern namePattern = Pattern.compile("^(.+)\\s+OBJECT-TYPE$");
	static final Pattern syntaxPattern = Pattern.compile(".*SYNTAX\\s+([a-zA-Z]+).*");

	private class OidInfo {
		OID oid;
		String name;
		DsType type;
	}


	public void configure(Properties configuration) {
		logger.debug("Configuration: " + configuration);
	}

	private OidInfo translate(String oidstring) throws IOException {
		OidInfo info = new OidInfo();

		Process p = Runtime.getRuntime().exec(new String[] {"snmptranslate", "-Td", "-On", oidstring});
		InputStreamReader isr = new InputStreamReader(p.getInputStream());
		BufferedReader r = new BufferedReader(isr);
		String line = r.readLine();
		while (line != null) {
			Matcher nameMatcher = namePattern.matcher(line);
			Matcher syntaxMatcher = syntaxPattern.matcher(line);
			if(oidPattern.matcher(line.trim()).matches()) {
				String oidString = line.substring(1);
				info.oid = new OID(oidString);
			}
			else if(nameMatcher.matches()) {
				info.name = nameMatcher.group(1);
			}
			else if(syntaxMatcher.matches()) {
				String syntax = syntaxMatcher.group(1);
				if("counter".matches(syntax.toLowerCase()))
					info.type = DsType.COUNTER;
			}
			line = r.readLine();
		}
		return info;
	}

	@SuppressWarnings("unchecked")
	public void start(String[] args) throws Exception {
		ProbeDesc pd = new ProbeDesc();
		pd.setProbeClass(jrds.probe.snmp.RdsSnmpSimple.class);
		boolean indexed = false;
		for(int i=0; i < args.length ; i++) {
			String cmd = args[i];
			if("--specific".equals(cmd.toLowerCase())) {
				for(String specargs: args[++i].split(",")) {
					String[] specinfo = specargs.split("=");
					pd.addSpecific(specinfo[0], specinfo[1]);
				}
			}
			else if("--index".equals(cmd.toLowerCase())) { 
				OidInfo info = translate(args[++i]);

				pd.setProbeClass(jrds.probe.snmp.RdsIndexedSnmpRrd.class);
				pd.addSpecific(RdsIndexedSnmpRrd.INDEXOIDNAME, info.oid.toString());
				indexed = true;
			}
			else if("--probeclass".equals(cmd.toLowerCase())) {
				Class<?> c = Class.forName(args[++i]);
				pd.setProbeClass((Class<? extends Probe<?, ?>>) c);
			}
			else if("--graphs".equals(cmd.toLowerCase())) {
				pd.setGraphClasses(Arrays.asList(args[++i].split(",")));
			}
			else if("--collect".equals(cmd.toLowerCase())) {
				for(String collectarg: args[++i].split(",")) {
					OidInfo info = translate(collectarg);
					pd.add(info.name, info.type, info.oid);
				}
			}

			else if(cmd.startsWith("--") ) {
				String method = cmd.replace("--", "set");
				Method m = ProbeDesc.class.getMethod(method, String.class);
				m.invoke(pd, args[++i]);
			}
		}
		if( ! indexed)
			pd.addSpecific(RdsSnmpSimple.REQUESTERNAME, "simple");

		Map<String, String> prop = new HashMap<String, String>();
		prop.put(OutputKeys.INDENT, "yes");
		prop.put(OutputKeys.DOCTYPE_PUBLIC, "-//jrds//DTD Probe Description//EN");
		prop.put(OutputKeys.DOCTYPE_SYSTEM, "urn:jrds:probedesc");
		prop.put(OutputKeys.INDENT, "yes");
		jrds.Util.serialize(pd.dumpAsXml(), System.out, null, prop);
		System.out.println();
	}
}
