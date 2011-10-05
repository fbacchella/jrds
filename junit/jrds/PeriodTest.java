package jrds;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class PeriodTest {
    static final private Logger logger = Logger.getLogger(PeriodTest.class);

    static final private DateFormat fullISOFORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    static final private DateFormat strictISOFORMAT = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ssZ");

    private Date begin;
    private Date end;

    @BeforeClass 
    static public void configure() throws IOException {
        jrds.Tools.configure();
        Tools.setLevel(logger, Level.TRACE, "jrds.Period");
    }

    @Test
    public void empty() {
        Date now = new Date();
        Period p = new Period();
        logger.trace("7" + " == " + p.getScale());
        Assert.assertTrue(p.getEnd().compareTo(now) >= 0);
        Assert.assertEquals(new Integer(7), new Integer(p.getScale()));
    }
    @Test
    public void scale() {
        Date now = new Date();

        Calendar calBegin = Calendar.getInstance();
        calBegin.setTime(now);
        calBegin.add(Calendar.HOUR, -4);
        Date begin = calBegin.getTime();

        Period p = new Period(4);
        long delta = p.getBegin().getTime() - begin.getTime();
        Assert.assertTrue(p.getEnd().compareTo(now) >= 0);
        logger.trace(delta);
        Assert.assertTrue(delta < 10 && delta > -10);
        Assert.assertEquals(new Integer(4), new Integer(p.getScale()));
    }
    @Test
    public void setScale() throws ParseException {
        Date now = new Date();
        begin = fullISOFORMAT.parse("1900-01-01T00:00:00");
        end = fullISOFORMAT.parse("1900-12-31T23:59:59");
        Period p = new Period("2007-01-01", "2007-12-31");
        p.setScale(1);
        Assert.assertTrue(p.getEnd().compareTo(now) >= 0);
        Assert.assertEquals(1, p.getScale());
    }
    @Test
    public void now() throws ParseException {
        Date now = new Date();
        Period p = new Period("NOW","now");
        Assert.assertTrue(p.getEnd().compareTo(now) >= 0);
        Assert.assertTrue(p.getBegin().compareTo(now) >= 0);
        Assert.assertEquals(0, p.getScale());
    }
    @Test public void shortFormat() throws ParseException {
        begin = fullISOFORMAT.parse("2007-01-01T00:00:00");
        end = fullISOFORMAT.parse("2007-12-31T23:59:59");
        Period p = new Period("2007-01-01", "2007-12-31");
        Assert.assertEquals(p.getBegin(), begin);
        Assert.assertEquals(p.getEnd(), end);
        Assert.assertEquals(0, p.getScale());
    }
    @Test
    public void mixedFormat() throws ParseException {
        begin = fullISOFORMAT.parse("2007-01-01T00:00:00");
        end = fullISOFORMAT.parse("2007-12-31T23:59:59");
        Period p = new Period("2007-01-01T00:00", "2007-12-31T23:59");
        Assert.assertEquals(p.getBegin(), begin);
        Assert.assertEquals(p.getEnd(), end);
        Assert.assertEquals(0, p.getScale());
    }
    @Test
    public void hourShortFormat() throws ParseException {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 1);
        cal.set(Calendar.MINUTE, 12);
        cal.set(Calendar.SECOND, 00);
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
        Assert.assertEquals(0, p.getScale());
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
        Assert.assertEquals(0, p.getScale());
    }
    @Test
    public void fullIsoFormat() throws ParseException {
        begin = fullISOFORMAT.parse("2007-01-01T00:00:00");
        end = fullISOFORMAT.parse("2007-12-31T23:59:59");
        Period p = new Period("2007-01-01T00:00:00", "2007-12-31T23:59:59");
        Assert.assertEquals(begin, p.getBegin());
        Assert.assertEquals(end, p.getEnd());
        Assert.assertEquals(0, p.getScale());
    }
    @Test
    public void StrictIsoFormat() throws ParseException {
        begin = strictISOFORMAT.parse("20070101T00:00:00+0000");
        end = fullISOFORMAT.parse("2007-12-31T23:59:59");
        Period p = new Period("20070101T00:00:00Z", "20071231T23:59:59");
        Assert.assertEquals(begin, p.getBegin());
        Assert.assertEquals(end, p.getEnd());
        Assert.assertEquals(0, p.getScale());
    }
    @Test
    public void spaceSeparator() throws ParseException {
        begin = fullISOFORMAT.parse("2007-02-01T00:15:31");
        end = fullISOFORMAT.parse("2007-12-31T23:59:50");
        Period p = new Period("2007-02-01 00:15:31", "2007-12-31 23:59:50");
        Assert.assertEquals(begin, p.getBegin());
        Assert.assertEquals(end, p.getEnd());
        Assert.assertEquals(0, p.getScale());
    }
    @Test
    public void fromLong() throws ParseException {
        begin = new Date(1);
        end = new Date(100000);
        Period p = new Period("1", "100000");
        Assert.assertEquals(begin, p.getBegin());
        Assert.assertEquals(end, p.getEnd());
        Assert.assertEquals(0, p.getScale());
    }
    @Test(expected=ParseException.class)
    public void invalid1() throws ParseException{
        Period p = new Period("a1", "2007-01");
        Assert.assertNull(p.getBegin());
        Assert.assertNull(p.getEnd());
        Assert.assertEquals(0, p.getScale());
    }
    @Test(expected=ParseException.class)
    public void invalid2() throws ParseException{
        Period p = new Period(" ", "");
        Assert.assertNull(p.getBegin());
        Assert.assertNull(p.getEnd());
        Assert.assertEquals(0, p.getScale());
    }
    @Test(expected=ParseException.class) public void nullargs() throws ParseException{
        Period p = new Period(null, null);
        Assert.assertNull(p.getBegin());
        Assert.assertNull(p.getEnd());
        Assert.assertEquals(0, p.getScale());
    }
    @Test(expected=ParseException.class)
    public void invaliddate() throws ParseException{
        @SuppressWarnings("unused")
        Period p = new Period("2007-14-42 00:15:31", "2007-12-31 23:59:50");
    }

}
