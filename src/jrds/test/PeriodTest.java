package jrds.test;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import jrds.Period;

public class PeriodTest {
	static final Appender app = new WriterAppender() {
		public void doAppend(LoggingEvent event) {
			System.out.println(event.getLevel() + ": " + event.getMessage());
		}
	};
	static final private DateFormat fullISOFORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

	private Date begin;
	private Date end;

	@Test public void empty() {
		Date now = new Date();
		Period p = new Period();
		Assert.assertTrue(p.getEnd().compareTo(now) >= 0);
		Assert.assertEquals(7, p.getScale());
	}
	@Test public void scale() {
		Date now = new Date();

		Calendar calBegin = Calendar.getInstance();
		calBegin.setTime(now);
		calBegin.add(Calendar.HOUR, -4);
		Date begin = calBegin.getTime();

		Period p = new Period(4);
		long delta = p.getBegin().getTime() - begin.getTime();
		Assert.assertTrue(p.getEnd().compareTo(now) >= 0);
		System.out.println(delta);
		Assert.assertTrue(delta < 10 && delta > -10);
		Assert.assertEquals(4, p.getScale());
	}
	@Test public void setScale() throws ParseException {
		Date now = new Date();
		begin = fullISOFORMAT.parse("1900-01-01T00:00:00");
		end = fullISOFORMAT.parse("1900-12-31T23:59:59");
		Period p = new Period("2007-01-01", "2007-12-31");
		p.setScale(1);
		Assert.assertTrue(p.getEnd().compareTo(now) >= 0);
		Assert.assertEquals(1, p.getScale());
	}
	@Test public void now() throws ParseException {
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
	@Test public void mixedFormat() throws ParseException {
		begin = fullISOFORMAT.parse("2007-01-01T00:00:00");
		end = fullISOFORMAT.parse("2007-12-31T23:59:59");
		Period p = new Period("2007-01-01T00:00", "2007-12-31T23:59");
		Assert.assertEquals(p.getBegin(), begin);
		Assert.assertEquals(p.getEnd(), end);
		Assert.assertEquals(0, p.getScale());
	}
	@Test public void hourShortFormat() throws ParseException {
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
	@Test public void hourLongFormat() throws ParseException {
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
	@Test public void fullIsoFormat() throws ParseException {
		begin = fullISOFORMAT.parse("2007-01-01T00:00:00");
		end = fullISOFORMAT.parse("2007-12-31T23:59:59");
		Period p = new Period("2007-01-01T00:00:00", "2007-12-31T23:59:59");
		Assert.assertEquals(begin, p.getBegin());
		Assert.assertEquals(end, p.getEnd());
		Assert.assertEquals(0, p.getScale());
	}
	@Test public void spaceSeparator() throws ParseException {
		begin = fullISOFORMAT.parse("2007-02-01T00:15:31");
		end = fullISOFORMAT.parse("2007-12-31T23:59:50");
		Period p = new Period("2007-02-01 00:15:31", "2007-12-31 23:59:50");
		Assert.assertEquals(begin, p.getBegin());
		Assert.assertEquals(end, p.getEnd());
		Assert.assertEquals(0, p.getScale());
	}
	@Test public void fromLong() throws ParseException {
		begin = new Date(1);
		end = new Date(100000);
		Period p = new Period("1", "100000");
		Assert.assertEquals(begin, p.getBegin());
		Assert.assertEquals(end, p.getEnd());
		Assert.assertEquals(0, p.getScale());
	}
	@Test(expected=ParseException.class) public void invalid1() throws ParseException{
		Period p = new Period("a1", "2007-01");
		Assert.assertNull(p.getBegin());
		Assert.assertNull(p.getEnd());
		Assert.assertEquals(0, p.getScale());
	}
	@Test(expected=ParseException.class) public void invalid2() throws ParseException{
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
	@Test(expected=ParseException.class) public void invaliddate() throws ParseException{
		@SuppressWarnings("unused")
		Period p = new Period("2007-14-42 00:15:31", "2007-12-31 23:59:50");
	}

	@BeforeClass static public void configure() {
		System.getProperties().setProperty("java.awt.headless","true");
		jrds.JrdsLoggerConfiguration.initLog4J();
		app.setName(jrds.JrdsLoggerConfiguration.APPENDER);
		jrds.JrdsLoggerConfiguration.putAppender(app);
		Logger.getRootLogger().setLevel(Level.INFO);
		Logger.getLogger(jrds.JrdsLoggerConfiguration.APPENDER).setLevel(Level.INFO);
		Logger.getLogger("org.apache.commons.digester.Digester").setLevel(Level.INFO);
		Logger.getLogger("jrds.Period").setLevel(Level.TRACE);
	}

}
