/*
 * Created on 10 déc. 2004
 *
 * TODO 
 */
package jrds;

import java.util.Calendar;

import org.apache.log4j.Logger;


/**
 * @author bacchell
 *
 * TODO 
 */
public class Scale {
	public static final String SCALE_DAILY = "Quotidienne";
	public static final String SCALE_WEEKLY = "Hebdomadaire";
	public static final String SCALE_MONTHLY = "Mensuelle";
	public static final String SCALE_YEARLY = "Annuelle";
	static Logger logger = JrdsLogger.getLogger(Scale.class.getPackage().getName());
	int scale = 0;
	public static String[] allScale;
	static {
		allScale = new String [4];
		allScale[0] = SCALE_DAILY;
		allScale[1] = SCALE_WEEKLY;
		allScale[2] = SCALE_MONTHLY;
		allScale[3] = SCALE_YEARLY;	
	}

	/**
	 * @param scale
	 */
	public Scale(int scale) {
		this.scale = scale;
	}
	
	/**
	 * @param scale
	 */
	public Scale(String scale) {
		setScale(scale);
	}
	
	/**
	 * @return Returns the scale.
	 */
	public String getScaleName() {
		return allScale[scale];
	}

	/**
	 * @return Returns the scale.
	 */
	public int getScale() {
		return scale;
	}

	/**
	 * @param scale The scale to set.
	 */
	public  void setScale(String scale) {
		for(int i = 0; i < allScale.length; i++) {
			if(allScale[i].equals(scale)) {
				this.scale = i;
				break;
			}
		}
	}

	/**
	 * @param scale The scale to set.
	 */
	public  void setScale(int scale) {
		this.scale = scale;
	}
	
	public int getFiedl() {
		int retValue = -1;
		if(scale == 0)
			retValue = Calendar.DATE;
		else if(scale == 1)
			retValue = Calendar.WEEK_OF_YEAR;
		else if(scale == 2)
			retValue = Calendar.MONTH;
		else if(scale == 3)
			retValue = Calendar.YEAR;
		return retValue;
	}
}
