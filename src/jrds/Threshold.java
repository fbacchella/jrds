package jrds;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.net.SMTPAppender;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.TriggeringEventEvaluator;
public class Threshold {
	static final private Logger logger = Logger.getLogger(Threshold.class);

	public enum Comparator {
		GREATER {
			@Override
			public boolean check(double a, double b) {
				return Double.compare(a, b) >= 0;
			}
		},
		LESS {
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
		this.duration = duration * 60 * 1000;
		this.operation = operation;
	}

	public void addAction(Action a, List<Object> args) {
		actions.add(a);
		actionsArgs.add(args);

	}
	public boolean check(double collected) {
		return check(collected, System.currentTimeMillis());
	}

	public boolean check(double collected, long time) {
		boolean checked = operation.check(collected, value);
		if(!checked && firstTrue >= 0)
			firstTrue = -1;
		else if (checked && firstTrue < 0 ) {
			firstTrue = time;
		}
		return checked && (time - firstTrue) >= duration;
	}

	public void run(Probe p) {
		int i=0;
		for(Action a: actions) {
			a.run(this, p, actionsArgs.get(i++));
		}
	}
}
