/*
 * Created on 17 juil. 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package jrds.standalone;

import java.io.IOException;

import org.rrd4j.core.RrdBackend;

import jrds.RrdCachedFileBackend;
import jrds.RrdCachedFileBackendFactory;

/**
 * @author bacchell
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TestBackEnd {

	public static void main(String[] args) throws IOException {
		//RrdCachedFileBackend fbe = new RrdCachedFileBackend(args[0], false, 0, 0, 10);
		RrdCachedFileBackendFactory befact = new RrdCachedFileBackendFactory();
		RrdCachedFileBackendFactory.setSyncPeriod(5);
		//RrdCachedFileBackend fbe = befact.opencached(args[0], false, 0);
		RrdBackend fbe = befact.open(args[0], false, 0);
		byte b[] = new byte[1];
		((RrdCachedFileBackend)fbe).read((long)0,b);
		b[0]='_';
		((RrdCachedFileBackend)fbe).write(20,b);
		b[0]=' ';
		((RrdCachedFileBackend)fbe).write(30,b);
		((RrdCachedFileBackend)fbe).read(0,b);
		try {
			Thread.sleep(10 * 1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		fbe.close();
		//System.out.println(b);
	}
}
