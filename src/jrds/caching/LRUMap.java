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

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
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
class LRUMap<K,V> implements Map<K,V>
{
    static final private Logger logger = Logger.getLogger(LRUMap.class);

    // double linked list for lru
    private DoubleLinkedList<K,V> list;

    /** Map where items are stored by key. */
    protected Map<K,DoubleLinkedList.Node<K,V>> map;
    protected Map<K,V> mapValue;

    int hitCnt = 0;

    int missCnt = 0;

    int putCnt = 0;

    // if the max is less than 0, there is no limit!
    int maxObjects = -1;

    // make configurable
    private int chunkSize = 1;

    /**
     * This sets the size limit.
     * <p>
     * @param maxObjects
     */
    public LRUMap( int maxObjects ) {
        this.maxObjects = maxObjects;
        list = new DoubleLinkedList<K, V>();

        // normal hshtable is faster for
        // sequential keys.
        map = new Hashtable<K,DoubleLinkedList.Node<K,V>>(maxObjects);
        mapValue = new HashMap<K,V>(maxObjects);
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
    public void clear()
    {
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

    /*
     * (non-Javadoc)
     * @see java.util.Map#values()
     */
    public Collection<V> values()
    {
        return mapValue.values();
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map#putAll(java.util.Map)
     */
    public void putAll( Map<? extends K,? extends V> source )
    {
        if ( source != null )
            for(Map.Entry<? extends K,? extends V> entry: source.entrySet() ) {
                put( entry.getKey(), entry.getValue() );
            }
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map#get(java.lang.Object)
     */
    public V get( Object key ) {
        V retVal = null;

        logger.debug(Util.delayedFormatString("getting item  for key %s", key));

        DoubleLinkedList.Node<K, V> me = map.get( key );

        if ( me != null ) {
            logger.debug(Util.delayedFormatString("LRUMap hit for " + key ));
            hitCnt++;
            retVal = me.value;
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

        DoubleLinkedList.Node<K,V> me = map.get(key);
        if ( me != null ) {
            logger.debug(Util.delayedFormatString("LRUMap quiet hit for %s", key ));
            ce = me.value;
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

        DoubleLinkedList.Node<K,V> me = null;
        // remove single item.
        synchronized (this) {
            mapValue.remove(key);
            me = map.remove( key );
        }

        if ( me != null ) {
            list.remove( me );
            return me.value;
        }

        return null;
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    public V put( K key, V value )
    {
        putCnt++;

        DoubleLinkedList.Node<K,V> old = null;
        synchronized ( this )
        {
            // TODO address double synchronization of addFirst, use write lock
            list.addFirst( key, value );
            // this must be synchronized
            old = map.put( list.getFirst().key, list.getFirst() );
            mapValue.put(key, value);

            // If the node was the same as an existing node, remove it.
            if ( old != null && list.getFirst().key.equals( old.key ) )
                list.remove( old );
        }

        int size = map.size();
        // If the element limit is reached, we need to spool

        if ( maxObjects >= 0 && size > maxObjects )
        {
            logger.debug("In memory limit reached, removing least recently used.");

            // Write the last 'chunkSize' items to disk.
            int chunkSizeCorrected = Math.min( size, getChunkSize() );

            logger.debug(Util.delayedFormatString("About to remove the least recently used. map size: " + size + ", max objects: "
                    + this.maxObjects + ", items to spool: " + chunkSizeCorrected ));

            // The spool will put them in a disk event queue, so there is no
            // need to pre-queue the queuing. This would be a bit wasteful
            // and wouldn't save much time in this synchronous call.

            for ( int i = 0; i < chunkSizeCorrected; i++ ) {
                synchronized ( this ) {
                    if ( list.getLast() != null ) {
                        if ( list.getLast() != null ) {
                            processRemovedLRU(list.getLast().key, list.getLast().value );
                            if ( !map.containsKey( list.getLast().value ) ) {
                                logger.error(Util.delayedFormatString("update: map does not contain key: "
                                        + list.getLast().key ));
                                verifyCache();
                            }
                            mapValue.remove(list.getLast().key);
                            if ( map.remove(list.getLast().key ) == null ) {
                                logger.warn(Util.delayedFormatString("update: remove failed for key: %s", list.getLast().key));
                                verifyCache();
                            }
                        }
                        else {
                            throw new Error( "update: last.ce is null!" );
                        }
                        list.removeLast();
                    }
                    else {
                        verifyCache();
                        throw new Error( "update: last is null!" );
                    }
                }
            }

            logger.debug( "update: After spool map size: " + map.size() );
            if ( map.size() != dumpCacheSize() ) {
                logger.error( "update: After spool, size mismatch: map.size() = " + map.size() + ", linked list size = "
                        + dumpCacheSize() );
            }
        }

        if ( old != null )
            return old.value;
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
        for(DoubleLinkedList.Node<K, V> me: list) {
            logger.debug( "dumpCacheEntries> key=" + me.key + ", val=" + me.value );
        }
    }

    /**
     * Dump the cache map for debugging.
     */
    public void dumpMap() {
        logger.debug( "dumpingMap" );
        for ( Iterator itr = map.entrySet().iterator(); itr.hasNext(); )
        {
            Map.Entry e = (Map.Entry) itr.next();
            LRUElementDescriptor me = (LRUElementDescriptor) e.getValue();
            logger.debug( "dumpMap> key=" + e.getKey() + ", val=" + me.getPayload() );
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
        for ( DoubleLinkedList.Node<K,V> li: list)
        {
            Object key = li.key;
            if ( !map.containsKey( key ) )
            {
                logger.error( "verifycache: map does not contain key : " + li.key );
                logger.error( "li.hashcode=" + li.key.hashCode() );
                logger.error( "key class=" + key.getClass() );
                logger.error( "key hashcode=" + key.hashCode() );
                logger.error( "key toString=" + key.toString() );
                dumpMap();
            }
            else if ( map.get( li.key ) == null )
            {
                logger.error( "verifycache: linked list retrieval returned null for key: " + li.key );
            }
        }

        logger.debug( "verifycache: checking linked list by value " );
        for ( DoubleLinkedList.Node<K, V> li3: list)
        {
            if ( map.containsValue( li3 ) == false )
            {
                logger.error( "verifycache: map does not contain value : " + li3 );
                dumpMap();
            }
        }

        logger.debug( "verifycache: checking via keysets!" );
        for ( Iterator itr2 = map.keySet().iterator(); itr2.hasNext(); )
        {
            found = false;
            Serializable val = null;
            try {
                val = (Serializable) itr2.next();
            }
            catch ( NoSuchElementException nse ) {
                logger.error( "verifycache: no such element exception" );
            }

            for ( DoubleLinkedList.Node<K,V> li2: list) {
                if ( val.equals( li2.key ) ) {
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
        for ( DoubleLinkedList.Node<K,V> li: list) {
            if ( li.key == key ) {
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

    /**
     * The chunk size is the number of items to remove when the max is reached. By default it is 1.
     * <p>
     * @param chunkSize The chunkSize to set.
     */
    public void setChunkSize( int chunkSize )
    {
        this.chunkSize = chunkSize;
    }

    /**
     * @return Returns the chunkSize.
     */
    public int getChunkSize() {
        return chunkSize;
    }

    /**
     * This returns a set of entries. Our LRUMapEntry is used since the value stored in the
     * underlying map is a node in the double linked list. We wouldn't want to return this to the
     * client, so we construct a new entry with the payload of the node.
     * <p>
     * TODO we should return out own set wrapper, so we can avoid the extra object creation if it
     * isn't necessary.
     * <p>
     * @see java.util.Map#entrySet()
     */
    public synchronized Set<Map.Entry<K, V>> entrySet() {
        return mapValue.entrySet();
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map#keySet()
     */
    public Set<K> keySet() {
        return mapValue.keySet();
    }
}
