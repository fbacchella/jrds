package jrds.webapp;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import jrds.GetMoke;
import jrds.JrdsTester;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class UrlParserTester  extends JrdsTester {
	static private final Map<String, String[]> parameters = new HashMap<String, String[]>();
	static private final HttpServletRequest req = GetMoke.getRequest(parameters);

	@Test public void checkId() {
		parameters.clear();
		parameters.put("id", new String[] { "1" });
		ParamsBean pb = new ParamsBean(req);
		Assert.assertEquals(1, pb.getId());
	}
	@Test public void checkSortedTrue() {
		parameters.clear();
		parameters.put("sort", new String[] { "1" });
		ParamsBean pb = new ParamsBean(req);
		Assert.assertTrue(pb.isSorted());
	}
	@Test public void checkSortedFalseDefault() {
		parameters.clear();
		ParamsBean pb = new ParamsBean(req);
		Assert.assertTrue(! pb.isSorted());
	}
	@Test public void checkSortedFalse() {
		parameters.clear();
		parameters.put("sort", new String[] { "0" });
		ParamsBean pb = new ParamsBean(req);
		Assert.assertTrue(! pb.isSorted());
	}
	@Test public void checkUrl1() {
		parameters.clear();
		parameters.put("host", new String [] { "host"} );
		parameters.put("scale", new String [] { "2"} );
		parameters.put("max", new String [] { "2"} );
		parameters.put("min", new String [] { "2"} );
		ParamsBean pb = new ParamsBean(req);
		String url = pb.makeObjectUrl("root", "", false);
		Assert.assertTrue(url.contains("host=host"));
		Assert.assertTrue(url.contains("/root?"));
		Assert.assertTrue(url.contains("id=" + "".hashCode()));
		Assert.assertTrue(url.contains("scale=2"));
		Assert.assertTrue(url.contains("max=2"));
		Assert.assertTrue(url.contains("min=2"));
		Assert.assertFalse(url.contains("begin="));
		Assert.assertFalse(url.contains("end="));
	}
	@Test public void checkUrl2() {
		parameters.clear();
		parameters.put("max", new String [] { String.valueOf(Double.NaN)} );
		parameters.put("min", new String [] { String.valueOf(Double.NaN)} );
		ParamsBean pb = new ParamsBean(req);
		jrds.Filter f = new jrds.FilterHost("host");
		String url = pb.makeObjectUrl("root", f, true);
		Assert.assertTrue(url.contains("host=host"));
		Assert.assertTrue(url.contains("/root?"));
		//Assert.assertTrue(url.contains("id=" + f.hashCode()));
		Assert.assertTrue(url.contains("begin="));
		Assert.assertTrue(url.contains("end="));
		Assert.assertFalse(url.contains("scale="));
		Assert.assertFalse(url.contains("max="));
		Assert.assertFalse(url.contains("min="));
	}
	@Test public void checkUrl3() throws UnsupportedEncodingException {
		parameters.clear();
		jrds.Filter f = jrds.Filter.ALLHOSTS;
		String filterName = f.getName();
		parameters.put("filter", new String [] { filterName } );

		ParamsBean pb = new ParamsBean(req);
		String url = pb.makeObjectUrl("root", f, true);
		Assert.assertTrue(url.contains("filter=" + URLEncoder.encode(filterName, "UTF-8")));
		Assert.assertTrue(url.contains("/root?"));
		//Assert.assertTrue(url.contains("id=" + f.hashCode()));
		Assert.assertTrue(url.contains("begin="));
		Assert.assertTrue(url.contains("end="));
		Assert.assertFalse(url.contains("scale="));
	}
	@BeforeClass static public void configure() {
		JrdsTester.configure();
		Logger.getLogger(ParamsBean.class).setLevel(Level.TRACE);
	}

	@After public void tearDown() {
	}

}
