/*##########################################################################
 _##
 _##  $Id: Period.java 217 2006-02-16 01:06:45 +0100 (jeu., 16 févr. 2006) fbacchella $
 _##
 _##########################################################################*/

package jrds.webapp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.servlet.http.HttpServletRequest;

import jrds.Base64;
import jrds.Filter;
import jrds.Graph;
import jrds.GraphTree;
import jrds.HostsList;
import jrds.Period;
import jrds.Probe;
import jrds.Tab;
import jrds.Util;
import jrds.Util.SiPrefix;

import org.apache.log4j.Logger;
import org.json.JSONException;

/**
 * A bean to have a period with begin and end of type String
 *
 * @author Fabrice Bacchella
 * @version $Revision: 217 $ $Date$
 */
public class ParamsBean implements Serializable {

	static final private Logger logger = Logger.getLogger(ParamsBean.class);

	static private final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	static private final Pattern rangePattern = Pattern.compile("(-?\\d+(.\\d+)?)([a-zA-Z]{0,2})");

	static public String DEFAULTTAB = "filtertab";

	String contextPath = "";
	String dsName = null;
	Period period = new Period();
	Integer id = null;
	Integer gid = null;
	Integer pid = null;
	boolean sorted = false;
	boolean history = false;
	String maxArg = null;
	String minArg = null;
	Filter f = null;
	private HostsList hostlist;
	String user = null;
	Set<String> roles = Collections.emptySet();
	private Map<String, String[]> params  = Collections.emptyMap();
	private GraphTree tree = null;
	private Tab tab = null;
	private String choiceType = null;
	private String choiceValue = null;

	public ParamsBean(){

	}

	public ParamsBean(HttpServletRequest req, HostsList hl) {
		contextPath = req.getContextPath();
		params =  getReqParamsMap(req);
		readAuthorization(req, hl);
		parseReq(hl);
	}

	public ParamsBean(HttpServletRequest req, HostsList hl, String... restPath ) {
		contextPath = req.getContextPath();
		String probeInfo = req.getPathInfo();
		if (probeInfo != null) {
			params = new HashMap<String, String[]>(restPath.length);
			String[] path = probeInfo.trim().split("/");
			logger.trace(jrds.Util.delayedFormatString("mapping %s to %s", Arrays.asList(path), Arrays.asList(restPath)));
			int elem = Math.min(path.length -1 , restPath.length);
			for(int i=0; i < elem; i++) {
				String value = path[i + 1];
				String key = restPath[i].toLowerCase();
				params.put(key, new String[] {value});
			}
			params.putAll(getReqParamsMap(req));
		}
		else {
			params =  getReqParamsMap(req);
		}
		if(logger.isTraceEnabled()) {
			logger.trace("params map:");
			for(String key: params.keySet()) {
				String[] value = params.get(key);
				if(value != null)
					logger.trace(key + ": " + Arrays.asList(value));
			}
		}
		readAuthorization(req, hl);
		parseReq(hl);
	}

	@SuppressWarnings("unchecked")
	//Not a really useful method, just to reduce warning
	private Map<String, String[]> getReqParamsMap(HttpServletRequest req) {
		logger.trace(jrds.Util.delayedFormatString("Parameter map for %s: %s", req, req.getParameterMap()));
		return new HashMap<String, String[]>(req.getParameterMap());
	}

	public String getValue(String key) {
		String[] values = params.get(key);
		if(values != null && values.length > 0)
			return values[0];
		return null;
	}

	/**
	 * Set the authentication context, witout touching the requests parameters
	 * @param req
	 * @param hl
	 */
	public void readAuthorization(HttpServletRequest req, HostsList hl) {
		user = req.getRemoteUser();
		if(user != null) {
			roles = new HashSet<String>();
			for(String role: hl.getRoles()) {
				if(req.isUserInRole(role))
					roles.add(role);
			}
		}
		logger.trace("Found user "  + user + " with roles " + roles);		
	}

