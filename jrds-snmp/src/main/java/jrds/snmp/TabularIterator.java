package jrds.snmp;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.snmp4j.Target;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.TableEvent;
import org.snmp4j.util.TableUtils;

public class TabularIterator implements Iterable<SnmpVars> {

    private final List<TableEvent> events;

    public TabularIterator(SnmpConnection starter, Collection<OID> oids) {
        if (starter != null && starter.isStarted()) {
            Target snmpTarget = starter.getConnection();
            if (snmpTarget != null) {
                DefaultPDUFactory localfactory = new DefaultPDUFactory();
                TableUtils tableRet = new TableUtils(starter.getSnmp(), localfactory);
                tableRet.setMaxNumColumnsPerPDU(30);
                OID[] oidTab = new OID[oids.size()];
                oids.toArray(oidTab);
                events = tableRet.getTable(snmpTarget, oidTab, null, null);
            } else {
                events = Collections.emptyList();
            }
        } else {
            events = Collections.emptyList();
        }
    }

    @Override
    public Iterator<SnmpVars> iterator() {
        Iterator<TableEvent> tabIterator = events.iterator();
        return new Iterator<SnmpVars>() {

            @Override
            public boolean hasNext() {
                return tabIterator.hasNext();
            }

            @Override
            public SnmpVars next() {
                TableEvent te = tabIterator.next();
                VariableBinding[] columns = te.getColumns();
                return columns != null ? new SnmpVars(columns) : new SnmpVars();
            }
        };
    }

}
