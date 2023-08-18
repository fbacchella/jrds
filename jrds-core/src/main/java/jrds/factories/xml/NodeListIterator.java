package jrds.factories.xml;

import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class NodeListIterator<N extends AbstractJrdsNode<?>> implements Iterable<N>, NodeList {

    private final NodeList nl;

    public NodeListIterator(Node d, XPathExpression path) {
        try {
            this.nl = (NodeList) path.evaluate(d, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new RuntimeException("XPathExpressionException", e);
        }
    }

    public NodeListIterator(NodeList nl) {
        if(nl == null) {
            throw new NullPointerException("Node list invalid");
        }
        this.nl = nl;
    }

    public Iterator<N> iterator() {
        return new Iterator<>() {
            int i = 0;
            final int last = nl.getLength();

            public boolean hasNext() {
                return i < last;
            }

            public N next() {
                if (hasNext()) {
                    return AbstractJrdsNode.build(nl.item(i++));
                } else {
                    throw new NoSuchElementException();
                }
            }

            public void remove() {
                throw new UnsupportedOperationException("Cannot remove in a JrdsNode");
            }

        };
    }

    public int getLength() {
        if(nl == null)
            return 0;
        return nl.getLength();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.NodeList#item(int)
     */
    public N item(int index) {
        if(nl == null)
            return null;
        return AbstractJrdsNode.build(nl.item(index));
    }

}
