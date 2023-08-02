package jrds;

import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class used to manage a time interval
 *
 * @author Fabrice Bacchella
 */
public class Period {
    static final private Logger logger = LoggerFactory.getLogger(Period.class);
    static private final String dateRegexpBoth = "((\\d\\d\\d\\d)-?(\\d\\d)-?(\\d\\d))?";
    static private final String timeRegexp = "((\\d?\\d):(\\d\\d))?(:(\\d\\d))?";
    static private final Pattern datePatternBoth = Pattern.compile(dateRegexpBoth + "[T ]?" + timeRegexp + "(.*)");
    static private final Pattern secondsPattern = Pattern.compile("\\d+");

    public enum Scale {
        MANUAL("Manual", null),
        HOUR("Last Hour", Duration.of(1, ChronoUnit.HOURS)),
        HOURS2("Last 2 Hours", Duration.of(2, ChronoUnit.HOURS)),
        HOURS3("Last 3 Hours", Duration.of(3, ChronoUnit.HOURS)),
        HOURS4("Last 4 Hours", Duration.of(4, ChronoUnit.HOURS)),
        HOURS6("Last 6 Hours", Duration.of(6, ChronoUnit.HOURS)),
        HOURS12("Last 12 Hours", Duration.of(12, ChronoUnit.HOURS)),
        DAY("Last Day", Duration.of(24, ChronoUnit.HOURS)),
        DAYS2("Last 2 Days", Duration.of(48, ChronoUnit.HOURS)),
        WEEK("Last Week", java.time.Period.ofDays(7)),
        WEEKS2("Last 2 Weeks", java.time.Period.ofDays(14)),
        MONTH("Last Month", java.time.Period.ofMonths(1)),
        MONTH2("Last 2 Months", java.time.Period.ofMonths(2)),
        MONTH3("Last 3 Months", java.time.Period.ofMonths(3)),
        MONTH4("Last 4 Months", java.time.Period.ofMonths(4)),
        MONTH6("Last 6 Months", java.time.Period.ofMonths(6)),
        YEAR("Last Year", java.time.Period.ofYears(1)),
        YEARS2("Last 2 Years",java.time.Period.ofYears(2));
        public final String name;
        public final TemporalAmount p;

        Scale(String name, TemporalAmount p) {
            this.name = name;
            this.p = p;
        }

        public static Scale valueOfOrdinal(int ordinal) {
            if(ordinal < 0) {
                throw new IllegalArgumentException("Period invalid: " + ordinal);
            }
            Scale[] scales = values();
            if(ordinal > scales.length) {
                throw new IllegalArgumentException("Period invalid: " + ordinal);
            }
            return scales[ordinal];
        }
    }

    private final ZonedDateTime begin;
    private final ZonedDateTime end;
    private final Scale scale;
    private final Duration period;

    public Period() {
        this(Scale.DAY);
    }

    public Period(int ordinal) {
        this(Scale.valueOfOrdinal(ordinal));
    }

    public Period(Scale scale) {
        this(scale, null, null);
    }

    public Period(String begin, String end) throws ParseException {
        this(Scale.MANUAL, string2Date(begin, true), string2Date(end, false));
    }

    private Period(Scale scale, ZonedDateTime begin, ZonedDateTime end) {
        this.scale = scale;

        if (begin == null || end ==  null) {
            end = ZonedDateTime.now().withNano(0);
            begin = end.minus(scale.p);
        } else {
            // Drop milliseconds, rrd4j precision is second anyway
            end = end.withNano(0);
            begin = begin.withNano(0);
        }
        Duration tryperiod = Duration.between(begin, end);
        if(begin.getSecond() == end.getSecond()) {
            // second for end and begin are the same, that's mathematically
            // wrong
            // but that's the way human and joda's period manage this
            // set end to one second less
            tryperiod = Duration.between(begin, end);
            end = end.minusSeconds(1);
        } else {
            tryperiod = tryperiod.plusSeconds(1);
        }
        this.period = tryperiod;
        this.begin = begin;
        this.end = end;
        logger.trace("now Period is {}, begin is {}, end is {}", period, begin, end);
    }

    public Period previous() {
        return new Period(Scale.MANUAL, begin.minus(period), end.minus(period));
    }

    public Period next() {
        return new Period(Scale.MANUAL, begin.plus(period), end.plus(period));
    }

