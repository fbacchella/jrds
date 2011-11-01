package jrds.caching;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.Iterator;

import org.apache.log4j.Logger;

/**
 * This is a generic thread safe double linked list. It's very simple and all
 * the operations are so quick that course grained synchronization is more than
 * acceptible.
 */
class DoubleLinkedList<PAYLOAD> implements Iterable<DoubleLinkedList.Node<PAYLOAD>>
{
    static class Node<PAYLOAD> {
        private Node<PAYLOAD> next;
        private Node<PAYLOAD> prev;
        final PAYLOAD value;
        protected Node(PAYLOAD value) {
            this.value = value;
        }
    }
    /** record size to avoid having to iterate */
    private int size = 0;

    /** The logger */
    static final private Logger logger = Logger.getLogger(DoubleLinkedList.class);

    /** LRU double linked list head node */
    private Node<PAYLOAD> first;

    /** LRU double linked list tail node */
    private Node<PAYLOAD> last;

    /**
     * Default constructor.
     */
    public DoubleLinkedList()
    {
        super();
    }

    /**
     * Adds a new node to the end of the link list.
     * <p>
     * @param me
     *            The feature to be added to the Last
     */
    public synchronized void addLast(PAYLOAD value) {
        Node<PAYLOAD> n = new Node<PAYLOAD>(value);
        if ( first == null ) {
            // empty list.
            first = n;
        } else {
            last.next = n;
            n.prev = last;
        }
        last = n;
        size++;
    }

    /**
     * Adds a new node to the start of the link list.
     * <p>
     * @param me
     *            The feature to be added to the First
     */
    public synchronized void addFirst(PAYLOAD value )
    {
        Node<PAYLOAD> n = new Node<PAYLOAD>(value);
        if ( last == null ) {
            // empty list.
            last = n;
        } else {
            first.prev = n;
            n.next = first;
        }
        first = n;
        size++;
    }

    /**
     * Returns the last node from the link list, if there are any nodes.
     * <p>
     * @return The last node.
     */
    public synchronized Node<PAYLOAD> getLast() {
        return last;
    }

    /**
     * Removes the specified node from the link list.
     * <p>
     * @return DoubleLinkedListNode, the first node.
     */
    public synchronized Node<PAYLOAD> getFirst() {
        return first;
    }

    /**
     * Moves an existing node to the start of the link list.
     * <p>
     * @param ln
     *            The node to set as the head.
     */
    public synchronized void makeFirst( Node<PAYLOAD> ln ) {

        if ( ln.prev == null )  {
            // already the first node. or not a node
            return;
        }
        ln.prev.next = ln.next;

        if ( ln.next == null ) {
            // last but not the first.
            last = ln.prev;
            last.next = null;
        }  else {
            // neither the last nor the first.
            ln.next.prev = ln.prev;
        }
        first.prev = ln;
        ln.next = first;
        ln.prev = null;
        first = ln;
    }

    /**
     * Remove all of the elements from the linked list implementation.
     */
    public synchronized void removeAll() {
        for ( Node<PAYLOAD> me = first; me != null; ) {
            if ( me.prev != null ) {
                me.prev = null;
            }
            Node<PAYLOAD> next = me.next;
            me = next;
        }
        first = last = null;
        size = 0;
    }

    /**
     * Removes the specified node from the link list.
     * <p>
     * @param me
     *            Description of the Parameter
     * @return true if an element was removed.
     */
    public synchronized void remove( Node<PAYLOAD> me ) {
        if ( me.next == null ) {
            if ( me.prev == null )  {
                // Make sure it really is the only node before setting head and
                // tail to null. It is possible that we will be passed a node
                // which has already been removed from the list, in which case
                // we should ignore it
                if ( me == first && me == last )
                    first = last = null;
            } else {
                // last but not the first.
                last = me.prev;
                last.next = null;
                me.prev = null;
            }
        } else if ( me.prev == null ) {
            // first but not the last.
            first = me.next;
            first.prev = null;
            me.next = null;
        } else {
            // neither the first nor the last.
            me.prev.next = me.next;
            me.next.prev = me.prev;
            me.prev = me.next = null;
        }
        size--;
    }

    /**
     * Removes the specified node from the link list.
     * <p>
     * @return The last node if there was one to remove.
     */
    public synchronized Node<PAYLOAD> removeLast() {
        Node<PAYLOAD> temp = last;
        if ( last != null )
            remove( last );
        return temp;
    }

    /**
     * Returns the size of the list.
     * <p>
     * @return int
     */
    public synchronized int size() {
        return size;
    }

    /**
     * Dump the cache entries from first to list for debugging.
     */
    public synchronized void debugDumpEntries() {
        if(! logger.isDebugEnabled())
            return;

        logger.debug( "dumping Entries" );
        for ( Node<PAYLOAD> me = first; me != null; me = me.next )
            logger.debug( "dump Entries> payload= '" + me.value + "'" );
    }

    public Iterator<DoubleLinkedList.Node<PAYLOAD>> iterator() {        
        return new Iterator<DoubleLinkedList.Node<PAYLOAD>>() {
            DoubleLinkedList.Node<PAYLOAD> curs = first;
            public boolean hasNext() {
                return curs.next != null;
            }
            public Node<PAYLOAD> next() {
                curs = curs.next;
                return curs;
            }
            public void remove() {
            }
        };
    }

}
