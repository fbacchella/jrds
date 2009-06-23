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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

public class Renderer {
	final int PRIME = 31;

	public class RendererRun implements Runnable {
		public Graph graph;
		public boolean finished = false;
		public final ReentrantLock running = new ReentrantLock(); 
		File destFile;

		public RendererRun(Graph graph) throws IOException {
			this.graph = graph;
			destFile = new File(HostsList.getRootGroup().getTmpdir(), Integer.toHexString(graph.hashCode()) + ".png");
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
			running.lock();
			if( ! finished) {
				long starttime = System.currentTimeMillis();
				try {
					BufferedImage bImg = graph.makeImg();
					long middletime = System.currentTimeMillis();
					if(bImg != null) {
						OutputStream out = new BufferedOutputStream(new FileOutputStream(destFile));
						javax.imageio.ImageIO.write(bImg, "png", out);
					}
					if(logger.isTraceEnabled()) {
						long endtime = System.currentTimeMillis();
						long duration1 = (middletime - starttime );
						long duration2 = (endtime - middletime );
						logger.trace("Graph " + graph.getQualifieName() + " renderding ran for (ms) " + duration1 + ":" + duration2);	
					}
				} catch (FileNotFoundException e) {
					logger.error("Error with temporary output file: " +e);
				} catch (IOException e) {
					logger.error("Error with temporary output file: " +e);
					Throwable cause = e.getCause();
					if(cause != null)
						logger.error("    Cause was: " + cause);
				} catch (Exception e) {
					logger.error("Run time rendering" + this, e);
				}
				//Allways set to true, we do not try again in case of failure
				finished = true;
			}						
			running.unlock();
		}

		@Override
		public String toString() {
			return graph.toString();
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
		Map<Integer, RendererRun> m = new LinkedHashMap<Integer, RendererRun>(cacheSize + 5 , hashTableLoadFactor, true) {
			/* (non-Javadoc)
			 * @see java.util.LinkedHashMap#removeEldestEntry(java.util.Map.Entry)
			 */
			@Override
			protected boolean removeEldestEntry(Entry<Integer, RendererRun> eldest) {
				RendererRun rr = eldest.getValue();
				if( rr != null && rr.finished &&  size() > Renderer.this.cacheSize) {
					remove(eldest);
					rr.clean();
				}
				else if (rr != null &&  size() > Renderer.this.cacheSize){
					logger.info("Graph queue too short, it's now " +  size() + " instead of " + Renderer.this.cacheSize);
				}
				return false;
			}

		};
		rendered =  Collections.synchronizedMap(m);	
	}

	public void render(Graph graph) throws IOException {
		if( ! rendered.containsKey(graph.hashCode())) {
			synchronized(rendered){
				if( ! rendered.containsKey(graph.hashCode())) {
					RendererRun runRender = new RendererRun(graph);
					// Create graphics object
					rendered.put(graph.hashCode(), runRender);
					try {
						tpool.execute(runRender);
					}
					catch(RejectedExecutionException ex) {
						logger.warn("Render thread dropped for graph " + graph);
					}
					logger.debug("wants to render " + runRender);
				}
			}
		}
	}
	
	public Graph getGraph(int key) {
		Graph g = null;
		if(key != 0) {
			RendererRun rr = rendered.get(key);
			if(rr != null)
				g = rr.graph;
		}
		return g;
	}

	public boolean isReady(Graph graph) {
		RendererRun runRender = null;
		runRender = rendered.get(graph.hashCode());
		if( runRender == null) {
			try {
				render(graph);
				runRender = rendered.get(graph.hashCode());
			}
			// If cannot launch render, will always be false
			catch (IOException e) {
				logger.error("graph " + graph + " will not be calculated:" + e);
				runRender = null;
			}
		}
		return (runRender != null) && runRender.isReady();
	}

	public void send(Graph graph, OutputStream out) throws IOException {
		RendererRun runRender = null;
		try {
			runRender = rendered.get(graph.hashCode());
		} catch (Exception e) {
			logger.error("Error with probe: " + e);
		}
		if(runRender != null) {
			runRender.send(out);
		}
		else {
			logger.info("No precalculated render found for " + graph);
			//No precalculation found, so we do it right now
			graph.writePng(out);
		}
	}

	public Collection<RendererRun> getWaitings() {
		return rendered.values();
	}

	public void finish() {
		tpool.shutdown();
		try {
			tpool.awaitTermination(HostsList.getRootGroup().getStep() - 10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			logger.info("Collect interrupted");
		}

	}
}
