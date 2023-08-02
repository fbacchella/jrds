package jrds.webapp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet implementation class JSonPack
 */
public class JSonPack extends HttpServlet {

    static private final Logger logger = LoggerFactory.getLogger(JSonPack.class);

    static public final List<String> JSONKEYS = Collections.unmodifiableList(Arrays.asList("id", "autoperiod", "filter", "host", "path", "begin", "end", "max", "min", "tab", "sort", "tree"));
    static public final Map<String, Integer> JSONDICT;

    static {
        Map<String, Integer> tempdict = new HashMap<>(JSONKEYS.size());
        for(int i = JSONKEYS.size() - 1; i >= 0; i--) {
            tempdict.put(JSONKEYS.get(i), i);
        }
        JSONDICT = Collections.unmodifiableMap(tempdict);
    }

    static public final String GZIPHEADER = "H4sIAAAAAAA";

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int len = request.getContentLength();
        if (len > 4096) {
            logger.error("post data too big: {}", len);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "post data too big: " + len);
            return;
        }
        byte[] bufferin = new byte[len];
        ServletInputStream postDataStream = request.getInputStream();

        // Build the POST data string
        ByteArrayOutputStream postDataBuffer = new ByteArrayOutputStream(len);
        int read;
        while ((read = postDataStream.read(bufferin)) >= 0) {
            postDataBuffer.write(bufferin, 0, read);
        }
        String postData = postDataBuffer.toString();
        logger.debug("Post data: {}", postData);

        JrdsJSONObject paramsClean;

        try {
            JrdsJSONObject params = new JrdsJSONObject(postData);
            paramsClean = new JrdsJSONObject();
            for(String key: params) {
                if(JSONKEYS.contains(key)) {
                    Object value = params.get(key);
                    if(value instanceof String && "".equals(((String) value).trim())) {
                        value = null;
                    }
                    if(value != null)
                        paramsClean.put(JSONDICT.get(key).toString(), value);
                }
            }
        } catch (JSONException e) {
            logger.error("Invalid JSON object: {}", postData);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid POST data");
            return;
        }

        ByteArrayOutputStream packedDataBuffer = new ByteArrayOutputStream(len);
        try (GZIPOutputStream gzipBuffer = new GZIPOutputStream(Base64.getEncoder().wrap(packedDataBuffer), len)) {
            gzipBuffer.write(paramsClean.toString().getBytes());
        }

        // get or build referer
        String referer = request.getHeader("Referer");
        if (referer != null) {
            if (! referer.startsWith("http://") && ! referer.startsWith("https://")) {
                logger.error("Invalid referer: {}", referer);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid referer");
                return;
            }
            // Make sure we do not have any queryString, we received the state as a
            // JSON
            // object and the only parameter we should return is the packed version
            // of this state as 'p' parameter
            // It will prevent also the issue where, if you create a state URL from
            // another
            // state URL, you would have multiple 'p' parameter.
            int querySep = referer.indexOf('?');
            if (querySep != -1) {
                referer = referer.substring(0, querySep);
            }
            try {
                referer = new URL(referer).toString(); // make sure valid URL
            } catch (MalformedURLException e) {
                logger.error("Malformed referer URL: {}", referer);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid referer");
                return;
            }
        } else {
            referer = "http://" + request.getHeader("Host") + request.getContextPath() + "/";
        }

         String packedurl = referer + "?p=" + packedDataBuffer.toString().substring(GZIPHEADER.length()).replace('=', '!').replace('/', '$').replace('+', '*');

        response.getOutputStream().print(packedurl);
        response.flushBuffer();
    }

}
