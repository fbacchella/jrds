/*
 * Created on 17 juil. 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package jrds.standalone;

import java.io.IOException;

import org.snmp4j.smi.OID;

/**
 * @author bacchell
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TestBackEnd {

	public static void main(String[] args) throws IOException {
		OID a = new OID("1.2.3");
		OID b = new OID("1.2.3.4");
		a.fromSubIndex(b, true);
		System.out.println(a);
	}
}