	private void unpack(String packed) {
		String formatedpack;
		formatedpack = JSonPack.GZIPHEADER +  packed.replace('!', '=').replace('$', '/').replace('*', '+');
		logger.trace(formatedpack);
		ByteArrayOutputStream outbuffer = new ByteArrayOutputStream(formatedpack.length());
		ByteArrayInputStream inbuffer = new ByteArrayInputStream(Base64.decode(formatedpack));
		try {
			byte[] copybuffer = new byte[1500];
			GZIPInputStream os = new GZIPInputStream(inbuffer);
			int realread = os.read(copybuffer);
			while(realread > 0) {
				outbuffer.write(copybuffer, 0, realread );
				realread = os.read(copybuffer);
			}
			JrdsJSONObject json = new JrdsJSONObject(outbuffer.toString());
			for(String key: json) {
				Object value = json.get(key);
				String newkey = JSonPack.JSONKEYS.get(new Integer(key));
				params.put(newkey, new String[] {value.toString()});
				logger.trace(jrds.Util.delayedFormatString("adding %s = %s", newkey, value));
			}
		} catch (IOException e) {
			logger.error("IOException " + e, e);
		} catch (JSONException e) {
			logger.error("JSON parsing exception " + e);
		}
		logger.trace("Params unpacked: " + params);
	}

	private void parseReq(HostsList hl) {
		hostlist =	hl;

		String packed = getValue("p");
		if(packed != null && ! "".equals(packed))
			unpack(packed);

		period = makePeriod();

		String host = getValue("host");
		String probe = getValue("probe");

		gid = jrds.Util.parseStringNumber(getValue("gid"), Integer.class, null);
		//Many way to discover id (graph node id)
		String idStr = getValue("id");
		String graph =  getValue("graphname");
		if(idStr != null && ! "".equals(idStr))
			id = Util.parseStringNumber(idStr, Integer.class, null);
		else if(host != null && ! "".equals(host) && graph != null && ! "".equals(graph)) {
			id = (host + "/" + graph).hashCode();
		}

		String pidStr = getValue("pid");
		if(pidStr != null)
			pid = jrds.Util.parseStringNumber(pidStr, Integer.class, null);
		else if(host != null && ! "".equals(host) && probe != null && ! "".equals(probe) )
			pid = (host + "/" + probe).hashCode();

		dsName =  getValue("dsName");
		if("".equals(dsName))
			dsName = null;

		String sortArg = getValue("sort");
		if(sortArg != null && "true".equals(sortArg.toLowerCase()))
			sorted = true;
		String historyArg = getValue("history");
		if("1".equals(historyArg))
			history = true;

		//max and min should only go together
		//it's up to the gui (aka js code) to manage default value
		String minStr = getValue("min");
		String maxStr = getValue("max");
		if(minStr != null && maxStr != null && ! "".equals(minStr) && ! "".equals(maxStr)) {
			maxArg = maxStr;
			minArg = minStr;
		}
		String paramFilterName = getValue("filter");
		String paramHostFilter = getValue("host");
		String treeName = getValue("tree");
		String tabName = getValue("tab");
		if(paramFilterName != null && ! "".equals(paramFilterName)) {
			f = hostlist.getFilter(paramFilterName);
			if(f != null) {
				choiceType = "filter";
				choiceValue = paramFilterName;
			}
		}
		else if(paramHostFilter != null && ! "".equals(paramHostFilter)) {
			tree = hl.getGraphTreeByHost().getByPath(GraphTree.HOSTROOT, paramHostFilter);
			if(tree != null) {
				choiceType = "host";
				choiceValue = paramHostFilter;
			}
			//f = new jrds.FilterHost(paramHostFilter);
		}
		else if(treeName != null && ! "".equals(treeName)) {
			tree = hl.getGraphTree(treeName);
			if(tree != null) {
				choiceType = "tree";
				choiceValue = treeName;
			}
		}
		else if(tabName != null && ! "".equals(tabName)) {
			tab = hl.getTab(tabName);
			if(tab != null) {
				choiceType = "tab";
				choiceValue = tabName;
			}
		}
		
		//If previous steps failed
		if(choiceType == null || choiceValue == null) {
			tab = hl.getTab(DEFAULTTAB);
			choiceType = "tab";
			choiceValue = DEFAULTTAB;
		}
	}

