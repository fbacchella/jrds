/*##########################################################################
 _##
 _##  $Id$
 _##
 _##########################################################################*/

package jrds.webapp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;

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

	@SuppressWarnings("unchecked")
	public void getJavascriptTree(int sort, String father, JspWriter out, HttpServletRequest req, PeriodBean period) throws JspException {
		GraphTree graphTree = null;
		if(sort == GraphTree.LEAF_GRAPHTITLE )
			graphTree = HostsList.getRootGroup().getGraphTreeByHost();
		else if(sort == GraphTree.LEAF_HOSTNAME)
			graphTree = HostsList.getRootGroup().getGraphTreeByView();
		try {
			if(graphTree != null) {
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
				graphTree.getJavaScriptCode(out, parambuff.toString(), father);
			}
		} catch (IOException e) {
			throw new JspException(e.getMessage());
		}
	}
	
	public void getGraphList(JspWriter out, HttpServletRequest req, PeriodBean period) {
		String idS = req.getParameter("id");
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
				for(RdsGraph graph: node.enumerateChildsGraph()) {
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
