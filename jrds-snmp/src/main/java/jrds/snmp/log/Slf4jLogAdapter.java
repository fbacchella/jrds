package jrds.snmp.log;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.snmp4j.log.LogAdapter;
import org.snmp4j.log.LogLevel;

class Slf4jLogAdapter implements LogAdapter {

    private static final Marker FATAL_MARKER = MarkerFactory.getMarker("FATAL");

    private final Logger logger;
    private LogLevel level;

    Slf4jLogAdapter(String logger) {
        this.logger = LoggerFactory.getLogger(logger);
        this.level = LogLevel.ALL;
    }

    public boolean isTraceEnabled() {
        return level.getLevel() >= LogLevel.LEVEL_ALL && level.getLevel() <= LogLevel.LEVEL_TRACE && logger.isDebugEnabled();
    }

    @Override
    public boolean isDebugEnabled() {
        return level.getLevel() >= LogLevel.LEVEL_ALL && level.getLevel() <= LogLevel.LEVEL_DEBUG && logger.isDebugEnabled();
    }

    @Override
    public boolean isInfoEnabled() {
        return level.getLevel() >= LogLevel.LEVEL_ALL && level.getLevel() <= LogLevel.LEVEL_INFO && logger.isInfoEnabled();
    }

    @Override
    public boolean isWarnEnabled() {
        return level.getLevel() >= LogLevel.LEVEL_ALL && level.getLevel() <= LogLevel.LEVEL_WARN && logger.isWarnEnabled();
    }

    public boolean isErrorEnabled() {
        return level.getLevel() >= LogLevel.LEVEL_ALL && level.getLevel() <= LogLevel.LEVEL_ERROR && logger.isErrorEnabled();
    }

    public boolean isFatalEnabled() {
        return level.getLevel() >= LogLevel.LEVEL_ALL && level.getLevel() <= LogLevel.LEVEL_FATAL && logger.isErrorEnabled();
    }

    @Override
    public void debug(Serializable message) {
        if (isDebugEnabled()) {
            logger.debug(message.toString());
        }
    }

    @Override
    public void info(CharSequence message) {
        if (isInfoEnabled()) {
            logger.info(message.toString());
        }
    }

    @Override
    public void warn(Serializable message) {
        if (isWarnEnabled()) {
            logger.warn(message.toString());
        }
    }

    @Override
    public void error(Serializable message) {
        if (isErrorEnabled()) {
            logger.error(message.toString());
        }
    }

    @Override
    public void error(CharSequence message, Throwable throwable) {
        if (isErrorEnabled()) {
            logger.error(message.toString(), throwable);
        }
    }

    @Override
    public void fatal(Object message) {
        if (isFatalEnabled()) {
            logger.error(FATAL_MARKER, message.toString());
        }
    }

    @Override
    public void fatal(CharSequence message, Throwable throwable) {
        if (isFatalEnabled()) {
            logger.error(FATAL_MARKER, message.toString(), throwable);
        }
    }

    @Override
    public void setLogLevel(LogLevel level) {
        this.level = level;
    }

    @Override
    public LogLevel getLogLevel() {
        return level;
    }

    @Override
    public LogLevel getEffectiveLogLevel() {
        if (isTraceEnabled()) {
            return LogLevel.TRACE;
        } else if (isDebugEnabled()) {
            return LogLevel.DEBUG;
        } else if (isInfoEnabled()) {
            return LogLevel.INFO;
        } else if (isWarnEnabled()) {
            return LogLevel.WARN;
        } else if (isErrorEnabled()) {
            return LogLevel.ERROR;
        } else if (isFatalEnabled()) {
            return LogLevel.FATAL;
        } else {
            return LogLevel.NONE;
        }
    }

    @Override
    public String getName() {
        return logger.getName();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Iterator getLogHandler() {
        // Not used anyway, should be deprecated
        return Collections.EMPTY_LIST.iterator();
    }

    @Override
    public int hashCode() {
        return logger.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return logger.getName().equals(obj);
    }

    @Override
    public String toString() {
        return "slf4[" +  logger.getName() + "]." + level;
    }

}
