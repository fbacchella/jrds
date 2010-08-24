package jrds.webapp.rpc;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import jrds.RdsHost;
import jrds.webapp.Configuration;
import jrds.webapp.rpc.JrdsRequestProcessorFactoryFactory.InitializableRequestProcessor;

import org.apache.xmlrpc.XmlRpcException;

public class ConfigurationInformations implements InitializableRequestProcessor {
	static final String REMOTENAME = "configurationinformations";
	
	Configuration config;

	public Object[] getHostsName() {
		Collection<RdsHost> hostsList = config.getHostsList().getHosts();
		Set<String> hosts = new HashSet<String>(hostsList.size());
		for(RdsHost host: hostsList) {
			hosts.add(host.getName());
		}
		return hosts.toArray();
	}

	public void init(Configuration config) throws XmlRpcException {
		this.config = config;
	}		
}
