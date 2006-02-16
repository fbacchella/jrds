/*##########################################################################
 _##
 _##  $Id$
 _##
 _##########################################################################*/

package jrds;

import java.beans.PropertyChangeSupport;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

/**
 * This class manage a thread that runs in the background and is used to commit to disk the RRD datas modifications.
 *
 * @author Fabrice Bacchella
 * @version $Revision$
 */
public class Period {
	static final private Logger logger = Logger.getLogger(Period.class);
	static private final DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
	static private final Pattern datePattern = Pattern.compile("\\d\\d\\d\\d-\\d\\d-\\d\\d");
	private Date begin = null;
	private Date end = null;
	private int calPeriod;
	
	private PropertyChangeSupport propertySupport;
	
	public Period() {
	}
	
	public Period(String begin, String end) {
		setBegin(begin);
		setEnd(end);
	}
	
	/**
	 * @return Returns the begin.
	 */
	public Date getBegin() {
		if(begin == null && end != null) {
			Calendar cbegin = new GregorianCalendar();
			cbegin.setTime(end);
			cbegin.add(convertScale(calPeriod), -1);
			begin.setTime(cbegin.getTimeInMillis());
		}
		return begin;
	}
	/**
	 * @param begin The begin to set.
	 */
	public void setBegin(String begin) {
		this.begin = string2Date(begin);
	}

	public void setBegin(Date begin) {
		this.begin = begin;
	}
	/**
	 * @return Returns the end.
	 */
	public Date getEnd() {
		return end;
	}
	/**
	 * @param end The end to set.
	 */
	public void setEnd(String end) {
		this.end = string2Date(end);
	}
	public void setEnd(Date end) {
		this.end = end;
	}

	/**
	 * Calculate date from string parametrs comming from the URL
	 *
	 * @param sbegin String
	 * @param send String
	 * @param begin The calculated begin date
	 * @param end The calculated end date
	 */
	private Date string2Date(String date){
		Date foundDate = null;
		if("NOW".compareToIgnoreCase(date) == 0) {
			foundDate = new Date();
		}
		else if(datePattern.matcher(date).matches()) {
			try {
				foundDate = df.parse(date);
			} catch (ParseException e) {
				logger.error("Illegal date argument: " + e);
			}
			if(foundDate == null) {
				try {
					long value = Long.parseLong(date);
					if(value == 0)
						foundDate = new Date();
					else if(value > 0)
						foundDate = new Date(value);
				}
				catch (NumberFormatException ex) {}
			}
		}
		return foundDate;
	}
	
	/**
	 * Convert a int to a calendar field
	 * @param scale
	 * @return
	 */
	static private int convertScale(long scale) {
		//Failsafe, the whole year
		int retValue = Calendar.YEAR;
		if(scale == -1)
			retValue = Calendar.DAY_OF_MONTH;
		else if(scale == -2)
			retValue = Calendar.WEEK_OF_YEAR;
		else if(scale == -3)
			retValue = Calendar.MONTH;
		else if(scale == -4)
			retValue = Calendar.YEAR;
		return retValue;
	}
	
}
