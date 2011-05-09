package jrds;

import java.awt.Color;
import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

public class EnumerateColors {
    static final private Logger logger = Logger.getLogger(EnumerateColors.class);

    @BeforeClass 
    static public void configure() throws IOException {
        jrds.Tools.configure();
        logger.setLevel(Level.TRACE);
        //Tools.setLevel(new String[] {"jrds.Period"}, logger.getLevel());
        //logger.error("after before");
    }

    @Test
    public void listColors() {
        //            ICC_Profile profile = ICC_Profile.getInstance(ICC_Profile.icSigHlsData);
        //            ColorSpace cs = new ICC_ColorSpace(profile);
        //            logger.trace("=====");
        //            for(int j = cs.getNumComponents(); j>0; j--) {
        //                logger.trace(cs.getName(j));
        //            }
        float[] components = new float[3];

        //ColorSpace hls = new ICC_ColorSpace(ICC_Profile.getInstance(ICC_Profile.icSigHlsData));
        for(GraphDesc.Colors c: GraphDesc.Colors.values()) {
            Color jc = c.getColor();
            Color.RGBtoHSB(jc.getRed(), jc.getGreen(), jc.getBlue(), components);
            //jc.getComponents(hls, components);
            
            logger.debug(String.format("%s %.0f %.0f %.0f", c.name(), components[0] * 360, components[1] * 100, components[2] * 100));
        }
    }
}