	private double parseRangeArg(String rangeArg, Graph g){
		if(rangeArg == null)
			return Double.NaN;
		Matcher  m = rangePattern.matcher(rangeArg);
		jrds.GraphNode node = g.getNode();
		if(m.matches() && node != null) {
			String valueString = m.group(1);
			Number value = jrds.Util.parseStringNumber(valueString, Double.class, Double.NaN);
			String suffixString = m.group(3);
			if(! "".equals(suffixString)) {
				try {
					SiPrefix suffix = SiPrefix.valueOf(suffixString);
					return suffix.evaluate(value.doubleValue(), node.getGraphDesc().isSiUnit());
				} catch (java.lang.IllegalArgumentException e) {
					logger.info("Illegal SI suffix " + suffixString);
				}
			}
			else
				return value.doubleValue();
		}
		return Double.NaN;
	}

	public Filter getFilter() {
		return f;
	}

	public void configureGraph(jrds.Graph g) {
		g.setPeriod(period);
		double max = parseRangeArg(maxArg, g);
		double min = parseRangeArg(minArg, g);
		if(! Double.isNaN(max))
			g.setMax(max);
		if(! Double.isNaN(min))
			g.setMin(min);
	}

	public jrds.Graph getGraph() {
		jrds.Graph g = hostlist.getRenderer().getGraph(gid);
		if(g == null) {
			logger.warn("graph cache miss");
			jrds.GraphNode node = hostlist.getGraphById(getId());
			g = node.getGraph();
			configureGraph(g);
		}
		return g;
	}

	public Probe<?,?> getProbe() {
		Probe<?,?> p = hostlist.getProbeById(pid);
		if(p == null) {
			jrds.GraphNode node = hostlist.getGraphById(pid);
			if(node != null)
				p = node.getProbe();
		}
		return p;
	}

	private void addPeriodArgs(Map<String, Object> args, boolean timeAbsolute) {
		if(! timeAbsolute && period.getScale() != 0) {
			args.put("scale",period.getScale());
		}
		else {
			args.put("begin", period.getBegin().getTime());
			args.put("end", period.getEnd().getTime());
		}		
	}

	private void addMinMaxArgs(Map<String, Object> args) {
		if( maxArg != null)
			args.put("max", maxArg);
		if(minArg !=null)
			args.put("min", minArg);
	}

	private void addFilterArgs(Map<String, Object> args) {
		if(f instanceof jrds.FilterHost) {
			args.put("host", f.getName());
		}
		else if (f instanceof jrds.Filter){
			args.put("filter", f.getName());

		}
	}
	
	public Map<String, Object> doArgsMap(Object o, boolean timeAbsolute) {
		Map<String, Object> args = new HashMap<String, Object>();
		addPeriodArgs(args, timeAbsolute);
		addMinMaxArgs(args);
		if(o instanceof jrds.FilterHost) {
			args.put("host", ((jrds.Filter)o).getName());
		}
		else if(o instanceof jrds.Filter) {
			args.put("filter", ((jrds.Filter)o).getName());
		}
		else if(o instanceof jrds.Graph) {
			args.put("gid", o.hashCode());
			args.put("id", ((jrds.Graph)o).getNode().hashCode());	
		}
		else if(o instanceof jrds.Probe<?,?>){
			args.put("pid", o.hashCode());
		}
		else {
			addFilterArgs(args);
			args.put("id", o.hashCode());
		}
		return args;
	}

	public String makeObjectUrl(String file, Object o, boolean timeAbsolute) {
		Map<String, Object> args = doArgsMap(o, timeAbsolute);
		logger.trace(jrds.Util.delayedFormatString("Params string:%s ", args));
		//We build the Url
		StringBuilder urlBuffer = new StringBuilder();
		urlBuffer.append(contextPath);

		if(! contextPath.endsWith("/")) {
			urlBuffer.append("/");
		}
		urlBuffer.append(file + "?");

		for(Map.Entry<String, Object>e: args.entrySet()) {
			try {
				urlBuffer.append(e.getKey() + "="+ URLEncoder.encode(e.getValue().toString(), "UTF-8") + "&");
			} catch (UnsupportedEncodingException e1) {
			}
		}
		urlBuffer.deleteCharAt(urlBuffer.length() - 1);
		return urlBuffer.toString();
	}

