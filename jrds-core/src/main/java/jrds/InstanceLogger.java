package jrds;

import org.slf4j.Logger;
import org.slf4j.event.Level;

public interface InstanceLogger {
    
    Logger getInstanceLogger();

    default void log(Level l, Throwable e, String format, Object... elements) {
        jrds.Util.log(this, getInstanceLogger(), l, e, format, elements);
    }

    default void log(Level l, String format, Object... elements) {
        jrds.Util.log(this, getInstanceLogger(), l, null, format, elements);
    }

}
