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

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import jrds.Util;

import org.apache.log4j.Logger;

/**
 * This is a simple LRUMap. It implements most of the map methods. It is not recommended that you
 * use any but put, get, remove, and clear.
 * <p>
 * Children can implement the processRemovedLRU method if they want to handle the removal of the
 * lest recently used item.
 * <p>
 * This class was abstracted out of the LRU Memory cache. Put, remove, and get should be thread
 * safe. It uses a hashtable and our own double linked list.
 * <p>
 * Locking is done on the instance.
 * <p>
 * @author aaronsm
 */
class LRUMap<K,V> {
    static final private Logger logger = Logger.getLogger(LRUMap.class);

    static final class Payload<K,V> {
        K key;
        V value;
        Payload(K key, V value) {
            this .key = key;
            this.value = value;
        }
    }

    // double linked list for lru
    private DoubleLinkedList<Payload<K,V>> list;

    /** Map where items are stored by key. */
    protected Map<K,DoubleLinkedList.Node<Payload<K,V>>> map;

    private int hitCnt = 0;
    private int missCnt = 0;
    private int putCnt = 0;

    /**
     * This sets the size limit.
     * <p>
     * @param maxObjects
     */
    public LRUMap( int maxObjects ) {
        list = new DoubleLinkedList<Payload<K,V>>();

        // normal hshtable is faster for
        // sequential keys.
        map = new Hashtable<K,DoubleLinkedList.Node<Payload<K,V>>>(maxObjects);
        // map = new ConcurrentHashMap();
    }

    /**
     * This simply returned the number of elements in the map.
     * <p>
     * @see java.util.Map#size()
     */
    public int size() {
        return map.size();
    }

    /**
     * This removes all the items. It clears the map and the double linked list.
     * <p>
     * @see java.util.Map#clear()
     */
    public void clear() {
        map.clear();
        list.removeAll();
    }

    /**
     * Returns true if the map is empty.
     * <p>
     * @see java.util.Map#isEmpty()
     */
    public boolean isEmpty() {
        return map.size() == 0;
    }

    /**
     * Returns true if the map contains an element for the supplied key.
     * <p>
     * @see java.util.Map#containsKey(java.lang.Object)
     */
    public boolean containsKey( Object key ) {
        return map.containsKey( key );
    }

    /**
     * This is an expensive operation that determines if the object supplied is mapped to any key.
     * <p>
     * @see java.util.Map#containsValue(java.lang.Object)
     */
    public boolean containsValue( Object value ) {
        return map.containsValue( value );
    }

