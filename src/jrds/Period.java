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
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
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
	static private final DateFormat isoFormatShort = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ssZ");
	static private final DateFormat dateFormatShort= new SimpleDateFormat("yyyyMMdd");
	static private final String dateRegexpBoth = "(\\d\\d\\d\\d-?\\d\\d-?\\d\\d)?";
	static private final String timeRegexp = "(\\d?\\d:\\d\\d)?(:\\d\\d)?";
	static private final Pattern datePatternBoth = Pattern.compile( dateRegexpBoth+ "[T ]?" + timeRegexp + "(.*)");
	static private final Pattern secondsPattern = Pattern.compile( "\\d+");
	static private final String timeZone = TimeZone.getDefault().getDisplayName();

	private static class PeriodItem {
		String name;
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
	static final private List<Period.PeriodItem> periodList = new ArrayList<Period.PeriodItem>(18);
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
		periodList.add(new Period.PeriodItem("Last 2 Weeks", Calendar.WEEK_OF_MONTH, -2));
		periodList.add(new Period.PeriodItem("Last Month", Calendar.MONTH, -1));
		periodList.add(new Period.PeriodItem("Last 2 Months", Calendar.MONTH, -2));
		periodList.add(new Period.PeriodItem("Last 3 Months", Calendar.MONTH, -3));
		periodList.add(new Period.PeriodItem("Last 4 Months", Calendar.MONTH, -4));
		periodList.add(new Period.PeriodItem("Last 6 Months", Calendar.MONTH, -6));
		periodList.add(new Period.PeriodItem("Last Year", Calendar.YEAR, -1));
		periodList.add(new Period.PeriodItem("Last 2 Years", Calendar.YEAR, -2));
	}

	private Date begin = null;
	private Date end = null;
	private int calPeriod = 7;

	public Period() {
		end = new Date();
	}

	public Period(int p) {
		calPeriod = p;
		end = new Date();
	}

	public Period(String begin, String end) throws ParseException {
		setBegin(begin);
		setEnd(end);
		this.calPeriod = 0;
	}

	/**
	 * @return Returns the begin.
	 */
	public Date getBegin() {
		if(calPeriod != 0) {
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
	 * @throws ParseException 
	 */
	public void setBegin(String begin) throws ParseException {
		this.begin = string2Date(begin, true);
		calPeriod = 0;
	}

	public void setBegin(Date begin) {
		this.begin = begin;
		calPeriod = 0;
	}

	/**
	 * @return Returns the end.
	 */
	public Date getEnd() {
		return end;
	}

	/**
	 * @param end The end to set.
	 * @throws ParseException 
	 */
	public void setEnd(String end) throws ParseException {
		this.end = string2Date(end, false);
		calPeriod = 0;
	}

	public void setEnd(Date end) {
		this.end = end;
	}

	public void setScale(int scale) {
		calPeriod = scale;
		end = new Date();
		begin = null;
	}

	/**
	 * @return the calPeriod
	 */
	public int getScale() {
		return calPeriod;
	}

	/**
	 * Calculate date from string parameters coming from the URL
	 *
	 * @param sbegin String
	 * @param send String
	 * @param begin The calculated begin date
	 * @param end The calculated end date
	 * @throws ParseException 
	 */
	private Date string2Date(String date, boolean isBegin) throws ParseException{
		Date foundDate = null;
		if(date == null) {
			throw new ParseException("Null string to parse", 0);
		}
		Matcher dateMatcher = datePatternBoth.matcher(date);
		if("NOW".compareToIgnoreCase(date) == 0) {
			foundDate = new Date();
		}
		else if(secondsPattern.matcher(date).matches()) {
			try {
				long value = Long.parseLong(date);
				if(value == 0)
					foundDate = new Date();
				else if(value > 0)
					foundDate = new Date(value);
				else
					calPeriod = (int) value;
			} catch (NumberFormatException e) {
				throw new ParseException("Not a long: " + e.getMessage(), 0);
			}				
		}
		else if(date.length() >= 4 && dateMatcher.find()) {
			if(logger.isTraceEnabled()) {
				logger.trace("Matching " + date);
				for(int i = 1; i <= dateMatcher.groupCount(); i++) {
					logger.trace(i +": " + "'" + dateMatcher.group(i) + "'");
				}
			}
			String dateFound = dateMatcher.group(1);
			String timeFound = dateMatcher.group(2);
			String secondFound = dateMatcher.group(3);
			String timeZoneFound = dateMatcher.group(4);
			if(dateFound == null && timeFound == null && secondFound == null) {
				throw new ParseException("Invalid string to parse: " + date, 0);
			}
			if(secondFound == null) {
				if(isBegin)
					secondFound = ":00";
				else
					secondFound = ":59";	
			}
			if(timeFound == null) {
				if(isBegin)
					timeFound = "00:00";
				else
					timeFound = "23:59";
			}
			if(dateFound == null) {
				Date now = new Date();

				Calendar cal = Calendar.getInstance();
				cal.setTime(now);
				dateFound = dateFormatShort.format(cal.getTime());
			}
			else {
				dateFound = dateFound.replaceAll("-", "");
			}
			if(timeZoneFound == null || "".equals(timeZoneFound)) {
				timeZoneFound = timeZone;
			}
			else if("Z".equals(timeZoneFound.toUpperCase())) {
				timeZoneFound="+0000";
			}
			isoFormatShort.setLenient(false);
			foundDate = isoFormatShort.parse(dateFound + "T" + timeFound + secondFound + timeZoneFound);
		}
		else {
			throw new ParseException("Invalid string to parse: " + date, 0);
		}
		return foundDate;
	}
	
	static public List<String> getPeriodNames() {
		List<String> periodName = new ArrayList<String>(periodList.size());
		for(Period.PeriodItem pi: periodList) 
			periodName.add(pi.name);
		return periodName;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((begin == null) ? 0 : begin.hashCode());
		result = PRIME * result + calPeriod;
		result = PRIME * result + ((end == null) ? 0 : end.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Period other = (Period) obj;
		if (begin == null) {
			if (other.begin != null)
				return false;
		} else if (!begin.equals(other.begin))
			return false;
		if (calPeriod != other.calPeriod)
			return false;
		if (end == null) {
			if (other.end != null)
				return false;
		} else if (!end.equals(other.end))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "b=" + begin + ", e=" + end + ", s=" + periodList.get(calPeriod).name;
	}

}
