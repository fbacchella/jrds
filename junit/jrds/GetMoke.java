package jrds;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.rrd4j.DsType;

import jrds.GraphDesc;
import jrds.ProbeDesc;
import jrds.Probe;
import jrds.RdsHost;

public class GetMoke {
	static public RdsHost getHost() {
		return new RdsHost("MokeHost");
	}
	
	static public ProbeDesc getPd() {
		ProbeDesc pd = new ProbeDesc();
		pd.setName("MokeProbeDesc");
		pd.add("MokeDs", DsType.COUNTER);
		return pd;
	}
	
	static public Probe getProbe() {
		Probe p = new Probe() {
			@Override
			public Map getNewSampleValues() {
				return new HashMap();
			}
			@Override
			public String getSourceType() {
				return "MokeSourceType";
			}
			/* (non-Javadoc)
			 * @see jrds.Probe#getName()
			 */
			@Override
			public String getName() {
				return "MokeProbe";
			}
			/* (non-Javadoc)
			 * @see jrds.Probe#getLastUpdate()
			 */
			@Override
			public Date getLastUpdate() {
				return new Date();
			}
		};
		p.setHost(getHost());
		p.setPd(getPd());
		return p;
	}
	
	static public GraphDesc getGraphDesc() {
		GraphDesc gd = new GraphDesc();
		gd.setGraphName("MokeGD");
		return gd;
	}
	
	static public HttpServletResponse getResponse(final OutputStream os) {
		return new HttpServletResponse() {
			private String encoding = null;
			private String type = null;
			private final ServletOutputStream internalOs = new ServletOutputStream() {
			    private DataOutputStream stream = new DataOutputStream(os);
				@Override
				public void write(int b) throws IOException { stream.write(b); }
			    public void write(byte[] theData) throws IOException { stream.write(theData); }
			    public void write(byte[] theData, int theOffset, int theLength) throws IOException { stream.write(theData, theOffset, theLength); }
			};
			public void addCookie(Cookie arg0) { }
			public void addDateHeader(String arg0, long arg1) { }
			public void addHeader(String arg0, String arg1) { }
			public void addIntHeader(String arg0, int arg1) { }
			public boolean containsHeader(String arg0) { return true; }
			public String encodeRedirectURL(String arg0) { return null; }
			public String encodeRedirectUrl(String arg0) { return null; }
			public String encodeURL(String arg0) { return null; }
			public String encodeUrl(String arg0) { return null; }
			public void sendError(int arg0) throws IOException { }
			public void sendError(int arg0, String arg1) throws IOException { }
			public void sendRedirect(String arg0) throws IOException {	}
			public void setDateHeader(String arg0, long arg1) {	}
			public void setHeader(String arg0, String arg1) { }
			public void setIntHeader(String arg0, int arg1) { }
			public void setStatus(int arg0) { }
			public void setStatus(int arg0, String arg1) { }
			public void flushBuffer() throws IOException {	}
			public int getBufferSize() { return 0;}
			public String getCharacterEncoding() { return encoding; }
			public String getContentType() { return type; }
			public Locale getLocale() { return null; }
			public ServletOutputStream getOutputStream() throws IOException { return internalOs; }
			public PrintWriter getWriter() throws IOException { return new PrintWriter(internalOs); }
			public boolean isCommitted() { return false; }
			public void reset() { }
			public void resetBuffer() { }
			public void setBufferSize(int arg0) { }
			public void setCharacterEncoding(String arg0) { encoding = arg0; }
			public void setContentLength(int arg0) { }
			public void setContentType(String arg0) { type = arg0; }
			public void setLocale(Locale arg0) { }
		};
	}

	static public HttpServletRequest getRequest(final Map<String, String[]> p) {
		//static private final HttpServletRequest req =
		return new HttpServletRequest() {
			private final Map<String, String[]> parameters = p;

			public String getAuthType() { return null; }
			public String getContextPath() { return ""; }
			public Cookie[] getCookies() { return null; }
			public long getDateHeader(String arg0)  { return 0; }
			public String getHeader(String arg0) { return null; }
			public Enumeration getHeaderNames() { return null; }
			public Enumeration getHeaders(String arg0) { return null; }
			public int getIntHeader(String arg0) { return 0; }
			public String getMethod() { return null; }
			public String getPathInfo() { return null; }
			public String getPathTranslated() { return null; }
			public String getQueryString() { return null; }
			public String getRemoteUser() { return null; }
			public String getRequestURI() { return null; }
			public StringBuffer getRequestURL() { return null; }
			public String getRequestedSessionId() { return null; }
			public String getServletPath() { return null; }
			public HttpSession getSession() { return null; }
			public HttpSession getSession(boolean arg0) { return null; }
			public Principal getUserPrincipal()  { return null; }
			public boolean isRequestedSessionIdFromCookie() { return false; }
			public boolean isRequestedSessionIdFromURL() { return false; }
			public boolean isRequestedSessionIdFromUrl()  { return false; }
			public boolean isRequestedSessionIdValid() { return false; }
			public boolean isUserInRole(String arg0)  { return false; }
			public Object getAttribute(String arg0) { return null; }
			public Enumeration getAttributeNames() { return null; }
			public String getCharacterEncoding() { return null; }
			public int getContentLength() { return 0; }
			public String getContentType() { return null; }
			public ServletInputStream getInputStream() { return null; }
			public String getLocalAddr() { return null; }
			public String getLocalName() { return null; }
			public int getLocalPort() { return 0; }
			public Locale getLocale()  { return null; }
			public Enumeration getLocales() { return null; }
			public String getParameter(String arg0) { 
				String[] array = parameters.get(arg0);
				if(array != null)
					return array[0];
				return null;
			}
			public Map getParameterMap() { return parameters; }
			public Enumeration getParameterNames()  { return null; }
			public String[] getParameterValues(String arg0) { return null; }
			public String getProtocol() { return null; }
			public BufferedReader getReader() throws IOException { return null; }
			public String getRealPath(String arg0)  { return null; }
			public String getRemoteAddr()  { return null; }
			public String getRemoteHost()  { return null; }
			public int getRemotePort() { return 0; }
			public RequestDispatcher getRequestDispatcher(String arg0) { return null; }
			public String getScheme() { return null; }
			public String getServerName() { return null; }
			public int getServerPort() { return 0; }
			public boolean isSecure() { return false; }
			public void removeAttribute(String arg0) { }
			public void setAttribute(String arg0, Object arg1) {  }
			public void setCharacterEncoding(String arg0) throws UnsupportedEncodingException { }
		};
	}
}
