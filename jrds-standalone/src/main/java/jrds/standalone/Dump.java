package jrds.standalone;

import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.rrd4j.ConsolFun;
import org.rrd4j.data.DataProcessor;

import jrds.GraphDesc;
import jrds.GraphNode;
import jrds.GraphTree;
import jrds.HostInfo;
import jrds.HostsList;
import jrds.Probe;
import jrds.PropertiesManager;
import jrds.store.ExtractInfo;
import jrds.store.Extractor;

public class Dump extends CommandStarterImpl {

    private String propFileName = "jrds.properties";

    public void configure(Properties configuration) {
        propFileName = configuration.getProperty("propertiesFile", propFileName);
    }

    @Override
    public void start(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");

        Properties props = new Properties();
        File propFile = new File(propFileName);
        if(propFile.isFile()) {
            props.load(new FileReader(propFile));
        }

        PropertiesManager propertiesManager = new PropertiesManager();
        propertiesManager.join(props);
        propertiesManager.importSystemProps();
        propertiesManager.update();
        HostsList hostsList = new HostsList(propertiesManager);
        GraphTree gt = hostsList.getGraphTreeByHost();
        for (GraphNode gn: gt.enumerateChildsGraph()) {
            GraphDesc gd = gn.getGraphDesc();
            List<String> path = gd.getViewTree(gn);
        }
        Date now = new Date();
        Date start = new Date(now.getTime() - 10000000);
        long step = 100;
        ExtractInfo ei = ExtractInfo.get().make(ConsolFun.AVERAGE).make(start, now).make(step);
        for (HostInfo hi: hostsList.getHosts()) {
            System.out.println(hi.getName());
            for (Probe<?, ?> p: hi.getProbes()) {
                System.out.println("    " + p.getName());
                try (Extractor ex = p.fetchData()) {
                    for (String dsName: p.getPd().getDs()) {
                        ex.addSource(dsName, dsName);
                    }
                    DataProcessor dp = ei.getDataProcessor(ex);
                    System.out.println(Arrays.toString(dp.getSourceNames()));
                }
                
            }
        }
    }

}
