package jrds.mockobjects;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import jrds.Probe;

public class MokeProbe extends Probe {

	@SuppressWarnings("unchecked")
	@Override
	public Map getNewSampleValues() {
		return new HashMap();
	}

	@Override
	public String getSourceType() {
		return "MokeSourceType";
	}
	@Override
	public String getName() {
		return "MokeProbe";
	}
	@Override
	public Date getLastUpdate() {
		return new Date();
	}

}
