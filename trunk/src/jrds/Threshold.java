package jrds;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.net.SMTPAppender;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.TriggeringEventEvaluator;
import org.rrd4j.ConsolFun;
import org.rrd4j.core.FetchRequest;
import org.rrd4j.core.RrdDb;
public class Threshold {
	static final private Logger logger = Logger.getLogger(Threshold.class);

	public enum Comparator {
		HIGH {
			@Override
			public boolean check(double a, double b) {
				return Double.compare(a, b) >= 0;
			}
		},
		LOW {
			@Override
			public boolean check(double a, double b) {
				return Double.compare(a, b) <= 0;
			}
		};
		public abstract boolean check(double a, double b);
	};

	static final Logger mailloger = Logger.getLogger("maillogger");

	static final public  SMTPAppender mailappender = new org.apache.log4j.net.SMTPAppender( new TriggeringEventEvaluator() {
		public boolean isTriggeringEvent(LoggingEvent arg0) {
			return true;
		}
	});
	static {
		mailappender.setSubject("Threshold reached");
		mailappender.setName("Jrds Thresold logger");
		mailappender.setLayout(new PatternLayout() );
		mailappender.setSMTPDebug(false);
		mailappender.setThreshold(Level.TRACE);
		mailappender.activateOptions();
		mailloger.addAppender(mailappender);
	}
	public enum Action {
		LOG{
			@Override
			public void run(Threshold t, Probe p, List<Object> args) {
				Level loglevel = Level.INFO;
				if(args != null && args.size() == 1) {
					String level = args.get(0).toString();
					loglevel = Level.toLevel(level);
				}
				logger.log(loglevel, "Threshold reached for " + t.name + " on "+ p);
			}
		},
		MAIL{
			@Override
			public void run(Threshold t, Probe p, List<Object> args) {
				mailloger.log(Level.INFO, "Threshold reached for " + t.name + " on "+ p);
			}
		},
		TRAP {
			@Override
			public void run(Threshold t, Probe p, List<Object> args) {
			}
		};
		public abstract void run(Threshold t, Probe p, List<Object> args);
	}

	String name;
	String dsName;
	double value;
	long duration;
	Comparator operation;
	long firstTrue;
	List<Action> actions = new ArrayList<Action>(1);
	List<List<Object>> actionsArgs = new ArrayList<List<Object>>(1);

	/**
	 * Construct a Thresold object
	 * @param name
	 * @param dsName
	 * @param value
	 * @param duration in minute
	 * @param operation
	 */
	public Threshold(String name, String dsName, double value, long duration,
			Comparator operation) {
		super();
		this.name = name;
		this.dsName = dsName;
		this.value = value;
		this.duration = duration * 60;
		this.operation = operation;
		logger.debug( Threshold.mailappender.getErrorHandler());
	}

	public void addAction(Action a, List<Object> args) {
		actions.add(a);
		actionsArgs.add(args);
	}

	public boolean check(RrdDb db) {
		try {
			long lastUpdate = db.getLastUpdateTime();
			long tempduration = Math.max(duration, db.getHeader().getStep());
			FetchRequest fr = db.createFetchRequest(ConsolFun.AVERAGE, lastUpdate - tempduration , lastUpdate);
			double collected = fr.fetchData().getAggregate(dsName, ConsolFun.AVERAGE);
			if(Double.isNaN(collected))
				return false;
			logger.debug("compare value:" + value + " to " +  fr.fetchData().getAggregate(dsName, ConsolFun.AVERAGE) + ", result:"+ Double.compare(collected, value));
			return operation.check(collected, value);
		} catch (IOException e) {
			logger.equals("Check failed for " + this);
		}

		return false;
	}

	public void run(Probe p) {
		int i=0;
		for(Action a: actions) {
			a.run(this, p, actionsArgs.get(i++));
		}
	}
}
