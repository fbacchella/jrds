What is it?
=============

Jrds is performance collector, much like cacti or munins. But it intends to be more easy to use and able to collect a high
number of machines in a very short time. It's fully written in java and avoid call external process to increase
performances. It uses [RRD4J](http://code.google.com/p/rrd4j/), a clone of [rrdtool](http://oss.oetiker.ch/rrdtool/|)
written in java.

The site is [jrds.fr](http://jrds.fr)

How it works?
=============

JRDS is a java web application, that can run in any servlet server like tomcat or resin. It can also run in a standalone
mode, using jetty.

It uses threads to parallelize work. Each host is collected within the same thread and the number of simultaneous threads
can be configured. It use only one thread for each host to avoid overload of a server.

It use mainly snmp to collect data, but can be easily extended. There is also some jdbc probes, a agent using RMI for the
communication, and it can also parse XML data collected with HTTP. The currently available probes can be found
[here](http://jrds.fr/sourcetype/start). Additional collectors can be used using external jars.
