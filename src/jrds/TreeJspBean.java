/*
 * Created on 10 dŽc. 2004
 *
 * TODO
 */
package jrds;

import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.servlet.jsp.JspWriter;

import org.apache.log4j.Logger;


/**
 * @author bacchell
 *
 * TODO
 */
public class TreeJspBean implements Serializable {
	static private final Logger logger = Logger.getLogger(TreeJspBean.class);
	static final private DateFormat df = new SimpleDateFormat("y/M/d");
	static final private PropertiesManager pm = PropertiesManager.getInstance();
	private Date end;
	private Date begin;
	int dateField = -1;
	String host;
	int sort = 1;

	public void getJavascriptTree(int sort, String father, JspWriter out) {
		calcDate();
		String retValue = "";
		GraphTreeNode graphTree = null;
		if(sort == GraphTreeNode.LEAF_GRAPHTITLE )
			graphTree = HostsList.getRootGroup().getGraphTreeByHost();
		else if(sort == GraphTreeNode.LEAF_HOSTNAME)
			graphTree = HostsList.getRootGroup().getGraphTreeByView();
		try {
			if(graphTree != null) {
				graphTree.getJavaScriptCode(out, begin, end, father + "_0");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @param scale The scale to set.
	 */
	public  void setScale(String scale) {
		int s = Integer.parseInt(scale);
		dateField = 0 - s;
	}

	public void setDate(String date) {
		Calendar endCal = new GregorianCalendar();
		try {
			endCal.setTime(df.parse(date));
			endCal.set(Calendar.HOUR_OF_DAY,23);
			endCal.set(Calendar.MINUTE,59);
			endCal.set(Calendar.SECOND,59);
			if(endCal.getTimeInMillis() > System.currentTimeMillis())
				endCal.setTimeInMillis(-1);
		} catch (ParseException e) {
			endCal.setTimeInMillis(-1);
		}
		end = endCal.getTime();
	}

	private void calcDate() {
		begin = new Date(0 - dateField);
	}

	public void setSort(String sort)
	{
		this.sort = Integer.parseInt(sort);
	}
}
