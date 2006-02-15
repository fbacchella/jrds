/*
 * GetTreeTag.java
 *
 * Created on 15 février 2006, 22:54
 */

package jrds.webapp;

import javax.servlet.jsp.tagext.*;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.JspException;
import jrds.*;

/**
 *
 * @author  Fabrice Bacchella
 * @version
 */

public class GetTreeTag extends SimpleTagSupport {

    /**
     * Initialization of type property.
     */
    private int type;

    /**
     * Initialization of prefix property.
     */
    private java.lang.String prefix;
    
    /**Called by the container to invoke this tag.
     * The implementation of this method is provided by the tag library developer,
     * and handles all tag processing, body iteration, etc.
     */
    public void doTag() throws JspException {
        
        JspWriter out=getJspContext().getOut();
        
        try {
            // TODO: insert code to write html before writing the body content.
            // e.g.:
            //
            // out.println("<strong>" + attribute_1 + "</strong>");
            // out.println("    <blockquote>");
            
		calcDate();
		String retValue = "";
		GraphTreeNode graphTree = null;
		if(type == GraphTreeNode.LEAF_GRAPHTITLE )
			graphTree = HostsList.getRootGroup().getGraphTreeByHost();
		else if(type == GraphTreeNode.LEAF_HOSTNAME)
			graphTree = HostsList.getRootGroup().getGraphTreeByView();
		try {
			if(graphTree != null) {
				graphTree.getJavaScriptCode(out, begin, end, father + "_0");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
            JspFragment f=getJspBody();
            if (f != null) f.invoke(out);
            
            // TODO: insert code to write html after writing the body content.
            // e.g.:
            //
            // out.println("    </blockquote>");
            
        } catch (java.io.IOException ex) {
            throw new JspException(ex.getMessage());
        }
        
    }

    /**
     * Setter for the type attribute.
     */
    public void setType(int value) {
        this.type = value;
    }

    /**
     * Setter for the prefix attribute.
     */
    public void setPrefix(java.lang.String value) {
        this.prefix = value;
    }
}
