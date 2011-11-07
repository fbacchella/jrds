package jrds.caching;

import java.lang.reflect.Array;
import java.util.Iterator;

import jrds.Util;

import org.apache.log4j.Logger;

/**
 * A array that also provides a LRU access. It should be thread safe.
 * @author Fabrice Bacchella
 *
 * @param <V> the stored type
 */
class LRUArray<V> {
    static final private Logger logger = Logger.getLogger(LRUArray.class);

    static final class Payload<V> {
        int key;
        V value;
        Payload(int key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    /** double linked list for LRU **/
    private final DoubleLinkedList<Payload<V>> list;

    /** Array where the item are stored **/
    private final DoubleLinkedList.Node<Payload<V>>[] map;

    /**
     * This sets the size limit.
     * <p>
     * @param maxObjects
     */
    @SuppressWarnings("unchecked")
    public LRUArray( int maxObjects ) {
        list = new DoubleLinkedList<Payload<V>>();
        map = (DoubleLinkedList.Node<Payload<V>>[])Array.newInstance(DoubleLinkedList.Node.class, maxObjects);
    }

    /**
     * This simply returned the number of elements in the array.
     */
    public int size() {
        return map.length;
    }

    /**
     * This removes all the items. It clears the array and the double linked list.
     */
    public void clear() {
        for(int i=0; i < map.length; i++) {
            map[i] = null;
        }
        list.removeAll();
    }

    /**
     * Returns true if the array is empty.
     */
    public boolean isEmpty() {
        return list.size() == 0;
    }

    /**
     * Returns true if the array contains an element for the supplied index.
     */
    public boolean containsKey( int key ) {
        return map[key] != null;
    }

    /**
     * This is an expensive operation that determines if the object supplied is mapped to any index.
     */
    public boolean containsValue( V value ) {
        for(DoubleLinkedList.Node<Payload<V>> n: list) {
            if(n.value.value.equals(value))
                return true;
        }
        return false;
    }

    public Iterable<V> values() {
        final Iterator<DoubleLinkedList.Node<Payload<V>>> i = list.iterator();
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

    /**
     * Get the element at the index and update the LRU
     * @param the index
     * @return the element
     */
    public V get(int key) {
        V retVal = null;

        logger.debug(Util.delayedFormatString("getting item  for key %s", key));

        DoubleLinkedList.Node<Payload<V>> me = map[key];

        if ( me != null ) {
            retVal = me.value.value;
            list.makeFirst( me );
        }

        return retVal;
    }

    /**
     * This gets an element out of the array without adjusting it's position in the LRU. In other
     * words, this does not count as being used. If the element is the last item in the list, it
     * will still be the last item in the list.
     * <p>
     * @param key
     * @return Object
     */
    public V getQuiet( int key ) {
        DoubleLinkedList.Node<Payload<V>> me = map[key];
        if ( me != null ) {
            return  me.value.value;
        }
        return null;
    }

    /**
     * Remove the last element in the LRU
     * @return
     */
    public V removeEldest() {
        return remove(list.getLast().value.key);
    }

    /**
     * Remove an element from the array and return it
     * @param key
     * @return
     */
    public V remove( int key ) {
        logger.debug(Util.delayedFormatString("removing item for key: %s", key));

        // remove single item.
        synchronized (this) {
            DoubleLinkedList.Node<Payload<V>> me = map[key];
            map[key] = null;
            if ( me != null ) {
                list.remove(me);
                return me.value.value;
            }
        }

        return null;
    }

    /**
     * Put the the given element at the index
     * @param key
     * @param value
     * @return the old element
     */
    public V put( int key, V value ) {

        DoubleLinkedList.Node<Payload<V>> old = null;
        synchronized ( this ) {
            // TODO address double synchronization of addFirst, use write lock
            list.addFirst( new Payload<V>(key, value) );
            // this must be synchronized
            old = map[key];
            map[key] =  list.getFirst();

            // If the node was the same as an existing node, remove it.
            if ( old != null && list.getFirst().value.key == old.value.key )
                list.remove( old );
        }

        if ( old != null )
            return old.value.value;
        return null;
    }


    /**
     * Add an new element and make it the oldest
     * @param key
     * @param value
     * @return
     */
    public V putLast( int key, V value ) {

        DoubleLinkedList.Node<Payload<V>> old = null;
        synchronized ( this ) {
            // TODO address double synchronization of addFirst, use write lock
            list.addLast( new Payload<V>(key, value) );
            // this must be synchronized
            old = map[key];
            map[key] =  list.getLast();

            // If the node was the same as an existing node, remove it.
            if ( old != null && list.getLast().value.key == old.value.key )
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
        for(DoubleLinkedList.Node<Payload<V>> me: list) {
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
        for(DoubleLinkedList.Node<Payload<V>> n: map) {
            logger.debug(Util.delayedFormatString("dumpMap> key=%s, val=%s", n.value.key, n.value.value) );

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
        logger.debug( "verifycache: mapContains " + map.length + " elements, linked list contains " + dumpCacheSize()
                + " elements" );
        logger.debug( "verifycache: checking linked list by key " );
        for ( DoubleLinkedList.Node<Payload<V>> li: list)
        {
            int key = li.value.key;
            if ( map[key] == null )
            {
                logger.error( "verifycache: map does not contain key : " + li.value.key );
                dumpMap();
            }
            else if ( map[li.value.key] == null )
            {
                logger.error( "verifycache: linked list retrieval returned null for key: " + li.value.key );
            }
        }

        logger.debug( "verifycache: checking linked list by value " );
        for ( DoubleLinkedList.Node<Payload<V>> li3: list) {
            if ( containsValue( li3.value.value ) == false ) {
                logger.error( "verifycache: map does not contain value : " + li3 );
                dumpMap();
            }
        }

        logger.debug( "verifycache: checking via keysets!" );
        for(int val = 0 ; val < map.length; val++) {
            found = false;
            for ( DoubleLinkedList.Node<Payload<V>> li2: list) {
                if ( val == li2.value.key ) {
                    found = true;
                    break;
                }
            }
            if ( !found ) {
                logger.error( "verifycache: key not found in list : " + val );
                dumpCacheEntries();
                if ( map[val] != null ) {
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
    protected void verifyCache( int key ) {
        if ( !logger.isDebugEnabled() ) {
            return;
        }

        boolean found = false;

        // go through the linked list looking for the key
        for ( DoubleLinkedList.Node<Payload<V>> li: list) {
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
}
