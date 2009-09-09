

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.imageio.ImageIO;
import javax.xml.parsers.ParserConfigurationException;

import jrds.Tools;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.PNGEncodeParam;

public class BenchJAI {
	static final private Logger logger = Logger.getLogger(BenchJAI.class);

	static final File IMAGE = new File("/Users/bacchell/rrd4j-demo/demo.png");
	@BeforeClass
	static public void configure() throws IOException {
		Tools.configure();
		Tools.setLevel(new String[] {"jrds.Util"}, logger.getLevel());
	}

	@Test
	public void bench() throws ParserConfigurationException, IOException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		int iteration = 1;
		
		BufferedImage img = ImageIO.read(IMAGE);

		ByteArrayOutputStream streamBuffer = new ByteArrayOutputStream((int) (IMAGE.length() * 1.1));

		PNGEncodeParam params = new PNGEncodeParam.Palette(); //PNGEncodeParam.getDefaultEncodeParam(img);
		for(Method m:params.getClass().getMethods()) {
			if(m.getName().startsWith("is")) {
				Object ret = m.invoke(params);
				logger.debug(m.getName() + " " + ret);
			}
		}
		ImageEncoder encoder = ImageCodec.createImageEncoder("PNG", streamBuffer, params);

		long starttime;
		long endtime;

		starttime = System.currentTimeMillis();
		for(int i=0; i< iteration; i++) {
			streamBuffer.reset();
			encoder.encode(img);
			streamBuffer.flush();
		}
		endtime = System.currentTimeMillis();;
		long jaitime = endtime- starttime;
	
		logger.debug("Compression for JAI " + streamBuffer.size());
		starttime = System.currentTimeMillis();
		for(int i=0; i< iteration; i++) {
			streamBuffer.reset();
			ImageIO.write(img, "png", streamBuffer);
			streamBuffer.flush();
		}
		endtime = System.currentTimeMillis();;
		logger.debug("Compression for ImageIO " + streamBuffer.size());
		long imageiotime = endtime - starttime;
		logger.debug("" + imageiotime + " " +  jaitime);

	}

}
