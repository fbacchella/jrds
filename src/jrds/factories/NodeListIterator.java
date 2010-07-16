package jrds.factories;

import java.util.Iterator;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import jrds.factories.xml.JrdsNode;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class NodeListIterator implements Iterable<JrdsNode>, NodeList {

	final Node d;
	final XPathExpression path;
	final NodeList nl;

	public NodeListIterator(Node d, XPathExpression path) {
		this.d = d;
		this.path = path;
		try {
			this.nl = (NodeList)path.evaluate(d, XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			throw new RuntimeException("XPathExpressionException",e);
		}
	}

	public NodeListIterator(NodeList nl) {
		if(nl == null) {
			throw new NullPointerException("Node list invalid");
		}
		this.nl = nl;
		d = null;
		path = null;
	}

	public Iterator<JrdsNode> iterator() {
		Iterator<JrdsNode> iter  = new Iterator<JrdsNode>() {
			int i = 0;
			int last = nl.getLength();
			public boolean hasNext() {
				return i < last;
			}
			public JrdsNode next() {
				return new JrdsNode(nl.item(i++));
			}
			public void remove() {
				throw new UnsupportedOperationException("Cannot remove in a JrdsNode");
			}

		};
		return iter;
	}

	public int getLength() {
		if(nl==null)
			return 0;
		return nl.getLength();
	}

	public Node item(int index) {
		if(nl==null)
			return null;
		return nl.item(index);
	}

}
