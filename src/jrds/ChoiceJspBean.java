/*
 * Created on 10 déc. 2004
 *
 * TODO 
 */
package jrds;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;


/**
 * A back-end class associated with choice.jsp
 * @author Fabrice Bacchella
 */
public class ChoiceJspBean {
	static final public String ALLGROUPS = "tous";
	static final private Logger logger = JrdsLogger.getLogger(ChoiceJspBean.class);
	static private HostsList hostList = null;
	static final PropertiesManager pm = PropertiesManager.getInstance();
	static final DateFormat df = new SimpleDateFormat("d/M/y");

	Scale scale = new Scale(Scale.SCALE_DAILY);

	public ChoiceJspBean() {
		if(hostList == null) {
			hostList = HostsList.getRootGroup();
		}
	}

	/**
	 * @return Returns the scale.
	 */
	public  String getScale() {
		return scale.getScaleName();
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
	
	public Collection getScaleList() {
		List scaleList = new ArrayList(Scale.allScale.length);
		int currScale = scale.getScale();
		for(int i = 0; i < Scale.allScale.length ; i++  ) {
			String selected = "";
			if(i == currScale )
				selected = " selected ";
			String val = "<OPTION" + selected + ">" + Scale.allScale[i] + "</OPTION>";
			scaleList.add(val);
		}
		return scaleList;
	}
	
	public Collection getGroupList() {
		List hList = new ArrayList(hostList.size());
		hList.add("<OPTION>" + ALLGROUPS + "</OPTION>");
		for(Iterator i = hostList.enumGroups().iterator() ; i.hasNext() ; ) {
			String group = (String) i.next();
			String val = "<OPTION>" + group + "</OPTION>";
			hList.add(val);
		}
		return hList;
	}

}
