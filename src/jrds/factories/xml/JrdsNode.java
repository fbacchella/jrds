package jrds.factories.xml;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import jrds.factories.NodeListIterator;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.UserDataHandler;

public class JrdsNode implements Node {

	Node parent;

	public JrdsNode(Node n){
		if(n == null)
			throw new NullPointerException("The parent node is null");
		this.parent = n;
	}

	/**
	 * @param xpath
	 * @return
	 * @throws XPathExpressionException
	 */
	public  boolean checkPath(XPathExpression xpath) {
		Node n;
		try {
			n = (Node)xpath.evaluate(parent, XPathConstants.NODE);
		} catch (XPathExpressionException e) {
			throw new RuntimeException("xpath evaluate failed", e);
		}
		if(n == null)
			return(false);
		String value = n.getNodeValue();
		if (value == null)
			return true;
		value = value.trim();
		if("".equals(value) || "true".equals(value.toLowerCase()) || "yes".equals(value.toLowerCase()))
			return true;
		return false;
	}

	/**
	 * Apply a method on a object with the value found by the XPath
	 * @param o
	 * @param xpath
	 * @param method
	 * @throws XPathExpressionException
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public void setMethod(Object o, XPathExpression xpath, String method) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		setMethod(o, xpath, method, true);
	}

	/**
	 * Apply a method on a object with the value found by the XPath
	 * @param o the object to apply the method on
	 * @param xpath where to find the value
	 * @param method the name of the method
	 * @param uniq can the method be applied many time ?
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 * @throws NoSuchMethodException 
	 * @throws SecurityException 
	 */
	public void setMethod(Object o, XPathExpression xpath, String method, boolean uniq) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException {
		for(Node n: new NodeListIterator(parent, xpath)) {
			String name = n.getTextContent().trim();
			Method m;
			if(name != null && ! "".equals(name)) {
				try {
					m = o.getClass().getMethod(method, String.class);
				} catch (NoSuchMethodException e) {
					m = o.getClass().getMethod(method, Object.class);
				}
				m.invoke(o, name);
				if(uniq)
					break;
			}
		}
	}

	public static abstract class FilterNode {
		public abstract Object filter(Node input);
	};

