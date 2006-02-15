/*
 * DateBean.java
 *
 * Created on 15 février 2006, 23:17
 */

package jrds.webapp;

import java.beans.*;
import java.io.Serializable;

/**
 * @author Fabrice Bacchella
 */
public class DateBean extends Object implements Serializable {
    private Date begin;
    private Date end;
    
    private PropertyChangeSupport propertySupport;
    
    public DateBean() {
    }
    
    
}
