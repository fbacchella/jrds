/*
 * GetTreeTag.java
 *
 * Created on 15 février 2006, 22:54
 */

package jrds.webapp;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.JspFragment;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import jrds.GraphTree;
import jrds.HostsList;

/**
 *
 * @author  Fabrice Bacchella
 * @version
 */

public class GetTreeTag extends SimpleTagSupport {
	
	int sort;
	String father;
	HttpServletRequest req;
	
	/**Called by the container to invoke this tag.
	 * The implementation of this method is provided by the tag library developer,
	 * and handles all tag processing, body iteration, etc.
	 */
	public void doTag() throws JspException {
		
		JspWriter out=getJspContext().getOut();
		
		try {
			GraphTree graphTree = null;
			if(sort == GraphTree.LEAF_GRAPHTITLE )
				graphTree = HostsList.getRootGroup().getGraphTreeByHost();
			else if(sort == GraphTree.LEAF_HOSTNAME)
				graphTree = HostsList.getRootGroup().getGraphTreeByView();
			if(graphTree != null) {
				graphTree.getJavaScriptCode(out, req.getQueryString(), father + "_0", null);
			}
			JspFragment f=getJspBody();
			if (f != null) f.invoke(out);
		} catch (java.io.IOException ex) {
			throw new JspException(ex.getMessage());
		}
		
	}
	
	public String getFather() {
		return father;
	}
	
	public void setFather(String father) {
		this.father = father;
	}
	
	public HttpServletRequest getReq() {
		return req;
	}
	
	public void setReq(HttpServletRequest req) {
		this.req = req;
	}
	
	public int getSort() {
		return sort;
	}
	
	public void setSort(int sort) {
		this.sort = sort;
	}
}
