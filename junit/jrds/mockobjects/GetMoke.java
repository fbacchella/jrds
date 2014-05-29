package jrds.mockobjects;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.security.Principal;
import java.util.Collections;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

import jrds.GraphDesc;
import jrds.Probe;
import jrds.ProbeDesc;
import jrds.HostInfo;
import jrds.starter.HostStarter;
import jrds.store.RrdDbStoreFactory;

import org.rrd4j.DsType;

public class GetMoke {
    static public HostInfo getHost() {
        return new HostInfo("MokeHost");
    }

    static public ProbeDesc getPd() {
        ProbeDesc pd = new ProbeDesc();
        pd.setName("MokeProbeDesc");
        pd.setProbeName("mokeprobedesc");
        pd.add("MokeDs", DsType.COUNTER);
        return pd;
    }

    static public Probe<?,?> getProbe() throws InvocationTargetException {
        Probe<?,?> p = new MokeProbe<String, Number>();
        p.setPd(getPd());
        p.setHost(new HostStarter(getHost()));
        Map<String, String> empty = Collections.emptyMap();
        p.setMainStore(new RrdDbStoreFactory(), empty);
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
                @Override
                public boolean isReady() {
                    // TODO Auto-generated method stub
                    return false;
                }
                @Override
                public void setWriteListener(WriteListener arg0) {
                    // TODO Auto-generated method stub
                    
                }
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
            public void setContentLengthLong(long arg0) { }
            public String getHeader(String arg0) { return null; }
            public Collection<String> getHeaderNames() { return null; }
            public Collection<String> getHeaders(String arg0) { return null; }
            public int getStatus() { return 0; }
        };
    }

    static public HttpServletRequest getRequest(final Map<String, String[]> p,  String... ctxPathArgs) {
        StringBuffer buffer = new StringBuffer("/");
        if( ctxPathArgs != null && ctxPathArgs.length > 0) {
            for(String element: ctxPathArgs)
                buffer.append('/').append(element); 
        }
        buffer.delete(0, 1);
        final String pathInfo = buffer.toString();

        return new HttpServletRequest() {
            private final Map<String, String[]> parameters = p;
            private final String restPath = pathInfo;

            public String getAuthType() { return null; }
            public String getContextPath() {
                return "/";
            }
            public Cookie[] getCookies() { return null; }
            public long getDateHeader(String arg0)  { return 0; }
            public String getHeader(String arg0) { return null; }
            @SuppressWarnings({ "rawtypes", "unchecked" })
            public Enumeration getHeaderNames() { return null; }
            @SuppressWarnings({ "rawtypes", "unchecked" })
            public Enumeration getHeaders(String arg0) { return null; }
            public int getIntHeader(String arg0) { return 0; }
            public String getMethod() { return null; }
            public String getPathInfo() { return restPath; }
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
            @SuppressWarnings({ "rawtypes", "unchecked" })
            public Enumeration getAttributeNames() { return null; }
            public String getCharacterEncoding() { return null; }
            public int getContentLength() { return 0; }
            public String getContentType() { return null; }
            public ServletInputStream getInputStream() { return null; }
            public String getLocalAddr() { return null; }
            public String getLocalName() { return null; }
            public int getLocalPort() { return 0; }
            public Locale getLocale()  { return null; }
            @SuppressWarnings({ "rawtypes", "unchecked" })
            public Enumeration getLocales() { return null; }
            public String getParameter(String arg0) { 
                String[] array = parameters.get(arg0);
                if(array != null)
                    return array[0];
                return null;
            }
            @SuppressWarnings({ "rawtypes", "unchecked" })
            public Map getParameterMap() { return parameters; }
            @SuppressWarnings({ "rawtypes", "unchecked" })
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
            public AsyncContext getAsyncContext() {return null; }
            public long getContentLengthLong() { return 0; }
            public DispatcherType getDispatcherType() { return null; }
            public ServletContext getServletContext() { return null; }
            public boolean isAsyncStarted() { return false; }
            public boolean isAsyncSupported() { return false; }
            public AsyncContext startAsync() throws IllegalStateException { return null; }
            public AsyncContext startAsync(ServletRequest arg0,
                    ServletResponse arg1) throws IllegalStateException { return null; }
            public boolean authenticate(HttpServletResponse arg0)
                    throws IOException, ServletException { return false; }
            public String changeSessionId() { return null; }
            public Part getPart(String arg0) throws IOException,
                    ServletException { return null; }
            public Collection<Part> getParts() throws IOException,
                    ServletException { return null; }
            public void login(String arg0, String arg1) throws ServletException { }
            public void logout() throws ServletException { }
            public <T extends HttpUpgradeHandler> T upgrade(Class<T> arg0)
                    throws IOException, ServletException { return null; }
        };
    }
}
