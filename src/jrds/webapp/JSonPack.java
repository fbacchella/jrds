package jrds.webapp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jrds.Base64.OutputStream;

import org.apache.log4j.Logger;
import org.json.JSONException;

/**
 * Servlet implementation class JSonPack
 */
public class JSonPack extends HttpServlet {
	static final private Logger logger = Logger.getLogger(JSonPack.class);
	static final public List<String> JSONKEYS =  Arrays.asList(new String[] {"id", "autoperiod", "filter", "host", "path", "begin", "end", "max", "min", "tab", "sort", "tree"});
	static final public Map<String, Integer> JSONDICT = new HashMap<String, Integer>(JSONKEYS.size());
	static {
		for(int i= JSONKEYS.size() -1; i >= 0; i--) {
			JSONDICT.put(JSONKEYS.get(i), i);
		}
	}
	static final public String GZIPHEADER="H4sIAAAAAAA";

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		int len = request.getContentLength();
		byte[] bufferin = new byte[len];
		ServletInputStream postDataStream = request.getInputStream();

		ByteArrayOutputStream postDataBuffer = new ByteArrayOutputStream(len);
		int read;
		while((read = postDataStream.read(bufferin)) > 0 ) {
			postDataBuffer.write(bufferin, 0, read);
		};

		String postData = postDataBuffer.toString();


		logger.debug( "Post data: " + postData);
		JrdsJSONObject paramsClean;

		try {
			JrdsJSONObject params = new JrdsJSONObject(postData);

			paramsClean = new JrdsJSONObject();
			for(String key: params) {
				if( JSONKEYS.contains(key)) {
					Object value =  params.get(key);
					//logger.trace(key + ": " + value + "@" + value.getClass());
					if(value instanceof String && "".equals(((String) value).trim())) {
						value = null;
					}
					if(value != null)
						paramsClean.put(JSONDICT.get(key).toString(), value);
				}
			}
			logger.trace(paramsClean);
		} catch (JSONException e) {
			logger.error("Invalid JSON object:" + postData);
			return;
		}

		ByteArrayOutputStream packedDataBuffer = new ByteArrayOutputStream(len);
		GZIPOutputStream gzipBuffer = new GZIPOutputStream(new OutputStream(packedDataBuffer), len);
		gzipBuffer.write(paramsClean.toString().getBytes());
		gzipBuffer.close();

		String host = request.getHeader("Host");
		String contextPath = request.getContextPath();
		String packedurl = "http://" + host + contextPath + "/?p=" + new String(packedDataBuffer.toByteArray()).substring(GZIPHEADER.length()).replace('=', '!').replace('/', '$').replace('+', '*');
		

		response.getOutputStream().print(packedurl);
		response.flushBuffer();
	}

}
