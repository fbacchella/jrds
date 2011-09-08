/*##########################################################################
 _##
 _##  $Id$
 _##
 _##########################################################################*/

package jrds;

import java.text.ParseException;
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
    static private final String dateRegexpBoth = "((\\d\\d\\d\\d)-?(\\d\\d)-?(\\d\\d))?";
    static private final String timeRegexp = "((\\d?\\d):(\\d\\d))?(:(\\d\\d))?";
    static private final Pattern datePatternBoth = Pattern.compile( dateRegexpBoth+ "[T ]?" + timeRegexp + "(.*)");
    static private final Pattern secondsPattern = Pattern.compile( "\\d+");

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
            return new Date();
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
            try {
                if(logger.isTraceEnabled()) {
                    logger.trace("Matching " + date);
                    for(int i = 1; i <= dateMatcher.groupCount(); i++) {
                        logger.trace(i +": " + "'" + dateMatcher.group(i) + "'");
                    }
                }
                String dateFound = dateMatcher.group(1);
                String timeFound = dateMatcher.group(5);
                String secondFound = dateMatcher.group(8);
                String timeZoneFound = dateMatcher.group(10);
                if(dateFound == null && timeFound == null && secondFound == null) {
                    throw new ParseException("Invalid string to parse: " + date, 0);
                }
                Calendar cal = Calendar.getInstance();
                cal.setLenient(false);
                cal.setTime(new Date());
                cal.set(Calendar.MILLISECOND, 0);

                if( timeZoneFound != null &&  ! "".equals(timeZoneFound)) {
                    cal.setTimeZone(TimeZone.getTimeZone(timeZoneFound));
                }

                if(dateFound != null && ! "".equals(dateFound)) {
                    String year = dateMatcher.group(2);
                    cal.set(Calendar.YEAR, jrds.Util.parseStringNumber(year, 1970).intValue());
                    String month = dateMatcher.group(3);
                    cal.set(Calendar.MONTH, jrds.Util.parseStringNumber(month, 1).intValue() - 1);
                    String day = dateMatcher.group(4);
                    cal.set(Calendar.DAY_OF_MONTH, jrds.Util.parseStringNumber(day, 1).intValue());
                }

                if(timeFound == null || "".equals(timeFound)) {
                    if(isBegin) {
                        cal.set(Calendar.HOUR_OF_DAY, 00);
                        cal.set(Calendar.MINUTE, 00);
                    }
                    else {
                        cal.set(Calendar.HOUR_OF_DAY, 23);
                        cal.set(Calendar.MINUTE, 59);
                    }
                }
                else {
                    String hour = dateMatcher.group(6);
                    cal.set(Calendar.HOUR_OF_DAY, jrds.Util.parseStringNumber(hour, 0).intValue());

                    String minute = dateMatcher.group(7);
                    cal.set(Calendar.MINUTE, jrds.Util.parseStringNumber(minute, 0).intValue());
                }

                if(secondFound == null || "".equals(secondFound)) {
                    if(isBegin)
                        cal.set(Calendar.SECOND, 00);
                    else
                        cal.set(Calendar.SECOND, 59);
                }
                else {
                    String seconds = dateMatcher.group(9);
                    cal.set(Calendar.SECOND, jrds.Util.parseStringNumber(seconds, 0).intValue());
                }

                return cal.getTime();
            } catch (Exception e) {
                throw new ParseException("Invalid string to parse: " + date, 0);
            }
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
