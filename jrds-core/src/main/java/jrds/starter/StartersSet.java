package jrds.starter;

/**
 * @author Fabrice Bacchella
 * @deprecated This interface is a compatibility wrapper for the StarterNode
 *             class
 */
public interface StartersSet {
    /**
     * @param s the starter to register
     * @param none ignored argument
     * @return the starter that will be used
     * @deprecated The second argument is not used, it can be removed
     */
    Starter registerStarter(Starter s, StarterNode none);

    <StarterClass extends Starter> StarterClass find(Class<StarterClass> sc, StarterNode node);

    /**
     * @param key
     * @deprecated Use the generic variant instead : &lt;StarterClass extends
     *             Starter&gt; StarterClass find(Class&lt;StarterClass&gt; sc);
     * @return
     */
    Starter find(Object key);

    <StarterClass extends Starter> StarterClass find(Class<StarterClass> sc, Object key);

    boolean isStarted(Object key);

    void setParent(StartersSet s);

    StartersSet getParent();

    StarterNode getLevel();
}
