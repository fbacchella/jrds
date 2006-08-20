package jrds;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.jrobin.core.RrdException;

public class Renderer {
	private class GraphKey {
		private long start;
		private long end;
		private int id;
		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		public GraphKey(RdsGraph graph, Date start, Date end) throws RrdException {
			long step = graph.probe.getRrdDef().getStep();
			this.start = org.jrobin.core.Util.normalize(start.getTime(), step * 1000L);
			this.end = org.jrobin.core.Util.normalize(end.getTime(), step * 1000L);
			this.id = graph.hashCode();
		}
		@Override
		public int hashCode() {
			final int PRIME = 31;
			int result = 1;
			result = PRIME * result + (int) (end ^ (end >>> 32));
			result = PRIME * result + id;
			result = PRIME * result + (int) (start ^ (start >>> 32));
			return result;
		}
		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			final GraphKey other = (GraphKey) obj;
			if (end != other.end)
				return false;
			if (id != other.id)
				return false;
			if (start != other.start)
				return false;
			return true;
		}
	};

	public class RendererRun implements Runnable {
		public BufferedImage bImg;
		public Date start;
		public Date end;
		public RdsGraph graph;
		public boolean  finished = false;
		public boolean knowSize = true;
		public RendererRun(Date start, Date end, RdsGraph graph) {
			this.start = start;
			this.end = end;
			this.graph = graph;
		}
		public synchronized void run() {
			if(bImg == null) {			//write is sometimes call before run
				bImg = graph.makeImg(start, end);				
				finished = true;
			}
		}
		public synchronized void write(OutputStream out) throws IOException {
			//We wait for the lock on the object, meaning rendering is still running
			if(bImg == null) { 			//write is sometimes call before run
				logger.info("image for " + graph + " not rendered correctly");
				bImg = graph.makeImg(start, end);				
			}
			javax.imageio.ImageIO.write(bImg, "png", out);
		}
		public void write() throws IOException {
			OutputStream out = new BufferedOutputStream(new FileOutputStream(new File(
					graph.getPngName())));
			write(out);
		}
	};

	static private final Logger logger = Logger.getLogger(Renderer.class);
	static private final float hashTableLoadFactor = 0.75f;
	private final ExecutorService tpool =  Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
	private int cacheSize;
	private Map<GraphKey, RendererRun> rendered;

	public Renderer(int cacheSize) {
		int hashTableCapacity = (int)Math.ceil(cacheSize / hashTableLoadFactor) + 1;
		rendered = new LinkedHashMap<GraphKey, RendererRun>(hashTableCapacity, hashTableLoadFactor, true) {
			/* (non-Javadoc)
			 * @see java.util.LinkedHashMap#removeEldestEntry(java.util.Map.Entry)
			 */
			@Override
			protected boolean removeEldestEntry(Entry<GraphKey, RendererRun> eldest) {
				return eldest.getValue() != null && eldest.getValue().finished &&  size() > Renderer.this.cacheSize;
			}

		};	
	}

	public void render(final RdsGraph graph, final Date start, final Date end) {
		RendererRun runRender = null;
		GraphKey key;
		try {
			key = new GraphKey(graph, start, end);
			synchronized(rendered){
				if( ! rendered.containsKey(key)) {
					// Create graphics object
					runRender = new RendererRun(start, end, graph); 
					rendered.put(key, runRender);
				}
			}
		} catch (RrdException e) {
			logger.error("Problem with renderer key for graph " + graph);
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

	public void write(RdsGraph graph, Date start, Date end, OutputStream out) throws IOException {
		RendererRun runRender = null;
		synchronized(rendered){
			GraphKey key;
			try {
				key = new GraphKey(graph, start, end);
				runRender = rendered.remove(key);
			} catch (RrdException e) {
				logger.error("Problem with renderer key for graph " + graph);
			}
		}
		if(runRender != null) {
			runRender.write(out);
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
}
