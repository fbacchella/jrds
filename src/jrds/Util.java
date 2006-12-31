package jrds;

import java.security.MessageDigest;

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
}

