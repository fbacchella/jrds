/*##########################################################################
 _##
 _##  $Id: Period.java 217 2006-02-16 01:06:45 +0100 (jeu., 16 févr. 2006) fbacchella $
 _##
 _##########################################################################*/

package jrds.webapp;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import jrds.Filter;
import jrds.HostsList;
import jrds.Period;

/**
 * A bean to have a period with begin and end of type String
 *
 * @author Fabrice Bacchella
 * @version $Revision: 217 $ $Date$
 */
public class ParamsBean {
	static final private Logger logger = Logger.getLogger(ParamsBean.class);
	static private final DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
	Period p = new Period();
	int id = 0;
	boolean scalePeriod = true;
	final Map<String, String[]> parameters = new HashMap<String, String[]>();
	final HostsList root = HostsList.getRootGroup();

	public ParamsBean(){

	}

	public ParamsBean(HttpServletRequest req) {
		parseReq(req);
	}

	@SuppressWarnings("unchecked")
	public void parseReq(HttpServletRequest req) {
		parameters.putAll(req.getParameterMap());
		p = makePeriod(req);
		String paramString = req.getParameter("id");
		if(paramString != null) {
			try {
				id = Integer.parseInt(paramString);
			} catch (Throwable e) {
				logger.error("bad argument " + paramString);
			}
		}
		paramString = req.getParameter("pid");
		if(req.getParameter("end") == null)
			scalePeriod = true;
		parameters.remove("id");
		parameters.remove("scale");
		parameters.remove("begin");
		parameters.remove("end");
	}

	public String getParameter(String name) {
		String[] params = parameters.get(name);
		String retValue = null;
		if(params != null)
			retValue = params[0];
		return retValue;
	}

	public String[] getParameterValues(String name) {
		return parameters.get(name);
	}

	/**
	 * @return Returns the begin.
	 */
	public Date getBegin() {
		return p.getBegin();
	}

	/**
	 * @return Returns the end.
	 */
	public Date getEnd() {
		return p.getEnd();
	}

	/**
	 * @return Returns the begin.
	 */
	public String getStringBegin() {
		String formatted = "";
		if(p.getScale() == 0)
			formatted = df.format(p.getBegin());
		return formatted;
	}

	/**
	 * @return Returns the end.
	 */
	public String getStringEnd() {
		String formatted = "";
		if(p.getScale() == 0)
			formatted = df.format(p.getEnd());
		return formatted;
	}

	public List getPeriodNames() {
		return Period.getPeriodNames();
	}

	/**
	 * @return Returns the scale.
	 */
	public int getScale() {
		return p.getScale();
	}
	/**
	 * @param scale The scale to set.
	 */
	public void setScale(int scale) {
		p.setScale(scale);
	}

	public Filter getFilter() {
		String filterName = getParameter("filter");
		String hostFilter = getParameter("host");
		Filter vf = null;
		if(filterName != null && ! "".equals(filterName))
			vf = root.getFilter(filterName);
		else if(hostFilter != null && ! "".equals(hostFilter))
			vf = new jrds.FilterHost(hostFilter);

		return vf;

	}

	public String getPeriodUrl() {
		StringBuffer parambuff = new StringBuffer();
		parambuff.append("begin=" + p.getBegin().getTime() + "&end=" + p.getEnd().getTime());
		return parambuff.toString();
	}

	@Override
	public String toString() {
		StringBuffer parambuff = new StringBuffer();
		if(p != null) {
			if( scalePeriod || p.getScale() != 0)
				parambuff.append("scale=" + p.getScale()) ;
			else
				parambuff.append("begin=" + p.getBegin().getTime() + "&end=" + p.getEnd().getTime());
		}
		parambuff.append('&');
		if(id != 0)
			parambuff.append("id=" + id + '&');
		for(Map.Entry<String, String[]> param: parameters.entrySet()) {
			String key = param.getKey();
			for(int i=0; i< param.getValue().length; i++) {
				String value = param.getValue()[i];
				if(value != null && ! "".equals(value)) {
					parambuff.append(key);
					parambuff.append("=");
					parambuff.append(value);
					parambuff.append('&');
				}
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

	public static Period makePeriod(HttpServletRequest req) {
		Period p = null;
		String scale = req.getParameter("scale");
		String end = req.getParameter("end");
		int scaleVal = -1;
		if(scale != null && (scaleVal = Integer.parseInt(scale)) > 0)
			p = new Period(scaleVal);
		else if(end != null)
			p = new Period(req.getParameter("begin"), end);
		else
			p = new Period();
		return p;
	}

}
