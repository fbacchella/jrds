package jrds;

// ----------------------------------------------------------------------------
// $Id$

import java.util.List;
import org.apache.log4j.Logger;
import java.util.Iterator;
import java.util.ArrayList;
import java.lang.reflect.Constructor;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 * @author Fabrice Bacchella
 * @version $Revision$
 */
public class GraphFactory {
    static private final Logger logger = JrdsLogger.getLogger(GraphFactory.class);
    static final private List graphPackages = new ArrayList(2);

    static {
        graphPackages.add("jrds.graph.");
    }

    /**
     * Private constructor
     */
    private GraphFactory() {
    }

    public static RdsGraph makeGraph(Object className, Probe probe) {
        RdsGraph retValue = null;

        //A class was used as a param
        //We only need to instanciate it
        if (className instanceof Class) {
            Class graphClass = (Class) className;

            try {
                if (RdsGraph.class.isAssignableFrom(graphClass)) {
                    Class[] probeClassArray = new Class[] {Probe.class};
                    Object[] args = new Object[] {probe};

                    Constructor co = graphClass.getConstructor(probeClassArray);
                    retValue = (RdsGraph) co.newInstance(args);
                }
                else {
                    logger.warn("didn't get a RdsGraph but a " +
                            graphClass.getClass().getName());
                }
            }
            catch (Exception ex) {
                logger.warn("Error during RdsGraph creation of type " + className +
                            ": " + ex, ex);
            }
        }
        //We get a GraphDesc
        //We need to instanciate a Graph using this description
        else if(className instanceof GraphDesc ) {
            retValue = new RdsGraph(probe, (GraphDesc) className);
        }
        else if(className instanceof String) {
             Class.getResourceAsStream("/monfichier.prop");
        }
        return retValue;
    }

    static private Class resolvClass(String name, List packList) {
        Class retValue = null;
        for (Iterator i = packList.iterator(); i.hasNext() && retValue == null; ) {
            try {
                String packageTry = (String) i.next();
                retValue = Class.forName(packageTry + name);
            }
            catch (ClassNotFoundException ex) {
            }
            catch (NoClassDefFoundError ex) {
            }
        }
        if (retValue == null)
            logger.warn("Class " + name + " not found");
        return retValue;
    }

}
