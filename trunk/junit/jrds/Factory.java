package jrds;

import java.net.URL;

import jrds.ArgFactory;

import org.junit.Assert;
import org.junit.Test;

public class Factory {
	@Test public void argFactory() {
		try {
			ArgFactory af = new ArgFactory();
			Object o = af.makeArg("Integer", "1");
			Assert.assertEquals(o, new Integer(1));
			o = af.makeArg("URL", "http://localhost/");
			Assert.assertEquals(o, new URL("http://localhost/"));
			o = af.makeArg(Integer.class.getName(), "1");
			Assert.assertEquals(o, new Integer(1));
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}


}
