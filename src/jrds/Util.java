package jrds;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.MessageDigest;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jrds.probe.IndexedProbe;
import jrds.probe.UrlProbe;

import org.apache.log4j.Logger;

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

	public enum SiPrefix {
		Y(24),
		Z(21),
		E(18),
		P(15),
		T(12),
		G(9),
		M(6),
		k(3),
		h(2),
		da(1),
		FIXED(0) {
			public double evaluate(double value, int base) {
				return value;
			}
		},
		d(-1),
		c(-2),
		m(-3),
		µ(-6),
		n(-9),
		p(-12),
		f(-15),
		a(-18),
		z(-21),
		y(-24);
		
		private int exponent;
		private SiPrefix(int exponent) {
			this.exponent = exponent;
		}
		public double evaluate(double value, boolean isSi) {
			return Math.pow(isSi ? 10 : 1024, isSi ? exponent: exponent/3.0 ) * value;
		}
		public int getExponent() {
			return exponent;
		}
	};

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

	public static String cleanPath(String s){
		String retval = s.replace('\\', '_');
		retval = retval.replace(':', '_');
		retval = retval.replace('/', '_');
		return retval;
	}

	/**
	 * Used to normalize the end date to the last update time
	 * but only if it's close to it 
	 * @param p the probe to check against
	 * @param endDate the desired end date
	 * @return the normalized end date
	 */
	public static Date endDate(Probe p, Date endDate) {
		//Date normalized = endDate;
		//We normalize the last update time, it can't be used directly
		long step = p.getStep();
		Date lastUpdate = p.getLastUpdate();
		
		//We dont want to graph past the last normalized update time
		//but only if we are within a step interval
		if( (endDate.getTime() - lastUpdate.getTime()) <= (step * 1000L))
			return normalize(lastUpdate, step);

		//Else rrd4j will manage the normalization itself
		return endDate;
	}
	
	/**
	 * Normalize to a probe step, as org.rrd4j.core.Util.normalize
	 * But use a Date argument and return a Date
	 * @param date	A Date to normalize
	 * @param step	Step in seconds
	 * @return "Rounded" Date
	 */
	public static Date normalize(Date date, long step) {
		long timestamp = org.rrd4j.core.Util.getTimestamp(date);
		return org.rrd4j.core.Util.getDate(org.rrd4j.core.Util.normalize(timestamp, step));
	}

	public static String evaluateVariables(String in, Map<String, Object> variables, StarterNode node) {
		ChainedProperties props = (ChainedProperties)node.getStarters().find(ChainedProperties.KEY);
		return evaluateVariables(in, variables, props);
	}

	public static String evaluateVariables(String in, Map<String, Object> variables, Map<String,String> props) {
		Matcher m = varregexp.matcher(in);
		if(m.find()) {
			StringBuilder out = new StringBuilder();
			if(m.groupCount() == 3) {
				String before = m.group(1);
				String var = m.group(2);
				String after = m.group(3);
				out.append(before);
				String toAppend = null;
				if(var.startsWith("system.")) {
					 toAppend = System.getProperty(var.replace("system.", ""));
				}
				else if(variables.containsKey(var)) {
					toAppend = variables.get(var).toString();
				}
				else if(props != null) {
					String propsValue = props.get(var);
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
			logger.trace("Argument for tempate: " + o.getClass());
			if( o instanceof IndexedProbe) {
				String index = ((IndexedProbe) o).getIndexName();
				env.put("index", index);
				env.put("index.signature", stringSignature(index));
				env.put("index.cleanpath", cleanPath(index));
			}
			if(o instanceof UrlProbe) {
				env.put("url", ((UrlProbe) o).getUrlAsString());
				env.put("port", ((UrlProbe) o).getPort());
				env.put("url.signature", jrds.Util.stringSignature(((UrlProbe) o).getUrlAsString()));
			}
			if( o instanceof Probe) {
				Probe p = ((Probe) o);
				RdsHost host = p.getHost();
				if(host != null)
					env.put("host", host.getName());
				String probename=p.getName();
				//It might be called just for evaluate probename
				//So no problem if it's null
				if(probename != null)
					env.put("probename", probename);
				String label = p.getLabel();
				if(label != null) {
					env.put("label", label);

				}
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

		logger.trace("Properties to use for parsing: " + env + " with template "+ template);
		return jrds.Util.evaluateVariables(template, env, node);
	}
	
	public static Number parseStringNumber(String toParse, Class<? extends Number> numberClass, Number defaultVal) {
		if(! (Number.class.isAssignableFrom(numberClass))) {
			return defaultVal;
		}
		
		try {
			Constructor<? extends Number> c = numberClass.getConstructor(String.class);
			Number n = c.newInstance(toParse);
			return n;
		} catch (SecurityException e) {
		} catch (NoSuchMethodException e) {
		} catch (IllegalArgumentException e) {
		} catch (InstantiationException e) {
		} catch (IllegalAccessException e) {
		} catch (InvocationTargetException e) {
		}
		return defaultVal;
	}
}

