package jrds.snmp.log;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.snmp4j.log.LogAdapter;
import org.snmp4j.log.LogFactory;

public class Slf4jLogFactory extends LogFactory {

    private final Map<String, LogAdapter> loggers = new ConcurrentHashMap<>();

    @Override
    protected LogAdapter createLogger(String loggerName) {
        return loggers.computeIfAbsent(loggerName, Slf4jLogAdapter::new);
    }

    @Override
    public LogAdapter getRootLogger() {
        return createLogger(Logger.ROOT_LOGGER_NAME);
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected LogAdapter createLogger(Class clazz) {
        return createLogger(clazz.getName());
    }

    @Override
    public Iterator<LogAdapter> loggers() {
        return loggers.values().iterator();
    }

}
