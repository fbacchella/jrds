/*##########################################################################
 _##
 _##  $Id$
 _##
 _##########################################################################*/

package jrds;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
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
	static private final DateFormat df = new SimpleDateFormat("yyyy-MM-dd:HH:mm");
	static private final Pattern datePattern = Pattern.compile("\\d\\d\\d\\d-\\d\\d-\\d\\d");
	private Date begin = null;
	private Date end = null;
	private int calPeriod = 7;
	private static class PeriodItem {
		String name;
		int length;
		int unit;
		int number;
		PeriodItem(String name, int unit, int number) {
			this.name = name;
			this.unit = unit;
			this.number = number;
		}
		Date getBegin(Date end) {
			Calendar calBegin = Calendar.getInstance();
			calBegin.setTime(end);
			calBegin.add(unit, number);
			return calBegin.getTime();
		}
	}
	static final private List periodList = new ArrayList(18);
	static {
		periodList.add(new Period.PeriodItem("Manual", Calendar.HOUR, -1));
		periodList.add(new Period.PeriodItem("Last Hour", Calendar.HOUR, -1));
		periodList.add(new Period.PeriodItem("Last 2 Hours", Calendar.HOUR, -2));
		periodList.add(new Period.PeriodItem("Last 3 Hours", Calendar.HOUR, -3));
		periodList.add(new Period.PeriodItem("Last 4 Hours", Calendar.HOUR, -4));
		periodList.add(new Period.PeriodItem("Last 6 Hours", Calendar.HOUR, -6));
		periodList.add(new Period.PeriodItem("Last 12 Hours", Calendar.HOUR, -12));
		periodList.add(new Period.PeriodItem("Last Day", Calendar.DAY_OF_MONTH, -1));
		periodList.add(new Period.PeriodItem("Last 2 Days", Calendar.DAY_OF_MONTH, -2));
		periodList.add(new Period.PeriodItem("Last Week", Calendar.WEEK_OF_MONTH, -1));
		periodList.add(new Period.PeriodItem("Last 2 Weeks", Calendar.WEEK_OF_MONTH, -1));
		periodList.add(new Period.PeriodItem("Last Month", Calendar.MONTH, -1));
		periodList.add(new Period.PeriodItem("Last 2 Months", Calendar.MONTH, -2));
		periodList.add(new Period.PeriodItem("Last 3 Months", Calendar.MONTH, -3));
		periodList.add(new Period.PeriodItem("Last 4 Months", Calendar.MONTH, -4));
		periodList.add(new Period.PeriodItem("Last 6 Months", Calendar.MONTH, -6));
		periodList.add(new Period.PeriodItem("Last Year", Calendar.YEAR, -1));
		periodList.add(new Period.PeriodItem("Last 2 Years", Calendar.YEAR, -2));
	}
	
	public Period() {
	}
	
	public Period(int p) {
		calPeriod = p;
		end = new Date();
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
			if(calPeriod > periodList.size()) {
				logger.info("Period invalid: " + calPeriod);
				calPeriod = periodList.size();
			}
			PeriodItem pi = (PeriodItem) periodList.get(calPeriod);
			begin = pi.getBegin(end);
		}
		return begin;
	}
	/**
	 * @param begin The begin to set.
	 */
	public void setBegin(String begin) {
		this.begin = string2Date(begin, "00:00");
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
		this.end = string2Date(end, "23:59");
		if(this.end == null)
			this.end = new Date();
	}
	
	public void setScale(int scale) {
		calPeriod = scale;
		end = new Date();
		begin = null;
	}

	/**
	 * Calculate date from string parametrs comming from the URL
	 *
	 * @param sbegin String
	 * @param send String
	 * @param begin The calculated begin date
	 * @param end The calculated end date
	 */
	private Date string2Date(String date, String hour){
		Date foundDate = null;
		if("NOW".compareToIgnoreCase(date) == 0) {
			foundDate = new Date();
		}
		else if(datePattern.matcher(date).matches()) {
			try {
				foundDate = df.parse(date + ":" + hour);
			} catch (ParseException e) {
				logger.error("Illegal date argument: " + e);
			}
		}
		if(foundDate == null) {
			try {
				long value = Long.parseLong(date);
				if(value == 0)
					foundDate = new Date();
				else if(value > 0)
					foundDate = new Date(value);
				else
					calPeriod = (int) value;
			}
			catch (NumberFormatException ex) {}
		}
		return foundDate;
	}
	static public List getPeriodNames() {
		List periodName = new ArrayList(periodList.size());
		for(Iterator i = periodList.iterator(); i.hasNext() ;) {
			String name =  ((Period.PeriodItem)i.next()).name;
			periodName.add(name);
		}
		return periodName;
	}
}
