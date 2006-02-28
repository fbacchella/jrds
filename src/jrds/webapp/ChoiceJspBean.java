/*
 * Created on 10 déc. 2004
 *
 * TODO
 */
package jrds.webapp;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import jrds.HostsList;
import jrds.PropertiesManager;


/**
 * A back-end class associated with choice.jsp
 * @author Fabrice Bacchella
 */
public class ChoiceJspBean {
	static final public String ALLGROUPS = "tous";
	static final HostsList hl = HostsList.getRootGroup() ;
	static final PropertiesManager pm = PropertiesManager.getInstance();
	static final DateFormat df = new SimpleDateFormat("d/M/y");
	
	public ChoiceJspBean() {
		try {
			jbInit();
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * @param scale The scale to set.
	 */
	public  void setScale(String scale) {
		this.setScale(scale);
	}
	
	public String getNow() {
		return "\"" + df.format(new Date()) + "\"";
	}
	
	private void jbInit() throws Exception {
	}
	
}
