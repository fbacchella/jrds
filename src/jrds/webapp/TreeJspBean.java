/*##########################################################################
 _##
 _##  $Id$
 _##
 _##########################################################################*/

package jrds.webapp;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
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
import jrds.Probe;
import jrds.RdsGraph;
import jrds.Renderer;

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
		if( referer == null 
				|| !(matchArgs(queryArgs.get("filter"), (String) req.getParameter("filter")))
				|| !(matchArgs(queryArgs.get("host"), (String) req.getParameter("host")))
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
		logger.trace("Comparing arg \"" + s1 + "\" with \"" + s2 + "\"");
		boolean retValue = false;
		//An empty string is like a null argument
		if(s1 != null && s1.trim().length() == 0)
			s1 = null;
		if(s2 != null && s2.trim().length() == 0)
			s2 = null;

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
		logger.trace("return  " + retValue);
		return retValue;
	}

	@SuppressWarnings("unchecked")
	public void getJavascriptTree(JspWriter out, ParamsBean params) throws JspException {

		HostsList root = HostsList.getRootGroup();

		Filter vf = params.getFilter();
		int oldId = params.getId();
		params.setId(0);
		try {
			if(vf != null) {
				int graphed = 0;
				for(GraphTree tree: root.getGraphsRoot()) {
					tree = vf.setRoot(tree);
					logger.trace("New tree root: " + tree);
					if(tree != null && ! tree.getJavaScriptCode(out, params, "tree" + graphed, vf)) {
						graphed++;
					}
				}
				if(graphed == 1)
					out.println("foldersTree = tree0;");
				else if(graphed > 1) {
					out.println("foldersTree = gFld(\"<i>Graph List</i>\")");
					out.print("foldersTree.addChildren([\n\ttree0");
					for(int i = 1; i < graphed ; i++)
						out.print(",\n\ttree" + i);
					out.println("\n]);");
				}
				if(graphed > 0)
					out.println("initializeDocument();");
			}
			else
				getAllFilterJavascript(out, params.toString(), root.getAllFiltersNames());
		} catch (IOException e) {
			throw new JspException(e.getMessage());
		}
		params.setId(oldId);
	}

	public static boolean getAllFilterJavascript(Writer out, String queryString, Collection<String> allFilters) throws IOException {
		out.append("foldersTree = gFld('All filters');\n");
		out.append("foldersTree.addChildren([\n");
		boolean first = true;
		for(String filterName: allFilters) {
			if( ! first) {
				out.append(",\n    [");
			}
			else {
				first = false;
				out.append("    [");
			}
			out.append("'" + filterName +"',");
			out.append("'");
			out.append("index.jsp?filter=");
			out.append(filterName);
			if(queryString != null &&  ! "".equals(queryString)) {
				out.append("&");
				out.append(queryString);
			}
			out.append("']");
		}
		out.append("\n]);\n");
		out.append("initializeDocument();");
		return true;
	}

	public void getGraphList(JspWriter out,  HttpServletRequest req, ParamsBean cgiParams) {
		HostsList  root = HostsList.getRootGroup();
		//cgiParams = new ParamsBean(req);
		logger.debug(cgiParams.getPeriodUrl());
		logger.debug(cgiParams.getFilter());
		logger.debug(cgiParams);
		Filter vf = cgiParams.getFilter();
		int id = cgiParams.getId();
		GraphBean gb = new GraphBean(req, cgiParams);
		HostsList hl = HostsList.getRootGroup();

		try {
			GraphTree node = null;
			node = root.getNodeById(id);
			List<RdsGraph> graphs = new ArrayList<RdsGraph>();
			if(node != null) {
				for(RdsGraph graph: node.enumerateChildsGraph(vf)) {
					graphs.add(graph);
				}
				if(cgiParams.isSorted()) {
					Collections.sort(graphs, new Comparator<RdsGraph>() {
						public int compare(RdsGraph g1, RdsGraph g2) {
							int order = String.CASE_INSENSITIVE_ORDER.compare(g1.getPngName(), g2.getPngName());
							if(order == 0)
								order = String.CASE_INSENSITIVE_ORDER.compare(g1.getProbe().getHost().getName(), g2.getProbe().getHost().getName());
							return order;
						}
					});
				}
			}
			else {
				RdsGraph graph = hl.getGraphById(id);
				if(graph != null)
					graphs.add(graph);
			}
			StringBuilder buffer = new StringBuilder();
			StringBuilder popupBuffer = new StringBuilder();
			StringBuilder detailsBuffer = new StringBuilder();
			StringBuilder historyBuffer = new StringBuilder();
			StringBuilder saveBuffer = new StringBuilder();

			if( ! graphs.isEmpty()) {
				Renderer r = hl.getRenderer();
				for(RdsGraph graph: graphs) {
					r.render(graph, cgiParams.getBegin(), cgiParams.getEnd());
					gb.setGraph(graph);

					popupBuffer.setLength(0);
					popupBuffer.append("onclick='popup(");
					popupBuffer.append(graph.hashCode());
					popupBuffer.append(")'");

					Probe p = graph.getProbe();
					detailsBuffer.setLength(0);
					detailsBuffer.append("onclick='details(");
					detailsBuffer.append(p.hashCode());
					detailsBuffer.append(", \"");
					detailsBuffer.append(p.getName());
					detailsBuffer.append("\")'");

					historyBuffer.setLength(0);
					historyBuffer.append("onclick='history_popup(");
					historyBuffer.append(graph.hashCode());
					historyBuffer.append(", \"");
					historyBuffer.append(p.getName());
					historyBuffer.append("\");'");

					saveBuffer.setLength(0);
					saveBuffer.append("onclick='save_popup(");
					saveBuffer.append(graph.hashCode());
					saveBuffer.append(");'");

					buffer.setLength(0);
					buffer.append("<div class='graphblock'>\n");
					buffer.append(gb.getImgElement() + "\n");
					buffer.append("<div class='iconslist'>\n");
					buffer.append("<img class='icon' " + popupBuffer + " src='img/application_double.png' alt='popup'  height='16' width='16'  />\n");
					buffer.append("<img class='icon' " + detailsBuffer + " src='img/application_view_list.png' alt=\"probe's details\"  height='16' width='16'  />\n");
					buffer.append("<img class='icon' " + historyBuffer + " src='img/time.png' alt='history'  height='16' width='16'  />\n");
					//buffer.append("<img class='icon' src='img/magnifier.png' alt='magnifier'  height='16' width='16'  />\n");
					buffer.append("<img class='icon' " + saveBuffer + " src='img/disk.png' alt='save'  height='16' width='16'  />\n");
					buffer.append("</div>\n");
					buffer.append("</div>\n");
					buffer.append("\n");
					out.println(buffer);
				}
			}
		} catch (IOException e) {
			logger.error("Result not written, connexion closed ?");
		}
	}

	public String getProbeUrl(HttpServletRequest req, ParamsBean cgiParams) {
		String retValue = req.getContextPath();
		RdsGraph g = HostsList.getRootGroup().getGraphById(cgiParams.getId());
		if(g != null) {
			StringBuffer urlBuffer = new StringBuffer();
			urlBuffer.append(req.getContextPath());
			Probe p = g.getProbe();
			urlBuffer.append("/details?id=" + p.hashCode());
			retValue = urlBuffer.toString();
		}
		return retValue;
	}	
}
