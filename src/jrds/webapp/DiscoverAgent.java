package jrds.webapp;

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;

import jrds.factories.xml.JrdsNode;

import org.w3c.dom.Document;

public abstract class DiscoverAgent {
    public abstract void discover(String hostname, Document hostDom, Collection<JrdsNode> probdescs, HttpServletRequest request);

}
