/*##########################################################################
 _##
 _##  $Id: Period.java 217 2006-02-16 01:06:45 +0100 (jeu., 16 févr. 2006) fbacchella $
 _##
 _##########################################################################*/

package jrds.webapp;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import jrds.Filter;
import jrds.Graph;
import jrds.HostsList;
import jrds.Period;
import jrds.Probe;
import jrds.Util.SiPrefix;

import org.apache.log4j.Logger;

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

	String contextPath = "";
	Period period = new Period();
	int id = 0;
	int gid = 0;
	boolean sorted = false;
	boolean history = false;
	String maxArg = null;
	String minArg = null;
	Filter f = null;
	HostsList root;

	public ParamsBean(){

	}

	public ParamsBean(HttpServletRequest req, HostsList hl) {
		parseReq(req, hl);
	}

	public void parseReq(HttpServletRequest req, HostsList hl) {
		root =	hl;

		contextPath = req.getContextPath();
		period = makePeriod(req);
		logger.trace("period from parameters: " + period);
		gid = jrds.Util.parseStringNumber(req.getParameter("gid"), Integer.class, 0).intValue();
		id = jrds.Util.parseStringNumber(req.getParameter("id"), Integer.class, 0).intValue();

		String sortArg = req.getParameter("sort");
		if(sortArg != null && "true".equals(sortArg.toLowerCase()))
			sorted = true;
		String historyArg = req.getParameter("history");
		if("1".equals(historyArg))
			history = true;
		maxArg = req.getParameter("max");
		minArg = req.getParameter("min");
		String paramFilterName = req.getParameter("filter");
		String paramHostFilter = req.getParameter("host");
		if(paramFilterName != null && ! "".equals(paramFilterName)) {
			f = root.getFilter(paramFilterName);
		}
		else if(paramHostFilter != null && ! "".equals(paramHostFilter)) {
			f = new jrds.FilterHost(paramHostFilter);
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
		jrds.Graph g = root.getRenderer().getGraph(gid);
		if(g == null) {
			logger.warn("graph cache miss");
			jrds.GraphNode node = root.getGraphById(getId());
			g = node.getGraph();
			configureGraph(g);
		}
		return g;
	}

	public Probe<?,?> getProbe() {
		Probe<?,?> p = root.getProbeById(getId());
		if(p == null) {
			jrds.GraphNode node = root.getGraphById(getId());
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

	public String makeObjectUrl(String file, Object o, boolean timeAbsolute) {
		//We build the Url
		StringBuilder urlBuffer = new StringBuilder();
		urlBuffer.append(contextPath);

		if(! contextPath.endsWith("/")) {
			urlBuffer.append("/");
		}
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
		else {
			addFilterArgs(args);
			args.put("id", o.hashCode());
		}

		urlBuffer.append(file + "?");
		logger.trace("Params string: " + args);
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
	public int getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	private Period makePeriod(HttpServletRequest req) {
		Period p = null;
		try {
			String scaleStr = req.getParameter("scale");
			//Changed name for this attribute
			if(scaleStr == null)
				scaleStr = req.getParameter("autoperiod");

			int scale = jrds.Util.parseStringNumber(scaleStr, Integer.class, -1).intValue();

			String end = req.getParameter("end");
			String begin = req.getParameter("begin");
			if(scale > 0)
				p = new Period(scale);
			else if(end != null && begin !=null)
				p = new Period(begin, end);
			else
				p = new Period();
		} catch (NumberFormatException e) {
			logger.error("Period cannot be parsed :" + req.getQueryString());
		} catch (ParseException e) {
			logger.error("Period cannot be parsed :" + req.getQueryString());
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
}
