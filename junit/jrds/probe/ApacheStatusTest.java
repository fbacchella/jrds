package jrds.probe;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;

public class ApacheStatusTest extends ApacheStatusDetails {

	public ApacheStatusTest() throws MalformedURLException {
	}

	@Test
	public void parse() {
		Map<String, Number> values = new HashMap<String, Number>();

		parseScoreboard("_SRWKDCLGI.", values);

		for(WorkerStat w: WorkerStat.values()) {
			Assert.assertEquals(1, values.get(w.name()));
		}

		for(Map.Entry<String, Number> e: values.entrySet()) {
			WorkerStat w = WorkerStat.valueOf(e.getKey());
			Assert.assertEquals(e.getValue(), values.get(w.name()));
			Assert.assertEquals(1, e.getValue());

		}
	}
}
