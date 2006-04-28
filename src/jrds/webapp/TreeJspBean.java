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

/**
 * 
 * @author Fabrice Bacchella
 * @version $Revision$ $Date$
 */
public class TreeJspBean {
	@SuppressWarnings("unchecked")
	public void getJavascriptTree(int sort, String father, JspWriter out, HttpServletRequest req) throws JspException {
		GraphTree graphTree = null;
		if(sort == GraphTree.LEAF_GRAPHTITLE )
			graphTree = HostsList.getRootGroup().getGraphTreeByHost();
		else if(sort == GraphTree.LEAF_HOSTNAME)
			graphTree = HostsList.getRootGroup().getGraphTreeByView();
		try {
			if(graphTree != null) {
				Map<String, String[]> parameters = new HashMap<String, String[]>();
				parameters.putAll(req.getParameterMap());
				if(parameters.containsKey("id"))
					parameters.remove("id");
				if( ! parameters.containsKey("scale") &&  ! parameters.containsKey("begin")) {
					String[] scales = new String[1];
					scales[0] = Integer.toString(new PeriodBean().getScale());
					parameters.put("scale", scales);
				}
				StringBuffer parambuff = new StringBuffer();
				for(Map.Entry<String, String[]> param: parameters.entrySet()) {
					parambuff.append("&");
					parambuff.append(param.getKey());
					parambuff.append("=");
					parambuff.append(param.getValue()[0]);
				}
				parambuff.deleteCharAt(0);
				graphTree.getJavaScriptCode(out, parambuff.toString(), father);
			}
		} catch (IOException e) {
			throw new JspException(e.getMessage());
			}
	}
}
