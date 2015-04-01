package jrds;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.rrd4j.ConsolFun;
import org.rrd4j.core.ArcDef;

/**
 * A archive container
 * 
 * @author Fabrice Bacchella
 *
 */
public class ArchivesSet extends ArrayList<ArcDef> {
    static private final Logger logger = Logger.getLogger(ArchivesSet.class);

    private final String name;

    public ArchivesSet(String name){
        super();
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void addArchive(ArcDef arcDef) {
        add(arcDef);
        logger.trace(Util.delayedFormatString("Adding archive: %s", arcDef.dump()));       
    }

    public ArcDef[] getArchives() {
        return toArray(new ArcDef[size()]);
    }

    private static final ArcDef[] DEFAULTARC = {
        new ArcDef(ConsolFun.AVERAGE, 0.5, 1, 12 * 24 * 30 * 3),
        new ArcDef(ConsolFun.AVERAGE, 0.5, 12, 24 * 365), 
        new ArcDef(ConsolFun.AVERAGE, 0.5, 288, 365 * 2)
    };

    public static final ArchivesSet DEFAULT = new ArchivesSet("_default_") {
        {
            this.addAll(Arrays.asList(DEFAULTARC));
        }
        public ArcDef[] getArchives() {
            return DEFAULTARC;
        }


    };

}
