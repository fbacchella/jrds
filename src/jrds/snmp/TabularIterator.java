package jrds.snmp;

import java.util.Collection;
import java.util.Iterator;

import jrds.probe.snmp.SnmpProbe;

import org.snmp4j.Target;
import org.snmp4j.smi.OID;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.TableEvent;
import org.snmp4j.util.TableUtils;

public class TabularIterator implements Iterator<SnmpVars> {
	Iterator<TableEvent> tabIterator;

	@SuppressWarnings("unchecked")
	public TabularIterator(SnmpProbe p, Collection<OID> oids) {
		SnmpStarter starter = p.getSnmpStarter();
		if(starter != null && starter.isStarted()) {
			Target snmpTarget = starter.getTarget();
			if(snmpTarget != null) {
				DefaultPDUFactory localfactory = new DefaultPDUFactory();
				TableUtils tableRet = new TableUtils(starter.getSnmp(), localfactory);
				tableRet.setMaxNumColumnsPerPDU(30);
				OID[] oidTab= new OID[oids.size()];
				oids.toArray(oidTab);
				tabIterator = tableRet.getTable(snmpTarget, oidTab, null, null).iterator();
			}
		}
	}
	public boolean hasNext() {
		return tabIterator.hasNext();
	}

	public SnmpVars next() {
		TableEvent te =  tabIterator.next();
		SnmpVars vars = new SnmpVars(te.getColumns());
		return vars;
	}
	
	public void remove() {
		throw new UnsupportedOperationException();
	}
}