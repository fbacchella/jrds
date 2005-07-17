/*
 * Created on 13 juil. 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package jrds;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.jrobin.core.RrdBackendFactory;
import org.jrobin.core.RrdDb;
import org.jrobin.core.RrdDbPool;
import org.jrobin.core.RrdException;
import org.jrobin.core.RrdOpener;

/**
 * @author bacchell
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class StoreOpener {
	static final private Logger logger = JrdsLogger.getLogger(StoreOpener.class);
	static final private RrdOpener opener = new RrdOpener(true);

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @throws java.io.IOException
	 * @throws org.jrobin.core.RrdException
	 */
	public final static RrdDb getRrd(String arg0)
			throws IOException, RrdException {
		return opener.getRrd(arg0, RrdBackendFactory.getDefaultFactory());
	}
	/**
	 * @param arg0
	 */
	public final static void releaseRrd(RrdDb arg0)  {
		try {
			opener.releaseRrd(arg0);
			RrdDbPool.getInstance().dump();
		} catch (Exception e) {
			logger.debug("Strange error" + e);
		}
	}
}