    /**
     * @return Returns the begin of the period.
     */
    public Date getBegin() {
        return Date.from(begin.toInstant());
    }

    /**
     * @return Returns the end of the period.
     */
    public Date getEnd() {
        return Date.from(end.toInstant());
    }

    /**
     * @return the period's scale ordinal
     */
    public Scale getScale() {
        // should return the enum instead of ordinal here..
        return scale;
    }

    /**
     * Calculate date from string parameters coming from the URL
     *
     * @param date
     * @param isBegin
     * @throws ParseException
     */
    private static ZonedDateTime string2Date(String date, boolean isBegin) throws ParseException {
        if(date == null) {
            throw new ParseException("Null string to parse", 0);
        }
        Matcher dateMatcher = datePatternBoth.matcher(date);
        if("NOW".compareToIgnoreCase(date) == 0) {
            return ZonedDateTime.now();
        } else if (secondsPattern.matcher(date).matches()) {
            return ZonedDateTime.ofInstant(Instant.ofEpochMilli(Util.parseStringNumber(date, Long.MIN_VALUE)), ZoneId.systemDefault());
        } else if(date.length() >= 4 && dateMatcher.find()) {
            try {
                if(logger.isTraceEnabled()) {
                    logger.trace("Matching " + date);
                    for(int i = 1; i <= dateMatcher.groupCount(); i++) {
                        logger.trace(i + ": " + "'" + dateMatcher.group(i) + "'");
                    }
                }
                String dateFound = dateMatcher.group(1);
                String timeFound = dateMatcher.group(5);
                String secondFound = dateMatcher.group(8);
                String timeZoneFound = dateMatcher.group(10);
                if(dateFound == null && timeFound == null && secondFound == null) {
                    throw new ParseException("Invalid string to parse: " + date, 0);
                }

                ZoneId tz = ZoneId.systemDefault();

                if(timeZoneFound != null && ! timeZoneFound.isEmpty()) {
                    tz = ZoneId.of(timeZoneFound);
                }

                LocalDate jdate = LocalDate.now(tz);
                if(dateFound != null && !"".equals(dateFound)) {
                    int year = jrds.Util.parseStringNumber(dateMatcher.group(2), 1970);
                    int month = jrds.Util.parseStringNumber(dateMatcher.group(3), 1);
                    int day = jrds.Util.parseStringNumber(dateMatcher.group(4), 1);
                    jdate = LocalDate.of(year, month, day);
                }

                int hour;
                int minute;
                int second;
                if(timeFound == null || "".equals(timeFound)) {
                    if(isBegin) {
                        hour = 0;
                        minute = 0;
                    } else {
                        hour = 23;
                        minute = 59;
                    }
                } else {
                    hour = jrds.Util.parseStringNumber(dateMatcher.group(6), 0);
                    minute = jrds.Util.parseStringNumber(dateMatcher.group(7), 0);
                }

                if(secondFound == null || "".equals(secondFound)) {
                    if(isBegin)
                        second = 0;
                    else
                        second = 59;
                } else {
                    second = jrds.Util.parseStringNumber(dateMatcher.group(9), 0);
                }
                LocalTime jtime = LocalTime.of(hour, minute, second);

                return ZonedDateTime.of(jdate, jtime, tz);
            } catch (Exception e) {
                ParseException newex = new ParseException("Invalid string to parse: " + date, 0);
                newex.initCause(e);
                throw newex;
            }
        } else {
            throw new ParseException("Invalid string to parse: " + date, 0);
        }
    }

    /**
     * Return the list of period label
     * 
     * @return a list of string label
     */
    static public List<String> getPeriodNames() {
        List<String> periodName = new ArrayList<String>(Scale.YEARS2.ordinal() + 1);
        for(Scale pi: Scale.values())
            periodName.add(pi.name);
        return periodName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ((begin == null) ? 0 : begin.hashCode());
        result = PRIME * result + scale.ordinal();
        result = PRIME * result + ((end == null) ? 0 : end.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(obj == null)
            return false;
        if(getClass() != obj.getClass())
            return false;
        final Period other = (Period) obj;
        if(begin == null) {
            if(other.begin != null)
                return false;
        } else if(!begin.equals(other.begin))
            return false;
        if(scale != other.scale)
            return false;
        if(end == null) {
            if(other.end != null)
                return false;
        } else if(!end.equals(other.end))
            return false;
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "b=" + begin + ", e=" + end + ", s=" + scale.name;
    }

}
