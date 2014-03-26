package jrds.configuration;

import java.lang.reflect.InvocationTargetException;

import jrds.ArchivesSet;
import jrds.Util;
import jrds.factories.xml.JrdsDocument;
import jrds.factories.xml.JrdsElement;

import org.rrd4j.ConsolFun;
import org.rrd4j.core.ArcDef;

public class ArchivesSetBuilder extends ConfigObjectBuilder<ArchivesSet> {

    protected ArchivesSetBuilder() {
        super(ConfigType.ARCHIVESSET);
    }

    @Override
    ArchivesSet build(JrdsDocument n) throws InvocationTargetException {
        try {
            return getArchiveDefs(n.getRootElement());
        }
        catch (IllegalArgumentException e) {
            throw new InvocationTargetException(e, ArchivesSetBuilder.class.getName());            
        }
    }

    private ArcDef getArchDef(JrdsElement archiveElement){
        String cfName = archiveElement.getElementbyName("consolFun").getTextContent();
        ConsolFun consolFun = null;
        if(cfName != null && ! "".equals(cfName.trim())) {
            consolFun = ConsolFun.valueOf(cfName.trim().toUpperCase());            
        }
        double xff = Util.parseStringNumber(archiveElement.getElementbyName("xff").getTextContent(), Double.NaN);
        int steps =  Util.parseStringNumber(archiveElement.getElementbyName("steps").getTextContent(), 0);
        int rows = Util.parseStringNumber(archiveElement.getElementbyName("rows").getTextContent(), 0);
        return new ArcDef(consolFun, xff, steps, rows);
    }

    private ArchivesSet getArchiveDefs(JrdsElement archivesSetElements){
        String name = archivesSetElements.getAttribute("name");
        ArchivesSet arcset = new ArchivesSet(name);
        for(JrdsElement archiveElement : archivesSetElements.getChildElementsByName("archive")){
            ArcDef arcDef = getArchDef(archiveElement);
            arcset.addArchive(arcDef);
        }
        return arcset;
    }
}
