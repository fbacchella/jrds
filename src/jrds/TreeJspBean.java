/*
 * Created on 10 dŽc. 2004
 *
 * TODO
 */
package jrds;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;

import org.apache.log4j.Logger;


/**
 * @author bacchell
 *
 * TODO
 */
public class TreeJspBean implements Serializable {
	static private final Logger logger = JrdsLogger.getLogger(TreeJspBean.class);
	private static final HostsList rootHostList = HostsList.getRootGroup();
	static final private DateFormat df = new SimpleDateFormat("d/M/y");
	static final private PropertiesManager pm = PropertiesManager.getInstance();
	private HostsList hostList = null;
	private Date end;
	private Date begin;
	int dateField = -1;
	String host;
	int sort = 1;

	public String getJavascriptTree() {
		calcDate();
		String retValue = "";
		GraphTreeNode graphTree = null;
		if(sort == GraphTreeNode.LEAF_GRAPHTITLE )
			graphTree = hostList.getGraphTreeByHost();
		else if(sort == GraphTreeNode.LEAF_HOSTNAME)
			graphTree = hostList.getGraphTreeByView();
		if(graphTree != null) {
			for(Iterator i = graphTree.valuesIterator() ; i.hasNext() ; ) {
				GraphTreeNode gt = (GraphTreeNode) i.next();
				retValue += gt.getJavaScriptCode(begin, end, sort);
			}
		}
		return retValue;
	}

	public String getJavascriptTree(int sort) {
		calcDate();
		String retValue = "";
		GraphTreeNode graphTree = null;
		if(sort == GraphTreeNode.LEAF_GRAPHTITLE )
			graphTree = hostList.getGraphTreeByHost();
		else if(sort == GraphTreeNode.LEAF_HOSTNAME)
			graphTree = hostList.getGraphTreeByView();
		if(graphTree != null) {
			for(Iterator i = graphTree.valuesIterator() ; i.hasNext() ; ) {
				GraphTreeNode gt = (GraphTreeNode) i.next();
				retValue += gt.getJavaScriptCode(begin, end, sort);
			}
		}
		return retValue;
	}

	/**
	 * @param scale The scale to set.
	 */
	public  void setScale(String scale) {
		int s = Integer.parseInt(scale);
		if(s == 1)
			dateField = Calendar.DATE;
		else if(s == 2)
			dateField = Calendar.WEEK_OF_YEAR;
		else if(s == 3)
			dateField = Calendar.MONTH;
		else if(s == 4)
			dateField = Calendar.YEAR;
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

	public void setGroup(String hostList)
	{
		if(ChoiceJspBean.ALLGROUPS.equals(hostList))
			this.hostList = rootHostList;
		else
			this.hostList = rootHostList.findGroup(hostList);
	}

	private void calcDate() {
		//begin.setTime(end.getTime());
		//begin.add(dateField, -1);
		//begin.setTimeInMillis()
		begin = new Date(0 - dateField);
	}

	public void setSort(String sort)
	{
		this.sort = Integer.parseInt(sort);
	}
}
