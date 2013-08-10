package jrds.mockobjects;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

public class MokeServletContext implements ServletContext {
    public Map<String, Object> attributes = new HashMap<String, Object>();
    public Map<String, String> initParameters = new HashMap<String, String>();

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @SuppressWarnings({ "rawtypes" })
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

    @SuppressWarnings("rawtypes")
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

    public URL getResource(String path) throws MalformedURLException {
        return null;
    }

    public InputStream getResourceAsStream(String path) {
        return null;
    }

    @SuppressWarnings("rawtypes")
    public Set getResourcePaths(String arg0) {
        return null;
    }

    public String getServerInfo() {
        return null;
    }

    public Servlet getServlet(String name) throws ServletException {
        return null;
    }

    public String getServletContextName() {
        return null;
    }

    @SuppressWarnings("rawtypes")
    public Enumeration getServletNames() {
        return null;
    }

    @SuppressWarnings("rawtypes")
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

}
