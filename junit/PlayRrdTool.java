import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import jrds.Tools;
import jrds.jrrd.Header;
import jrds.jrrd.RRDatabase;

import org.junit.BeforeClass;
import org.junit.Test;


public class PlayRrdTool {
	//rrdtool create test.rrd             \
	//--start 920804400          \
	//DS:speed:COUNTER:600:U:U   \
	//RRA:AVERAGE:0.5:1:24       \
	//RRA:AVERAGE:0.5:6:10

	@BeforeClass
	static public void create() throws IOException {
		Process p = Runtime.getRuntime().exec(new String[] {"/sw/bin/rrdtool", "restore ", "-", "tmp/test.rrd"});
		InputStreamReader stdout = new InputStreamReader(p.getInputStream());
		OutputStreamWriter stdin = new OutputStreamWriter(p.getOutputStream());

		InputStreamReader rrdxml = new InputStreamReader(Tools.class.getResourceAsStream("/ressources/rrdtool.xml"));
		BufferedReader r = new BufferedReader(rrdxml);
		String line = r.readLine();
		while (line != null) {
			stdin.append(line);
			line = r.readLine();
		}
	}
	
	@Test
	public void testfromjrrd() {
		File sampleDir = new File("/tmp/jrrd/trunk/sample");
		sampleDir.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				try {
					if(pathname.getCanonicalPath().endsWith(".rrd"))
						dumpFile(pathname);
				} catch (Exception e) {
					System.out.println("Failed rrd:" + pathname.getAbsolutePath() +" because " + e.getMessage());
				}
				return false;
			}

		});
	}

	@Test
	public void open() throws IOException {
		RRDatabase db = new RRDatabase("tmp/test.rrd");
		System.out.println(db);
		Header h = db.getHeader();
		//		for(int i = 0 ; i < h.getDSCount(); i++ ) {
		//			DataSource ds = db.getDataSource(i);
		//			System.out.println( ds.getName());
		//			System.out.println( ds.getType());
		//			System.out.println( ds);
		//			System.out.println( ds.getPDPStatusBlock());
		//		}
		for(int k = 0 ; k < h.getDSCount(); k++) {
			double[][] values = db.getArchive(k).getValues();
			System.out.println(values.length + ":" +  values[0].length);
			for(int i= 0; i < values.length; i++) {
				for(int j= 0; j < values[i].length; j++) {
					System.out.print(" " + j + ":" + values[i][j]);
				}
				System.out.println("");
			}
		}
	}

	private RRDatabase dumpFile(File path) throws IOException {
		RRDatabase db = new RRDatabase(path);
		System.out.println(path.getAbsolutePath() + ":");
		System.out.println(db);
		return db;
	}
}
