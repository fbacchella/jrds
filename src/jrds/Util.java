package jrds;

import java.security.MessageDigest;
import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	
	public static String evaluateVariables(String in, Map<String, Object> variables) {
		Matcher m = varregexp.matcher(in);
		if(m.find()) {
			StringBuilder out = new StringBuilder();
			if(m.groupCount() == 3) {
				String before = m.group(1);
				String var = m.group(2);
				String after = m.group(3);
				out.append(before);
				if(variables.containsKey(var)) {
					out.append(variables.get(var));
				}
				else
					out.append("${" + var + "}");
				if(after.length() > 0)
					out.append(evaluateVariables(after, variables));
				return out.toString();

			}
		}
		return in;
	}

}

