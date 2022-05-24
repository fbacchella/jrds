package jrds.bootstrap;

import java.util.Properties;

public interface CommandStarter {
	void configure(Properties configuration);
	void start(String[] args) throws Exception;
	void help();
	String getName();
}
