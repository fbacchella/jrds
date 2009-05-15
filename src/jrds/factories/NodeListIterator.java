package jrds.factories;

import java.util.Iterator;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import jrds.factories.xml.JrdsNode;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class NodeListIterator implements Iterable<JrdsNode> {

	final Node d;
	final XPathExpression path;
	final NodeList nl;

	public NodeListIterator(Node d, XPathExpression path) {
		this.d = d;
		this.path = path;
		this.nl = null;
	}

	public NodeListIterator(NodeList nl) {
		this.nl = nl;
		d = null;
		path = null;
	}

	public Iterator<JrdsNode> iterator() {
		try {
			final NodeList localnl;
			if(nl != null)
				localnl = nl;
			else
				localnl = (NodeList)path.evaluate(d, XPathConstants.NODESET);
			Iterator<JrdsNode> iter  = new Iterator<JrdsNode>() {
				int i = 0;
				int last = localnl.getLength();
				public boolean hasNext() {
					return i < last;
				}
				public JrdsNode next() {
					return new JrdsNode(localnl.item(i++));
				}
				public void remove() {
					throw new UnsupportedOperationException();
				}

			};
			return iter;
		} catch (XPathExpressionException e) {
			throw new RuntimeException("XPathExpressionException",e);
		}
	}

}
