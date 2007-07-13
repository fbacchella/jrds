package jrds.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import jrds.webapp.ParamsBean;

import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class UrlParserTester  extends JrdsTester {
	static private final Map<String, String[]> parameters = new HashMap<String, String[]>();
	static private final HttpServletRequest req = new HttpServletRequest() {
		public String getAuthType() { return null; }
		public String getContextPath() { return null; }
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

	@Test public void checkId() {
		parameters.clear();
		parameters.put("id", new String[] { "1" });
		ParamsBean pb = new ParamsBean(req);
		Assert.assertEquals(1, pb.getId());
	}
	@Test public void checkSortedTrue() {
		parameters.clear();
		parameters.put("sort", new String[] { "1" });
		ParamsBean pb = new ParamsBean(req);
		Assert.assertTrue(pb.isSorted());
	}
	@Test public void checkSortedFalseDefault() {
		parameters.clear();
		ParamsBean pb = new ParamsBean(req);
		Assert.assertTrue(! pb.isSorted());
	}
	@Test public void checkSortedFalse() {
		parameters.clear();
		parameters.put("sort", new String[] { "0" });
		ParamsBean pb = new ParamsBean(req);
		Assert.assertTrue(! pb.isSorted());
	}
	@BeforeClass static public void configure() {
		JrdsTester.configure();
	}

	@After public void tearDown() {
	}

}
