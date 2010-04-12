package jrds.standalone;

import java.io.IOException;
import java.util.Properties;

import jrds.bootstrap.CommandStarter;

public abstract class CommandStarterImpl implements CommandStarter {

	public void help() {
		System.out.println("Unimplemented help");
	}

	static {
		try {
			jrds.JrdsLoggerConfiguration.initLog4J();
		} catch (IOException e) {
			throw new RuntimeException("Log configuration failed", e);
		}
	}

	public  void configure(Properties configuration) {
	}
	
	public abstract void start(String args[]) throws Exception;
}