    public Iterable<V> values() {
        final Iterator<DoubleLinkedList.Node<Payload<K,V>>> i = this.map.values().iterator();
        return new Iterable<V>() {
            public Iterator<V> iterator() {
                return new Iterator<V>() {
                    public boolean hasNext() {
                        return i.hasNext();
                    }
                    public V next() {
                        return i.next().value.value;
                    }
                    public void remove() {
                    }
                };
            }
            
        };
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map#get(java.lang.Object)
     */
    public V get( Object key ) {
        V retVal = null;

        logger.debug(Util.delayedFormatString("getting item  for key %s", key));

        DoubleLinkedList.Node<Payload<K,V>> me = map.get(key);

        if ( me != null ) {
            logger.debug(Util.delayedFormatString("LRUMap hit for " + key ));
            hitCnt++;
            retVal = me.value.value;
            list.makeFirst( me );
        }
        else {
            missCnt++;
            logger.debug(Util.delayedFormatString("LRUMap miss for %s", key));
        }

        // verifyCache();
        return retVal;
    }

    /**
     * This gets an element out of the map without adjusting it's posisiton in the LRU. In other
     * words, this does not count as being used. If the element is the last item in the list, it
     * will still be the last itme in the list.
     * <p>
     * @param key
     * @return Object
     */
    public V getQuiet( Object key ) {
        V ce = null;

        DoubleLinkedList.Node<Payload<K,V>> me = map.get(key);
        if ( me != null ) {
            logger.debug(Util.delayedFormatString("LRUMap quiet hit for %s", key ));
            ce = me.value.value;
        }
        else
            logger.debug(Util.delayedFormatString("LRUMap quiet miss for %s", key ));

        return ce;
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map#remove(java.lang.Object)
     */
    public V remove( Object key ) {
        logger.debug(Util.delayedFormatString("removing item for key: %s",key ));

        DoubleLinkedList.Node<Payload<K,V>> me = null;
        // remove single item.
        synchronized (this) {
            me = map.remove( key );
        }

        if ( me != null ) {
            list.remove( me );
            return me.value.value;
        }

        return null;
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    public V put( K key, V value ) {
        putCnt++;

        DoubleLinkedList.Node<Payload<K,V>> old = null;
        synchronized ( this ) {
            // TODO address double synchronization of addFirst, use write lock
            list.addFirst( new Payload<K, V>(key, value) );
            // this must be synchronized
            old = map.put( list.getFirst().value.key, list.getFirst() );

            // If the node was the same as an existing node, remove it.
            if ( old != null && list.getFirst().value.key.equals( old.value.key ) )
                list.remove( old );
        }

        if ( old != null )
            return old.value.value;
        return null;
    }

    /**
     * Returns the size of the list.
     * <p>
     * @return int
     */
    private int dumpCacheSize() {
        return list.size();
    }

    /**
     * Dump the cache entries from first to list for debugging.
     */
    public void dumpCacheEntries() {
        logger.debug( "dumpingCacheEntries" );
        for(DoubleLinkedList.Node<Payload<K,V>> me: list) {
            logger.debug(Util.delayedFormatString( "dumpCacheEntries> key=%s, val=%s", me.value,  me.value));
        }
    }

    /**
     * Dump the cache map for debugging.
     */
    public void dumpMap() {
        if(! logger.isDebugEnabled())
            return;
        logger.debug( "dumpingMap" );
        for(Map.Entry<K, DoubleLinkedList.Node<Payload<K,V>>> e: map.entrySet()) {
            logger.debug(Util.delayedFormatString("dumpMap> key=%s, val=%s", e.getKey(), e.getValue().value.value) );
        }
    }

    /**
     * Checks to see if all the items that should be in the cache are. Checks consistency between
     * List and map.
     */
    protected void verifyCache()
    {
        if ( !logger.isDebugEnabled() )
            return;

        boolean found = false;
        logger.debug( "verifycache: mapContains " + map.size() + " elements, linked list contains " + dumpCacheSize()
                + " elements" );
        logger.debug( "verifycache: checking linked list by key " );
        for ( DoubleLinkedList.Node<Payload<K,V>> li: list)
        {
            Object key = li.value.key;
            if ( !map.containsKey( key ) )
            {
                logger.error( "verifycache: map does not contain key : " + li.value.key );
                logger.error( "li.hashcode=" + li.value.key.hashCode() );
                logger.error( "key class=" + key.getClass() );
                logger.error( "key hashcode=" + key.hashCode() );
                logger.error( "key toString=" + key.toString() );
                dumpMap();
            }
            else if ( map.get( li.value.key ) == null )
            {
                logger.error( "verifycache: linked list retrieval returned null for key: " + li.value.key );
            }
        }

        logger.debug( "verifycache: checking linked list by value " );
        for ( DoubleLinkedList.Node<Payload<K,V>> li3: list) {
            if ( map.containsValue( li3 ) == false ) {
                logger.error( "verifycache: map does not contain value : " + li3 );
                dumpMap();
            }
        }

        logger.debug( "verifycache: checking via keysets!" );
        for(K val: map.keySet()) {
            found = false;
            for ( DoubleLinkedList.Node<Payload<K,V>> li2: list) {
                if ( val.equals( li2.value.key ) ) {
                    found = true;
                    break;
                }
            }
            if ( !found ) {
                logger.error( "verifycache: key not found in list : " + val );
                dumpCacheEntries();
                if ( map.containsKey( val ) ) {
                    logger.error( "verifycache: map contains key" );
                }
                else {
                    logger.error( "verifycache: map does NOT contain key, what the HECK!" );
                }
            }
        }
    }

    /**
     * Logs an error is an element that should be in the cache is not.
     * <p>
     * @param key
     */
    protected void verifyCache( Object key ) {
        if ( !logger.isDebugEnabled() ) {
            return;
        }

        boolean found = false;

        // go through the linked list looking for the key
        for ( DoubleLinkedList.Node<Payload<K,V>> li: list) {
            if ( li.value.key == key ) {
                found = true;
                logger.debug( "verifycache(key) key match: " + key );
                break;
            }
        }
        if ( !found ) {
            logger.error( "verifycache(key), couldn't find key! : " + key );
        }
    }

    /**
     * This is called when an item is removed from the LRU. We just log some information.
     * <p>
     * Children can implement this method for special behavior.
     * @param key
     * @param value
     */
    protected void processRemovedLRU( K key, V value ) {
        if ( logger.isDebugEnabled() )
        {
            logger.debug( "Removing key: [" + key + "] from LRUMap store, value = [" + value + "]" );
            logger.debug( "LRUMap store size: '" + this.size() + "'." );
        }
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map#keySet()
     */
    public Set<K> keySet() {
        return map.keySet();
    }
}
