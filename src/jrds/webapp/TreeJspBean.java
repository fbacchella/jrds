/*##########################################################################
 _##
 _##  $Id$
 _##
 _##########################################################################*/

package jrds.webapp;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;

import jrds.Filter;
import jrds.GraphTree;
import jrds.HostsList;
import jrds.RdsGraph;

import org.apache.log4j.Logger;

/**
 * 
 * @author Fabrice Bacchella
 * @version $Revision$ $Date$
 */
public class TreeJspBean {

	static final private Logger logger = Logger.getLogger(TreeJspBean.class);
	static final Pattern extractQuery = Pattern.compile("[\\?&]([^ &=]+)=([^ &\"]*)");


	public void ManageTree(JspWriter out, HttpServletRequest req, HttpServletResponse res) throws IOException {
		String referer = req.getHeader("Referer");
		final Map<String, String> queryArgs = new HashMap<String, String>(5);
		if(referer != null) {
			Matcher queryMatcher = extractQuery.matcher(referer);
			/*Construction of a query's map of the argument*/
			while(queryMatcher.find()) {
				String var = queryMatcher.group(1);
				String val = queryMatcher.group(2);
				queryArgs.put(var, val);
			}
		}
		if( referer == null || !(matchArgs(queryArgs.get("filter"), (String) req.getParameter("filter"))
		)
		) {
			Cookie c1 = new Cookie("clickedFolder", "");
			c1.setPath("/");
			c1.setMaxAge(0);
			res.addCookie(c1);
			Cookie c2 = new Cookie("highlightedTreeviewLink", "");
			c2.setPath("/");
			c2.setMaxAge(0);
			res.addCookie(c2);
		}
	}

	private boolean matchArgs(String s1, String s2) {
		boolean retValue = false;
		if(s1 == null && s2 == null)
			retValue = true;
		else if(s1 != null && s2 != null){
			try {
				String s1d = URLDecoder.decode(s1, "ISO-8859-1");
				String s2d = URLDecoder.decode(s2, "ISO-8859-1");
				retValue = s1d.equals(s2d);
			} catch (UnsupportedEncodingException e) {
			}
		}
		return retValue;
	}

	@SuppressWarnings("unchecked")
	public void getJavascriptTree(JspWriter out, HttpServletRequest req, PeriodBean period) throws JspException {
		String filterName = req.getParameter("filter");
		Filter vf = Filter.get(filterName);

		Map<String, String[]> parameters = new HashMap<String, String[]>();
		parameters.putAll(req.getParameterMap());
		parameters.remove("id");
		parameters.remove("scale");
		parameters.remove("begin");
		parameters.remove("end");
		StringBuffer parambuff = new StringBuffer();
		parambuff.append(period.toString());
		for(Map.Entry<String, String[]> param: parameters.entrySet()) {
			for(int i=0; i< param.getValue().length; i++) {
				parambuff.append("&");
				parambuff.append(param.getKey());
				parambuff.append("=");
				parambuff.append(param.getValue()[i]);
			}
		}

		try {
			if(vf != null) {
				HostsList  root = HostsList.getRootGroup();
				boolean noHostTree = root.getGraphTreeByHost().getJavaScriptCode(out, parambuff.toString(), "hostTree", vf);
				boolean noViewTree = root.getGraphTreeByView().getJavaScriptCode(out, parambuff.toString(), "viewTree", vf);
				if( ! (noHostTree || noViewTree))
					out.println("foldersTree = gFld(\"<i>Graph List</i>\");\nfoldersTree.addChildren([hostTree, viewTree]);");
				else if( noViewTree)
					out.println("foldersTree = hostTree;");
				else if( noHostTree)
					out.println("foldersTree = viewTree;");
				if( !(noHostTree && noViewTree) )
					out.println("initializeDocument();");
			}
			else
				Filter.getJavaScriptCode(out, parambuff.toString(), "", null);
		} catch (IOException e) {
			throw new JspException(e.getMessage());
		}
	}

	public void getGraphList(JspWriter out, HttpServletRequest req, PeriodBean period) {
		String idS = req.getParameter("id");
		String filterName = req.getParameter("filter");
		Filter vf = Filter.get(filterName);
		GraphTree node = null;
		int id = 0;
		if(idS != null) {
			try {
				id = Integer.parseInt(idS);
				node = HostsList.getRootGroup().getNodeById(id);
			} catch (Throwable e) {
				logger.error("bad argument " + idS);
			}
		}

		try {
			if(node != null) {
				for(RdsGraph graph: node.enumerateChildsGraph(vf)) {
					out.println(getImgUrl(graph, period, req));
				}
			}
			else {
				RdsGraph graph = HostsList.getRootGroup().getGraphById(id);
				if(graph != null)
					out.println(getImgUrl(graph, period, req));
			}
		} catch (IOException e) {
			logger.error("Result not written, connexion closed ?");
		}
	}

	private StringBuffer getImgUrl(RdsGraph graph, PeriodBean period, HttpServletRequest req) {
		StringBuffer imgElement = new StringBuffer();

		imgElement.append("<img class=\"graph\" ");
		//We build the Url
		StringBuffer urlBuffer = new StringBuffer();
		urlBuffer.append(req.getContextPath());
		urlBuffer.append("/graph?id=" + graph.hashCode());
		urlBuffer.append("&" + period);
		imgElement.append("src='" + urlBuffer + "' ");

		//A few more attributes
		imgElement.append("alt='" + graph.getQualifieName() + "' ");
		int rHeight = graph.getRealHeight();
		if(rHeight > 0)
			imgElement.append("height='" + Integer.toString(rHeight) + "' ");
		int rWidth = graph.getRealWidth();
		if(rWidth > 0)
			imgElement.append("width='" + Integer.toString(rWidth)+ "' ");

		imgElement.append(" />");

		return imgElement;

	}
}
