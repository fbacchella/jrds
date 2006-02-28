/*##########################################################################
 _##
 _##  $Id: Period.java 217 2006-02-16 01:06:45 +0100 (jeu., 16 févr. 2006) fbacchella $
 _##
 _##########################################################################*/

package jrds.webapp;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import jrds.Period;

/**
 * A bean to have a period with begin and end of type String
 *
 * @author Fabrice Bacchella
 * @version $Revision: 217 $ $Date$
 */
public class PeriodBean {
	static private final DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
	int scale = 7;
	Period p = null;
	
	/**
	 * @return Returns the begin.
	 */
	public String getBegin() {
		String retValue="";
		if(p != null)
			retValue = df.format(p.getBegin());
		return retValue;
	}
	/**
	 * @param begin The begin to set.
	 */
	public void setBegin(String begin) {
		if(begin != null) {
			if(p == null)
				p = new Period();
			p.setBegin(begin);
		}
	}
	/**
	 * @return Returns the end.
	 */
	public String getEnd() {
		String retValue="";
		if(p!= null)
			retValue = df.format(p.getEnd());
		return retValue;
	}
	/**
	 * @param end The end to set.
	 */
	public void setEnd(String end) {
		if(end != null) {
			if(p == null)
				p = new Period();
			p.setEnd(end);
		}
	}
	
	public List getPeriodNames() {
		return Period.getPeriodNames();
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
	public void setScale(int scale) {
		this.scale = scale;
	}
}