	@Override
	public String toString() {
		StringBuilder parambuff = new StringBuilder();

		Map<String, Object> args = new HashMap<String, Object>();
		addFilterArgs(args);
		addPeriodArgs(args, true);
		addMinMaxArgs(args);

		parambuff.append('&');
		if(id != 0)
			parambuff.append("id=" + id + '&');
		if(gid != 0)
			parambuff.append("gid=" + gid + '&');
		for(Map.Entry<String, Object> param: args.entrySet()) {
			String key = param.getKey();
			Object value = param.getValue();
			if(value != null && ! "".equals(value)) {
				parambuff.append(key);
				parambuff.append("=");
				parambuff.append(value);
				parambuff.append('&');
			}
			else if(value == null) {
				parambuff.append(key);
				parambuff.append('&');
			}
		}

		//Remove the extra &
		parambuff.deleteCharAt(parambuff.length() - 1);

		return parambuff.toString();
	}
	/**
	 * @return the id
	 */
	public Integer getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	private Period makePeriod() {
		Period p = null;
		try {
			String scaleStr = getValue("scale");
			//Changed name for this attribute
			if(scaleStr == null)
				scaleStr = getValue("autoperiod");

			int scale = jrds.Util.parseStringNumber(scaleStr, Integer.class, -1).intValue();

			String end = getValue("end");
			String begin = getValue("begin");
			if(scale > 0)
				p = new Period(scale);
			else if(end != null && begin !=null)
				p = new Period(begin, end);
			else
				p = new Period();
		} catch (NumberFormatException e) {
			logger.error("Period cannot be parsed");
		} catch (ParseException e) {
			logger.error("Period cannot be parsed");
		}
		return p;
	}

	/**
	 * @return the sort
	 */
	public boolean isSorted() {
		return sorted;
	}

	/**
	 * @return the p
	 */
	public Period getPeriod() {
		if(period.getScale() != 0)
			period = new Period(period.getScale());
		return period;
	}

	/**
	 * @return Returns the begin.
	 */
	public String getStringBegin() {
		String formatted = "";
		if(period.getScale() == 0)
			formatted = df.format(period.getBegin());
		return formatted;
	}

	/**
	 * @return Returns the end.
	 */
	public String getStringEnd() {
		String formatted = "";
		if(period.getScale() == 0)
			formatted = df.format(period.getEnd());
		return formatted;
	}

	/**
	 * @return Returns the begin.
	 */
	public long getBegin() {
		if(period != null)
			return period.getBegin().getTime();
		return Long.MIN_VALUE;
	}

	/**
	 * @return Returns the end.
	 */
	public long getEnd() {
		if(period != null)
			return period.getEnd().getTime();
		return Long.MIN_VALUE;
	}

	/**
	 * @return Returns the scale.
	 */
	public int getScale() {
		return period.getScale();
	}

	public void setScale(int s) {
		period = new Period(s);
	}

	public List<String> getPeriodNames() {
		logger.trace("Knonw period names: " + Period.getPeriodNames());
		return Period.getPeriodNames();
	}

	public String getPeriodUrl() {
		StringBuilder parambuff = new StringBuilder();
		if(period != null)
			parambuff.append("begin=" + period.getBegin().getTime() + "&end=" + period.getEnd().getTime());
		return parambuff.toString();
	}

	public String getMaxStr() {
		return maxArg;
	}

	public String getMinStr() {
		return minArg;
	}

	/**
	 * @return the history
	 */
	public boolean isHistory() {
		return history;
	}

	public Integer getPid() {
		return pid;
	}

	public String getDsName() {
		return dsName;
	}

	/**
	 * @return the user
	 */
	public String getUser() {
		return user;
	}

	/**
	 * @return the roles
	 */
	public Set<String> getRoles() {
		return roles;
	}

	/**
	 * @return the tree
	 */
	public GraphTree getTree() {
		return tree;
	}

	/**
	 * @return the choiceType
	 */
	public String getChoiceType() {
		return choiceType;
	}

	/**
	 * @return the choiceValue
	 */
	public String getChoiceValue() {
		return choiceValue;
	}

	/**
	 * @return the tab
	 */
	public Tab getTab() {
		return tab;
	}
}
