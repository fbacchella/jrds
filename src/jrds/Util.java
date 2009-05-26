package jrds;

import java.net.URL;
import java.security.MessageDigest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jrds.probe.IndexedProbe;
import jrds.probe.UrlProbe;

import org.apache.log4j.Logger;

import com.sun.tools.javac.util.List;


/**
 *
 * @version $Revision$
 */
public class Util {
	static final private Logger logger = Logger.getLogger(Util.class);

	static private MessageDigest md5digest;
	static {
		try {
			md5digest = java.security.MessageDigest.getInstance("MD5");
		}
		catch (java.security.NoSuchAlgorithmException ex) {
			logger.fatal("You should not see this message, MD5 not available");
		}
	}

	private static final String BASE64_CHARS =
		"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+_=";
	private static final char[] BASE64_CHARSET = BASE64_CHARS.toCharArray();

	private static final Pattern varregexp = Pattern.compile("(.*?)\\$\\{([\\w\\.-]+)\\}(.*)");

	private Util() {
	}

	public static String stringSignature(String s)
	{
		byte[] digestval;
		synchronized(md5digest) {
			md5digest.reset();
			digestval = md5digest.digest(s.getBytes());
		}
		return toBase64(digestval);
	}

	/**

	 * <p>Converts a designated byte array to a Base-64 representation, with the	 
	 * exceptions that (a) leading 0-byte(s) are ignored, and (b) the character	 
	 * '.' (dot) shall be used instead of "+' (plus).</p>	 

	 * @param buffer an arbitrary sequence of bytes to represent in Base-64.	 
	 * @return unpadded (without the '=' character(s)) Base-64 representation of	 
	 * the input.

	 */
	public static String toBase64(byte[] buffer) {
		int len = buffer.length;
		int pos = 0;
		StringBuffer sb = new StringBuffer((int) (len * 1.4) + 3 );

		while(pos < len) {
			byte b0 = 0, b1 = 0, b2 = 0;
			b0 = buffer[pos++];
			if(pos  < len )
				b1 = buffer[pos++];
			if(pos  < len)
				b2 = buffer[pos++];
			int c0 = (b0 & 0xFC) >>> 2;
			sb.append(BASE64_CHARSET[c0]);
			int c1 = ((b0 & 0x03) << 4) | ((b1 & 0xF0) >>> 4);
			sb.append(BASE64_CHARSET[c1]);
			int c2 = ((b1 & 0x0F) << 2) | ((b2 & 0xC0) >>> 6);
			sb.append(BASE64_CHARSET[c2]);
			int c3 = b2 & 0x3F;
			sb.append(BASE64_CHARSET[c3]);
		}
		int mod = len %3;
		if(mod == 2)
			sb.deleteCharAt(sb.length() -1 );
		else if(mod == 1) {
			sb.deleteCharAt(sb.length() -1 );
			sb.deleteCharAt(sb.length() -1 );
		}
		return sb.toString();	
	}

	/**
	 * Used to normalize the end date to the last update time
	 * but only if it's close to it 
	 * @param p the probe to check against
	 * @param endDate the desired end date
	 * @return the normalized end date
	 */
	public static Date endDate(Probe p, Date endDate) {
		Date normalized = endDate;
		//We normalize the last update time, it can't be used directly
		long resolution = HostsList.getRootGroup().getResolution();
		Date lastUpdateNormalized = new Date(1000L * org.rrd4j.core.Util.normalize(p.getLastUpdate().getTime() / 1000L, resolution));
		//We dont want to graph past the last normalized update time
		//but only if we are within a resolution interval
		if(endDate.after(lastUpdateNormalized) && endDate.getTime() - lastUpdateNormalized.getTime() < resolution * 1000L)
			normalized = lastUpdateNormalized;
		return normalized;
	}

	public static String evaluateVariables(String in, Map<String, Object> variables, StarterNode node) {
		PropertyStarter props = (PropertyStarter)node.getStarters().find(PropertyStarter.KEY);
		return evaluateVariables(in, variables, props);
	}

	public static String evaluateVariables(String in, Map<String, Object> variables, PropertyStarter props) {
		Matcher m = varregexp.matcher(in);
		if(m.find()) {
			StringBuilder out = new StringBuilder();
			if(m.groupCount() == 3) {
				String before = m.group(1);
				String var = m.group(2);
				String after = m.group(3);
				out.append(before);
				String toAppend = null;
				if(variables.containsKey(var)) {
					toAppend = variables.get(var).toString();
				}
				else if(props != null) {
					String propsValue = props.getProp(var);
					if(propsValue != null)
						out.append(propsValue);
				}
				if(toAppend == null) {
					toAppend = "${" + var + "}";
				}
				out.append(toAppend);
				if(after.length() > 0)
					out.append(evaluateVariables(after, variables, props));
				return out.toString();

			}
		}
		return in;
	}

	@SuppressWarnings("unchecked")
	public static String parseTemplate(String template, Object... arguments) {
		Map<String, Object> env = new HashMap<String, Object>();
		StarterNode node = null;
		for(Object o: arguments) {
			if( o instanceof IndexedProbe) {
				String index = ((IndexedProbe) o).getIndexName();
				env.put("index", index);
				env.put("index.signature", jrds.Util.stringSignature(index));
			}
			if( o instanceof UrlProbe) {
				URL url = ((UrlProbe) o).getUrl();
				env.put("url", url);
				env.put("port", url.getPort());
				env.put("url.signature", jrds.Util.stringSignature(url.toString()));
			}
			if( o instanceof Probe) {
				Probe p = ((Probe) o);
				RdsHost host = p.getHost();
				if(host != null)
					env.put("host", host.getName());
				env.put("probename", p.getName());
			}
			if( o instanceof RdsHost) {
				env.put("host", ((RdsHost) o).getName());
			}
			if(o instanceof GraphDesc) {
				GraphDesc gd = (GraphDesc) o;
				env.put("graphdesc.title", gd.getGraphTitle());
				env.put("graphdesc.name", gd.getGraphName());
			}
			if(o instanceof ProbeDesc) {
				ProbeDesc pd = (ProbeDesc) o;
				env.put("probedesc.name", pd.getName());

			}
			if(o instanceof StarterNode) {
				node = (StarterNode) o;
			}
			if(o instanceof Map) {
				Map<? extends String, ?> tempMap = (Map<? extends String, ?>)o;
				env.putAll(tempMap);
			}
			if(o instanceof List) {
				int rank=1;
				for(Object listElem: (List<? extends Object>) o) {
					env.put(String.valueOf(rank++), listElem.toString());
				}
			}
		}

		return jrds.Util.evaluateVariables(template, env, node);
	}
}

