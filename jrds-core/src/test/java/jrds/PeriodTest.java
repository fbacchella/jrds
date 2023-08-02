package jrds;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.event.Level;

public class PeriodTest {

    static final private DateFormat fullISOFORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    static final private DateFormat strictISOFORMAT = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ssZ");

    @Rule
    public final Log4JRule logrule = new Log4JRule(this);
    private final Logger logger = logrule.getTestlogger();

    private Date begin;
    private Date end;

    @BeforeClass
    static public void configure() {
        jrds.Tools.configure();
    }

    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, "jrds.Period");
    }

    @Test
    public void empty() {
        Date now = new Date();

        Calendar calBegin = Calendar.getInstance();
        calBegin.setTime(now);
        Date end = calBegin.getTime();
        calBegin.add(Calendar.DAY_OF_MONTH, -1);
        Date begin = calBegin.getTime();

        Period p = new Period();
        long deltaBegin = Math.abs(p.getBegin().getTime() - begin.getTime());
        long deltaEnd = Math.abs(p.getEnd().getTime() + 1000 - end.getTime());
        logger.debug("delta being is " + deltaBegin + ", delta end is " + deltaEnd);
        Assert.assertTrue("Delta end is too wide: " + deltaEnd, deltaEnd < 1000);
        Assert.assertTrue("Delta begin is too wide: " + deltaBegin, deltaBegin < 1000);
        Assert.assertEquals(Period.Scale.DAY, p.getScale());
    }

    @Test
    public void scale() {
        Date now = new Date();

        Calendar calBegin = Calendar.getInstance();
        calBegin.setTime(now);
        Date end = calBegin.getTime();
        calBegin.add(Calendar.HOUR, -4);
        Date begin = calBegin.getTime();

        Period p = new Period(Period.Scale.HOURS4);
        long deltaBegin = Math.abs(p.getBegin().getTime() - begin.getTime());
        long deltaEnd = Math.abs(p.getEnd().getTime() + 1000 - end.getTime());
        Assert.assertTrue("Delta end is too wide: " + deltaEnd, deltaEnd < 1000);
        Assert.assertTrue("Delta begin is too wide: " + deltaBegin, deltaBegin < 1000);
        Assert.assertEquals(Period.Scale.HOURS4, p.getScale());
    }

    @Test
    public void now() throws ParseException {
        Date now = new Date();
        Period p = new Period("NOW", "now");
        long deltaBegin = p.getBegin().getTime() - now.getTime();
        // Don't forget that end was 1s less
        long deltaEnd = p.getEnd().getTime() + 1000 - now.getTime();
        logger.debug("delta being is " + deltaBegin + ", delta end is " + deltaEnd);
        logger.debug("{}", p.getEnd());
        // Less than 1 second appart
        Assert.assertTrue(Math.abs(deltaBegin) < 999);
        Assert.assertTrue(Math.abs(deltaEnd) < 999);
        Assert.assertEquals(Period.Scale.MANUAL, p.getScale());
    }

    @Test
    public void shortFormat() throws ParseException {
        begin = fullISOFORMAT.parse("2007-01-01T00:00:00");
        end = fullISOFORMAT.parse("2007-12-31T23:59:59");
        Period p = new Period("2007-01-01", "2007-12-31");
        Assert.assertEquals(p.getBegin(), begin);
        Assert.assertEquals(p.getEnd(), end);
        Assert.assertEquals(Period.Scale.MANUAL, p.getScale());
    }

    @Test
    public void mixedFormat() throws ParseException {
        begin = fullISOFORMAT.parse("2007-01-01T00:00:00");
        end = fullISOFORMAT.parse("2007-12-31T23:59:59");
        Period p = new Period("2007-01-01T00:00", "2007-12-31T23:59");
        Assert.assertEquals(p.getBegin(), begin);
        Assert.assertEquals(p.getEnd(), end);
        Assert.assertEquals(Period.Scale.MANUAL, p.getScale());
    }

    @Test
    public void hourShortFormat() throws ParseException {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 1);
        cal.set(Calendar.MINUTE, 12);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date begin = cal.getTime();

        cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 22);
        cal.set(Calendar.MINUTE, 58);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 0);
        Date end = cal.getTime();

        Period p = new Period("1:12", "22:58");
        Assert.assertEquals(begin, p.getBegin());
        Assert.assertEquals(end, p.getEnd());
        Assert.assertEquals(Period.Scale.MANUAL, p.getScale());
    }

    @Test
    public void hourLongFormat() throws ParseException {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 1);
        cal.set(Calendar.MINUTE, 12);
        cal.set(Calendar.SECOND, 15);
        cal.set(Calendar.MILLISECOND, 0);
        Date begin = cal.getTime();

        cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 22);
        cal.set(Calendar.MINUTE, 58);
        cal.set(Calendar.SECOND, 57);
        cal.set(Calendar.MILLISECOND, 0);
        Date end = cal.getTime();

        Period p = new Period("01:12:15", "22:58:57");
        Assert.assertEquals(begin, p.getBegin());
        Assert.assertEquals(end, p.getEnd());
        Assert.assertEquals(Period.Scale.MANUAL, p.getScale());
    }

    @Test
    public void fullIsoFormat() throws ParseException {
        begin = fullISOFORMAT.parse("2007-01-01T00:00:00");
        end = fullISOFORMAT.parse("2007-12-31T23:59:59");
        Period p = new Period("2007-01-01T00:00:00", "2007-12-31T23:59:59");
        Assert.assertEquals(begin, p.getBegin());
        Assert.assertEquals(end, p.getEnd());
        Assert.assertEquals(Period.Scale.MANUAL, p.getScale());
    }

    @Test
    public void StrictIsoFormat() throws ParseException {
        begin = strictISOFORMAT.parse("20070101T00:00:00+0000");
        end = strictISOFORMAT.parse("20071231T23:59:59+0000");
        Period p = new Period("20070101T00:00:00Z", "20071231T23:59:59Z");
        Assert.assertEquals(end, p.getEnd());
        Assert.assertEquals(begin, p.getBegin());
        Assert.assertEquals(Period.Scale.MANUAL, p.getScale());
    }

    @Test
    public void spaceSeparator() throws ParseException {
        begin = fullISOFORMAT.parse("2007-02-01T00:15:31");
        end = fullISOFORMAT.parse("2007-12-31T23:59:50");
        Period p = new Period("2007-02-01 00:15:31", "2007-12-31 23:59:50");
        Assert.assertEquals(begin, p.getBegin());
        Assert.assertEquals(end, p.getEnd());
        Assert.assertEquals(Period.Scale.MANUAL, p.getScale());
    }

    @Test
    public void previousFull() throws ParseException {
        begin = fullISOFORMAT.parse("2007-03-01T00:00:00");
        end = fullISOFORMAT.parse("2007-03-01T23:59:59");
        Period p = new Period("2007-03-02 00:00:00", "2007-03-02 23:59:59").previous();
        logger.trace("New begin is {}", p.getBegin());
        logger.trace("New end is {}", p.getEnd());
        Assert.assertEquals(begin, p.getBegin());
        Assert.assertEquals(end, p.getEnd());
        Assert.assertEquals(Period.Scale.MANUAL, p.getScale());
    }

    @Test
    public void nextFull() throws ParseException {
        begin = fullISOFORMAT.parse("2007-03-03T00:00:00");
        end = fullISOFORMAT.parse("2007-03-03T23:59:59");
        Period p = new Period("2007-03-02 00:00:00", "2007-03-02 23:59:59").next();
        logger.trace("{}", p.getBegin());
        logger.trace("{}", p.getEnd());
        Assert.assertEquals(begin, p.getBegin());
        Assert.assertEquals(end, p.getEnd());
        Assert.assertEquals(Period.Scale.MANUAL, p.getScale());
    }

    @Test
    public void nextFullWithOneSecond() throws ParseException {
        begin = fullISOFORMAT.parse("2007-03-03T00:00:00");
        end = fullISOFORMAT.parse("2007-03-03T23:59:59");
        Period p = new Period("2007-03-02 00:00:00", "2007-03-03T00:00:00").next();
        logger.trace("{}", p.getBegin());
        logger.trace("{}", p.getEnd());
        Assert.assertEquals(begin, p.getBegin());
        Assert.assertEquals(end, p.getEnd());
        Assert.assertEquals(Period.Scale.MANUAL, p.getScale());
    }

    @Test
    public void previousScale() {
        Period p = new Period().previous();
        long offsetDay = 86400 * 1000 - (p.getEnd().getTime() - p.getBegin().getTime());
        Assert.assertTrue("offset to large: " + offsetDay, Math.abs(offsetDay) < 1100);
        Assert.assertEquals(Period.Scale.MANUAL, p.getScale());
    }

    @Test
    public void nextScale() {
        Period p = new Period().next();
        logger.trace("{}", p.getBegin());
        logger.trace("{}", p.getEnd());
        long offsetDay = 86400 * 1000 - (p.getEnd().getTime() - p.getBegin().getTime());
        Assert.assertTrue("offset to large: " + offsetDay, Math.abs(offsetDay) < 1100);
        Assert.assertEquals(Period.Scale.MANUAL, p.getScale());
    }

    @Test
    public void fromLong() throws ParseException {
        begin = new Date(1000);
        end = new Date(100000);
        Period p = new Period("1000", "100000");
        Assert.assertEquals(begin, p.getBegin());
        Assert.assertEquals(end, p.getEnd());
        Assert.assertEquals(Period.Scale.MANUAL, p.getScale());
    }

    @Test(expected = ParseException.class)
    public void invalid1() throws ParseException {
        Period p = new Period("a1", "2007-01");
        Assert.assertNull(p.getBegin());
        Assert.assertNull(p.getEnd());
        Assert.assertEquals(Period.Scale.MANUAL, p.getScale());
    }

    @Test(expected = ParseException.class)
    public void invalid2() throws ParseException {
        Period p = new Period(" ", "");
        Assert.assertNull(p.getBegin());
        Assert.assertNull(p.getEnd());
        Assert.assertEquals(Period.Scale.MANUAL, p.getScale());
    }

    @Test(expected = ParseException.class)
    public void nullargs() throws ParseException {
        Period p = new Period(null, null);
        Assert.assertNull(p.getBegin());
        Assert.assertNull(p.getEnd());
        Assert.assertEquals(Period.Scale.MANUAL, p.getScale());
    }

    @Test(expected = ParseException.class)
    public void invaliddate() throws ParseException {
        @SuppressWarnings("unused")
        Period p = new Period("2007-14-42 00:15:31", "2007-12-31 23:59:50");
    }

}
