package jrds;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.jrobin.core.RrdException;

public class Renderer {
	final int PRIME = 31;

	public class RendererRun implements Runnable {
		public Date start;
		public Date end;
		public RdsGraph graph;
		//public RrdGraph rrdgraph;
		public boolean  finished = false;
		File destFile;

		public RendererRun(Date start, Date end, RdsGraph graph, int keyid) throws IOException, RrdException {
			this.start = start;
			this.end = end;
			this.graph = graph;
			//rrdgraph = graph.getRrdGraph(start, end);
			destFile = new File(HostsList.getRootGroup().getTmpdir(), Integer.toHexString(keyid) + ".png");
		}

		@Override
		protected void finalize() throws Throwable {
			clean();
			super.finalize();
		}

		public void run() {
			if(! finished) {			//isReady is sometimes call before run
				writeImg();
			}
		}
		public boolean isReady() {
			boolean retValue = false;

			if(! finished ) { 			//isReady is sometimes call before run
				writeImg();
			}
			if(destFile.isFile() && destFile.canRead())
				retValue = true;
			return retValue;

		}
		public void send(OutputStream out) throws IOException {
			if(isReady()){
				WritableByteChannel outC = Channels.newChannel(out);
				FileChannel inC = new FileInputStream(destFile).getChannel();
				inC.transferTo(0, destFile.length(), outC);
				inC.close();
			}
		}

		public void write() throws IOException {
			OutputStream out = new BufferedOutputStream(new FileOutputStream(new File(
					graph.getPngName())));
			send(out);
			out.close();
		}

		public void clean(){
			if(destFile.isFile())
				destFile.delete();
		}

		private synchronized void writeImg() {
			try {
				BufferedImage bImg = graph.makeImg(start, end);
				if(bImg != null) {
					OutputStream out = new BufferedOutputStream(new FileOutputStream(destFile));
					javax.imageio.ImageIO.write(bImg, "png", out);
				}
			} catch (FileNotFoundException e) {
				logger.error("Error with temporary output file: " +e);
			} catch (IOException e) {
				logger.error("Error with temporary output file: " +e);
			} catch (Exception e) {
				logger.error("Run time rendering" + this, e);
			}
			//Allways set to true, we do not try again in case of failure
			finished = true;
		}

		@Override
		public String toString() {
			return graph + "#" + start + "#" + end;
		}

	};

	static private final Logger logger = Logger.getLogger(Renderer.class);
	static private final float hashTableLoadFactor = 0.75f;
	final private Object counter = new Object() {
		int i = 0;
		@Override
		public String toString() {
			return Integer.toString(i++);
		}

	};

	private final ExecutorService tpool =  Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 3, 
			new ThreadFactory() {
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "RendererThread" + counter);
			t.setDaemon(true);
			logger.debug("New thread name:" + t.getName());
			return t;
		}
	}
	);
	private int cacheSize;
	private Map<Integer, RendererRun> rendered;

	public Renderer(int cacheSize) {
		this.cacheSize = cacheSize;
		rendered = new LinkedHashMap<Integer, RendererRun>(cacheSize + 5 , hashTableLoadFactor, true) {
			/* (non-Javadoc)
			 * @see java.util.LinkedHashMap#removeEldestEntry(java.util.Map.Entry)
			 */
			@Override
			protected boolean removeEldestEntry(Entry<Integer, RendererRun> eldest) {
				RendererRun rr = eldest.getValue();
				if( rr != null && rr.finished &&  size() > Renderer.this.cacheSize) {
					remove(eldest.getKey());
					rr.clean();
				}
				return false;
			}

		};	
	}

	public void render(final RdsGraph graph, final Date start, final Date end) throws IOException, RrdException {
		RendererRun runRender = null;
		int key = 0;
		key = makeKey(graph, start, end);
		synchronized(rendered){
			if( ! rendered.containsKey(key)) {
				// Create graphics object
				runRender = new RendererRun(start, end, graph, key); 
				rendered.put(key, runRender);
				logger.debug("wants to render " + runRender);
			}
		}
		if(runRender != null){
			try {
				tpool.execute(runRender);
			}
			catch(RejectedExecutionException ex) {
				logger.warn("Render thread dropped for graph " + graph);
			}
		}
	}

	public boolean isReady(final RdsGraph graph, final Date start, final Date end) {
		RendererRun  runRender = null;
		int key = 0;
		try {
			key = makeKey(graph, start, end);
			synchronized(rendered){
				runRender = rendered.get(key);
			}
		} catch (RrdException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if( runRender == null) {
			try {
				render(graph, start, end);
				synchronized(rendered){
					runRender = rendered.get(key);
				}

			}
			// If cannot launch render, will always be false
			catch (IOException e) {
			}
			catch (RrdException e) {
			}
		}
		return (runRender != null) && runRender.isReady();
	}

	public void send(RdsGraph graph, Date start, Date end, OutputStream out) throws IOException {
		RendererRun runRender = null;
		int key;
		try {
			key = makeKey(graph, start, end);
			synchronized(rendered){
				runRender = rendered.get(key);
			}
		} catch (RrdException e) {
			logger.error("Error with probe: " + e);
		}
		if(runRender != null) {
			runRender.send(out);
		}
		else {
			logger.info("Not precalculated render found for " + graph);
			//No precalculation found, so we do it right now
			graph.writePng(out, start, end);
		}
	}

	public Collection<RendererRun> getWaitings() {
		return rendered.values();
	}

	public void finish() {
		tpool.shutdown();
		try {
			tpool.awaitTermination(HostsList.getRootGroup().getResolution() - 10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			logger.info("Collect interrupted");
		}

	}
	private int makeKey(RdsGraph graph, Date startDate, Date endDate) throws RrdException {
		long step = graph.probe.getRrdDef().getStep();
		long start = org.jrobin.core.Util.normalize(startDate.getTime(), step * 1000L);
		long end = org.jrobin.core.Util.normalize(endDate.getTime(), step * 1000L);
		int id = graph.hashCode();
		int result = 1;
		result = PRIME * result + (int) (end ^ (end >>> 32));
		result = PRIME * result + id;
		result = PRIME * result + (int) (start ^ (start >>> 32));
		return result;
	}
	
}
