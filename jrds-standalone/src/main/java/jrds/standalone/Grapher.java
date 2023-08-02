/*
 * Created on 25 nov. 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package jrds.standalone;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jrds.Graph;
import jrds.GraphNode;
import jrds.GraphTree;
import jrds.HostsList;
import jrds.Period;
import jrds.PropertiesManager;
import jrds.Renderer;

/**
 * @author bacchell
 *
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class Grapher {
    static final private Logger logger = LoggerFactory.getLogger(Grapher.class);

    public static void main(String[] args) throws Exception {
        String propFile = "jrds.properties";
        if(args.length == 1)
            propFile = args[0];
        PropertiesManager pm = new PropertiesManager(new File(propFile));

        System.getProperties().setProperty("java.awt.headless", "true");
        System.getProperties().putAll(pm);

        HostsList hl = new HostsList(pm);

        Period p = new Period();
        Renderer r = new Renderer(10, pm.tmpdir);
        Collection<Integer> done = new HashSet<>();
        logger.debug("{}", hl.getTrees());
        for(GraphTree graphTree: hl.getTrees()) {
            logger.trace("Tree found: " + graphTree.getName());
            for(GraphNode gn: graphTree.enumerateChildsGraph(null)) {
                if(!done.contains(gn.hashCode())) {
                    done.add(gn.hashCode());
                    logger.debug("New graph: " + gn.getGraphTitle());
                    Graph g = gn.getGraph();
                    g.setPeriod(p);
                    logger.debug("Found graph for probe " + gn.getProbe());
                    try {
                        r.render(g);
                    } catch (Exception e) {
                        logger.error("Error " + e + " with " + gn.getGraphTitle());
                    }
                }
            }
        }
        for(jrds.Renderer.RendererRun rr: r.getWaitings()) {
            logger.debug("Rendering " + rr);
            rr.write();
            rr.clean();
        }
        r.finish();
    }
}
