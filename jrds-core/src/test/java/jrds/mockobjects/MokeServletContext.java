package jrds.mockobjects;

import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.FilterRegistration.Dynamic;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.SessionTrackingMode;
import jakarta.servlet.descriptor.JspConfigDescriptor;

public class MokeServletContext implements ServletContext {
    public Map<String, Object> attributes = new HashMap<>();
    public Map<String, String> initParameters = new HashMap<>();

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Enumeration getAttributeNames() {
        return Collections.enumeration(attributes.keySet());
    }

    public ServletContext getContext(String uripath) {
        return null;
    }

    public String getContextPath() {
        return null;
    }

    public String getInitParameter(String name) {
        return initParameters.get(name);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Enumeration getInitParameterNames() {
        return Collections.enumeration(initParameters.keySet());
    }

    public int getMajorVersion() {
        return 0;
    }

    public String getMimeType(String file) {
        return null;
    }

    public int getMinorVersion() {
        return 0;
    }

    public RequestDispatcher getNamedDispatcher(String name) {
        return null;
    }

    public String getRealPath(String path) {
        return null;
    }

    public RequestDispatcher getRequestDispatcher(String path) {
        return null;
    }

    public URL getResource(String path) {
        return null;
    }

    public InputStream getResourceAsStream(String path) {
        return null;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Set getResourcePaths(String arg0) {
        return null;
    }

    public String getServerInfo() {
        return null;
    }

    public Servlet getServlet(String name) {
        return null;
    }

    public String getServletContextName() {
        return null;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Enumeration getServletNames() {
        return null;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Enumeration getServlets() {
        return null;
    }

    public void log(String msg) {

    }

    public void log(Exception exception, String msg) {

    }

    public void log(String message, Throwable throwable) {

    }

    public void removeAttribute(String name) {

    }

    public void setAttribute(String name, Object object) {
        attributes.put(name, object);
    }

    public Dynamic addFilter(String arg0, String arg1) {
        return null;
    }

    public Dynamic addFilter(String arg0, Filter arg1) {
        return null;
    }

    public Dynamic addFilter(String arg0, Class<? extends Filter> arg1) {
        return null;
    }

    public void addListener(String arg0) {
    }

    public <T extends EventListener> void addListener(T arg0) {
    }

    public void addListener(Class<? extends EventListener> arg0) {
    }

    public ServletRegistration.Dynamic addServlet(String arg0, String arg1) {
        return null;
    }

    public ServletRegistration.Dynamic addServlet(String arg0, Servlet arg1) {
        return null;
    }

    public ServletRegistration.Dynamic addServlet(String arg0, Class<? extends Servlet> arg1) {
        return null;
    }

    @Override
    public ServletRegistration.Dynamic addJspFile(String s, String s1) {
        return null;
    }

    public <T extends Filter> T createFilter(Class<T> arg0) {
        return null;
    }

    public <T extends EventListener> T createListener(Class<T> arg0) {
        return null;
    }

    public <T extends Servlet> T createServlet(Class<T> arg0) {
        return null;
    }

    public void declareRoles(String... arg0) {
    }

    public ClassLoader getClassLoader() {
        return null;
    }

    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return null;
    }

    public int getEffectiveMajorVersion() {
        return 0;
    }

    public int getEffectiveMinorVersion() {
        return 0;
    }

    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return null;
    }

    public FilterRegistration getFilterRegistration(String arg0) {
        return null;
    }

    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        return null;
    }

    public JspConfigDescriptor getJspConfigDescriptor() {
        return null;
    }

    public ServletRegistration getServletRegistration(String arg0) {
        return null;
    }

    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        return null;
    }

    public SessionCookieConfig getSessionCookieConfig() {
        return null;
    }

    public String getVirtualServerName() {
        return null;
    }

    @Override
    public int getSessionTimeout() {
        return 0;
    }

    @Override
    public void setSessionTimeout(int i) {

    }

    @Override
    public String getRequestCharacterEncoding() {
        return null;
    }

    @Override
    public void setRequestCharacterEncoding(String s) {

    }

    @Override
    public String getResponseCharacterEncoding() {
        return null;
    }

    @Override
    public void setResponseCharacterEncoding(String s) {

    }

    public boolean setInitParameter(String arg0, String arg1) {
        return false;
    }

    public void setSessionTrackingModes(Set<SessionTrackingMode> arg0) {

    }

}