	public List<?> doTreeList(XPathExpression xpath, FilterNode f) {
		NodeList list;
		try {
			list = (NodeList) xpath.evaluate(parent, XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			throw new RuntimeException("xpath evaluation failed", e);
		}
		if(list == null || list.getLength() == 0 ) {
			return Collections.emptyList();
		}
		List<Object> l = new ArrayList<Object>(list.getLength());
		for(int i=0; i < list.getLength(); i++) {
			Object o = f.filter(list.item(i));
			l.add(o);
		}
		return l;
	}

	public NodeListIterator iterate(XPathExpression xpath) {
		return new NodeListIterator(parent, xpath);
	}

	public JrdsNode getChild(XPathExpression xpath) {
		try {
			Node n = (Node) xpath.evaluate(parent, XPathConstants.NODE);
			if(n == null)
				return null;
			return new JrdsNode(n);
		} catch (XPathExpressionException e) {
			throw new RuntimeException("xpath evaluate failed", e);
		}
	}

	public Map<String, String> attrMap() {
		NamedNodeMap attrs = parent.getAttributes();
		if(attrs == null)
			return Collections.emptyMap();
		Map<String, String> retValues = new HashMap<String, String>(attrs.getLength());
		for(int i = 0; i < attrs.getLength(); i++) {
			Node attrNode = attrs.item(i);
			retValues.put(attrNode.getNodeName(), attrNode.getNodeValue());
		}
		return retValues;
	}

	public String evaluate(XPathExpression xpath) {
		try {
			return xpath.evaluate(parent);
		} catch (XPathExpressionException e) {
			throw new RuntimeException("xpath evaluate failed", e);
		}
	}

	/**
	 * @param newChild
	 * @return
	 * @throws DOMException
	 * @see org.w3c.dom.Node#appendChild(org.w3c.dom.Node)
	 */
	public Node appendChild(Node newChild) throws DOMException {
		return parent.appendChild(newChild);
	}

	/**
	 * @param deep
	 * @return
	 * @see org.w3c.dom.Node#cloneNode(boolean)
	 */
	public Node cloneNode(boolean deep) {
		return parent.cloneNode(deep);
	}

	/**
	 * @param other
	 * @return
	 * @throws DOMException
	 * @see org.w3c.dom.Node#compareDocumentPosition(org.w3c.dom.Node)
	 */
	public short compareDocumentPosition(Node other) throws DOMException {
		return parent.compareDocumentPosition(other);
	}

	/**
	 * @return
	 * @see org.w3c.dom.Node#getAttributes()
	 */
	public NamedNodeMap getAttributes() {
		return parent.getAttributes();
	}

	/**
	 * @return
	 * @see org.w3c.dom.Node#getBaseURI()
	 */
	public String getBaseURI() {
		return parent.getBaseURI();
	}

	/**
	 * @return
	 * @see org.w3c.dom.Node#getChildNodes()
	 */
	public NodeList getChildNodes() {
		return parent.getChildNodes();
	}

	/**
	 * @param feature
	 * @param version
	 * @return
	 * @see org.w3c.dom.Node#getFeature(java.lang.String, java.lang.String)
	 */
	public Object getFeature(String feature, String version) {
		return parent.getFeature(feature, version);
	}

	/**
	 * @return
	 * @see org.w3c.dom.Node#getFirstChild()
	 */
	public Node getFirstChild() {
		return parent.getFirstChild();
	}

	/**
	 * @return
	 * @see org.w3c.dom.Node#getLastChild()
	 */
	public Node getLastChild() {
		return parent.getLastChild();
	}

	/**
	 * @return
	 * @see org.w3c.dom.Node#getLocalName()
	 */
	public String getLocalName() {
		return parent.getLocalName();
	}

	/**
	 * @return
	 * @see org.w3c.dom.Node#getNamespaceURI()
	 */
	public String getNamespaceURI() {
		return parent.getNamespaceURI();
	}

	/**
	 * @return
	 * @see org.w3c.dom.Node#getNextSibling()
	 */
	public Node getNextSibling() {
		return parent.getNextSibling();
	}

	/**
	 * @return
	 * @see org.w3c.dom.Node#getNodeName()
	 */
	public String getNodeName() {
		return parent.getNodeName();
	}

	/**
	 * @return
	 * @see org.w3c.dom.Node#getNodeType()
	 */
	public short getNodeType() {
		return parent.getNodeType();
	}

	/**
	 * @return
	 * @throws DOMException
	 * @see org.w3c.dom.Node#getNodeValue()
	 */
	public String getNodeValue() throws DOMException {
		return parent.getNodeValue();
	}

	/**
	 * If it's a document, it return itself
	 * @return
	 * @see org.w3c.dom.Node#getOwnerDocument()
	 */
	public Document getOwnerDocument() {
		if(getNodeType() == Node.DOCUMENT_NODE)
			return (Document) this.parent;
		return parent.getOwnerDocument();
	}

	/**
	 * @return
	 * @see org.w3c.dom.Node#getParentNode()
	 */
	public Node getParentNode() {
		return parent.getParentNode();
	}

	/**
	 * @return
	 * @see org.w3c.dom.Node#getPrefix()
	 */
	public String getPrefix() {
		return parent.getPrefix();
	}

	/**
	 * @return
	 * @see org.w3c.dom.Node#getPreviousSibling()
	 */
	public Node getPreviousSibling() {
		return parent.getPreviousSibling();
	}

	/**
	 * @return
	 * @throws DOMException
	 * @see org.w3c.dom.Node#getTextContent()
	 */
	public String getTextContent() throws DOMException {
		return parent.getTextContent();
	}

	/**
	 * @param key
	 * @return
	 * @see org.w3c.dom.Node#getUserData(java.lang.String)
	 */
	public Object getUserData(String key) {
		return parent.getUserData(key);
	}

	/**
	 * @return
	 * @see org.w3c.dom.Node#hasAttributes()
	 */
	public boolean hasAttributes() {
		return parent.hasAttributes();
	}

	/**
	 * @return
	 * @see org.w3c.dom.Node#hasChildNodes()
	 */
	public boolean hasChildNodes() {
		return parent.hasChildNodes();
	}

	/**
	 * @param newChild
	 * @param refChild
	 * @return
	 * @throws DOMException
	 * @see org.w3c.dom.Node#insertBefore(org.w3c.dom.Node, org.w3c.dom.Node)
	 */
	public Node insertBefore(Node newChild, Node refChild) throws DOMException {
		return parent.insertBefore(newChild, refChild);
	}

	/**
	 * @param namespaceURI
	 * @return
	 * @see org.w3c.dom.Node#isDefaultNamespace(java.lang.String)
	 */
	public boolean isDefaultNamespace(String namespaceURI) {
		return parent.isDefaultNamespace(namespaceURI);
	}

	/**
	 * @param arg
	 * @return
	 * @see org.w3c.dom.Node#isEqualNode(org.w3c.dom.Node)
	 */
	public boolean isEqualNode(Node arg) {
		return parent.isEqualNode(arg);
	}

	/**
	 * @param other
	 * @return
	 * @see org.w3c.dom.Node#isSameNode(org.w3c.dom.Node)
	 */
	public boolean isSameNode(Node other) {
		return parent.isSameNode(other);
	}

	/**
	 * @param feature
	 * @param version
	 * @return
	 * @see org.w3c.dom.Node#isSupported(java.lang.String, java.lang.String)
	 */
	public boolean isSupported(String feature, String version) {
		return parent.isSupported(feature, version);
	}

	/**
	 * @param prefix
	 * @return
	 * @see org.w3c.dom.Node#lookupNamespaceURI(java.lang.String)
	 */
	public String lookupNamespaceURI(String prefix) {
		return parent.lookupNamespaceURI(prefix);
	}

	/**
	 * @param namespaceURI
	 * @return
	 * @see org.w3c.dom.Node#lookupPrefix(java.lang.String)
	 */
	public String lookupPrefix(String namespaceURI) {
		return parent.lookupPrefix(namespaceURI);
	}

	/**
	 * 
	 * @see org.w3c.dom.Node#normalize()
	 */
	public void normalize() {
		parent.normalize();
	}

	/**
	 * @param oldChild
	 * @return
	 * @throws DOMException
	 * @see org.w3c.dom.Node#removeChild(org.w3c.dom.Node)
	 */
	public Node removeChild(Node oldChild) throws DOMException {
		return parent.removeChild(oldChild);
	}

	/**
	 * @param newChild
	 * @param oldChild
	 * @return
	 * @throws DOMException
	 * @see org.w3c.dom.Node#replaceChild(org.w3c.dom.Node, org.w3c.dom.Node)
	 */
	public Node replaceChild(Node newChild, Node oldChild) throws DOMException {
		return parent.replaceChild(newChild, oldChild);
	}

	/**
	 * @param nodeValue
	 * @throws DOMException
	 * @see org.w3c.dom.Node#setNodeValue(java.lang.String)
	 */
	public void setNodeValue(String nodeValue) throws DOMException {
		parent.setNodeValue(nodeValue);
	}

	/**
	 * @param prefix
	 * @throws DOMException
	 * @see org.w3c.dom.Node#setPrefix(java.lang.String)
	 */
	public void setPrefix(String prefix) throws DOMException {
		parent.setPrefix(prefix);
	}

	/**
	 * @param textContent
	 * @throws DOMException
	 * @see org.w3c.dom.Node#setTextContent(java.lang.String)
	 */
	public void setTextContent(String textContent) throws DOMException {
		parent.setTextContent(textContent);
	}

	/**
	 * @param key
	 * @param data
	 * @param handler
	 * @return
	 * @see org.w3c.dom.Node#setUserData(java.lang.String, java.lang.Object, org.w3c.dom.UserDataHandler)
	 */
	public Object setUserData(String key, Object data, UserDataHandler handler) {
		return parent.setUserData(key, data, handler);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return parent.toString();
	}

	/**
	 * @return the parent
	 */
	public Node getParent() {
		return parent;
	}
}
