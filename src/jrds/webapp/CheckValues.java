/*##########################################################################
 _##
 _##  $Id: Graph.java 236 2006-03-02 15:59:34 +0100 (jeu., 02 mars 2006) fbacchella $
 _##
 _##########################################################################*/

package jrds.webapp;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jrds.HostsList;
import jrds.Probe;

import org.rrd4j.ConsolFun;
import org.rrd4j.core.FetchData;

/**
 * A servlet wich return datastore values from a probe.
 * It can be used in many way :
 * The simplest way is by using a URL of the form :
 * http://<it>server</it>/values/<it>host</it>/<it>probe.</it>
 * It will return all datastores values for this probe. By adding a /<it>datastore</i>, one can choose only 
 * one data store.<p>
 * It's possible to refine the query with some arguments, using a post or a get.<p>
 * The argument can be:
 * <ul>
 * <li>host</li>
 * <li>probe</li>
 * <li>period: the time interval in seconds, default to the step value.</li>
 * <li>cf: the consolidated function used.</li>
 * <li>dsName: the datastore name</li>
 * <li>rpn: a rpn used to generate the value.</li>
 * </ul>
 * If there is only one value generated, it's displayed as is. Else the name is also shown as well as the last update value
 * in the form <code>datastore: value</code>
 * @author Fabrice Bacchella
 * @version $Revision: 236 $
 */
public final class CheckValues extends JrdsServlet {
	private static enum ParametersNames {
		HOST,
		RRD,
		PROBE,
		PERIOD,
		DSNAME,
		CF,
		RPN
	};

	private class ParamMap extends HashMap<String, String[]> {
		public ParamMap() {
			super();
		}
		public ParamMap(int initialCapacity) {
			super(initialCapacity);
		}
		public String[] get(ParametersNames name) {
			return get(name.toString().toUpperCase());
		}
		public String getFirst(ParametersNames name) {
			return get(name.toString().toUpperCase())[0];
		}
		public void put(ParametersNames name, String value) {
			put(name.toString().toUpperCase(), new String[] {value});
		}
		public void put(ParametersNames name, String[] value) {
			put(name.toString().toUpperCase(), value);
		}
		public boolean containsKey(ParametersNames key) {
			return containsKey(key.toString().toUpperCase());
		}
		@Override
		public void putAll(Map<? extends String, ? extends String[]> m) {
			for(Map.Entry<? extends String, ? extends String[]> e: m.entrySet()) {
				put(e.getKey().toUpperCase(), e.getValue());
			}
		}
	}

	/**
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	public void doGet(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException {
		doWork(req, res);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException {
		doWork(req, res);
	}
	
	private void doWork(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException {
		HostsList hl = getHostsList();

		ParamMap params = new ParamMap();
		params.put(ParametersNames.PERIOD, Integer.toString(hl.getStep()));
		params.put(ParametersNames.CF, ConsolFun.AVERAGE.toString());

		Map<String,String[]> reqParams =  extracted(req);
		params.putAll(reqParams);
		params.putAll(parseUrl(req));
		if(! params.containsKey(ParametersNames.HOST) || ! params.containsKey(ParametersNames.PROBE))
			return;

		String host = params.getFirst(ParametersNames.HOST);
		String probe = params.getFirst(ParametersNames.PROBE);
		int id = (host + "/" + probe).hashCode();
		int period = jrds.Util.parseStringNumber(params.getFirst(ParametersNames.PERIOD), Integer.class, hl.getStep()).intValue();
		ConsolFun cf = ConsolFun.valueOf(params.getFirst(ParametersNames.CF));
		Probe<?,?> p = hl.getProbeById(id);

		if(p != null) {
			Date lastupdate = p.getLastUpdate();
			//It the last update is too old, it fails
			if((new Date().getTime() - lastupdate.getTime()) > (p.getStep() * 1000)) {
				res.setContentType("text/plain");
				res.addHeader("Cache-Control", "no-cache");
				ServletOutputStream out = res.getOutputStream();
				out.println("Probe too old: " + (new Date().getTime() - lastupdate.getTime()));
				return;
			}
			Date paste = new Date(lastupdate.getTime() - period * 1000);
			FetchData fd = p.fetchData(paste, lastupdate);
		
			res.setContentType("text/plain");
			res.addHeader("Cache-Control", "no-cache");
			ServletOutputStream out = res.getOutputStream();

			//If no data to fetch specified, we get all of them
			if(! params.containsKey(ParametersNames.DSNAME) && ! params.containsKey(ParametersNames.RPN)) {
				params.put(ParametersNames.DSNAME, fd.getDsNames());
			}
			Map<String, Double> displayedValues = new HashMap<String, Double>();
			if(params.containsKey(ParametersNames.DSNAME)) {
				for(String dsName: params.get(ParametersNames.DSNAME)) {
					Double val = fd.getAggregate(dsName, cf);
					displayedValues.put(dsName, val);
				}
			}
			if(params.containsKey(ParametersNames.RPN)) {
				for(String rpn: params.get(ParametersNames.RPN)) {
					Double val = fd.getRpnAggregate(rpn, cf);
					displayedValues.put(rpn, val);
				}
			}
			if(displayedValues.size() == 1) {
				out.print(displayedValues.values().toArray()[0].toString());
			}
			else {
				for(Map.Entry<String, Double> e: displayedValues.entrySet()) {
					out.println(e.getKey() + ": " + e.getValue());
				}
				out.println("Last update: " + p.getLastUpdate());
				out.println("Last update age (ms): " + (new Date().getTime() - p.getLastUpdate().getTime()));
			}
		}
		
	}

	@SuppressWarnings("unchecked")
	private Map<String, String[]> extracted(HttpServletRequest req) {
		return (Map<String,String[]>)req.getParameterMap();
	}
	
	private Map<String, String[]> parseUrl(HttpServletRequest req) {
		String probeInfo = req.getPathInfo();
		if (probeInfo == null)
			return Collections.emptyMap();
		String[] path = probeInfo.trim().split("/");
		if(path.length < 3)
			return Collections.emptyMap();
		ParamMap retValues= new ParamMap(3);
		retValues.put(ParametersNames.HOST, path[1]);
		retValues.put(ParametersNames.PROBE, path[2]);
		if(path.length >= 4) {
			retValues.put(ParametersNames.DSNAME, path[3]);
		}
		return retValues;
	}

}
