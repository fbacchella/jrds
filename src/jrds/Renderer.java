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
		@Override
		public String toString() {
			return Integer.toString(hashCode());
		}

	};

	public class RendererRun implements Runnable {
		public Date start;
		public Date end;
		public RdsGraph graph;
		public boolean  finished = false;
		File destFile;

		public RendererRun(Date start, Date end, RdsGraph graph, int keyid) {
			this.start = start;
			this.end = end;
			this.graph = graph;
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
				OutputStream out = new BufferedOutputStream(new FileOutputStream(destFile));
				javax.imageio.ImageIO.write(bImg, "png", out);
			} catch (FileNotFoundException e) {
				logger.error("Error with temporary output file: " +e);
			} catch (IOException e) {
				logger.error("Error with temporary output file: " +e);
			}
			//Allways set to true, we do not try again in case of failure
			finished = true;
		}

	};

	static private final Logger logger = Logger.getLogger(Renderer.class);
	static private final float hashTableLoadFactor = 0.75f;
	private final ExecutorService tpool =  Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
	private int cacheSize;
	private Map<Integer, RendererRun> rendered;

	public Renderer(int cacheSize) {
		int hashTableCapacity = (int)Math.ceil(cacheSize / hashTableLoadFactor) + 1;
		rendered = new LinkedHashMap<Integer, RendererRun>(hashTableCapacity, hashTableLoadFactor, true) {
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

	public void render(final RdsGraph graph, final Date start, final Date end) {
		RendererRun runRender = null;
		GraphKey key;
		try {
			key = new GraphKey(graph, start, end);
			synchronized(rendered){
				if( ! rendered.containsKey(key.hashCode())) {
					// Create graphics object
					runRender = new RendererRun(start, end, graph, key.hashCode()); 
					rendered.put(key.hashCode(), runRender);
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

	public boolean isReady(final RdsGraph graph, final Date start, final Date end) {
		RendererRun  runRender = null;
		GraphKey key = null;
		try {
			key = new GraphKey(graph, start, end);
			synchronized(rendered){
				runRender = rendered.get(key.hashCode());
			}
		} catch (RrdException e) {
			logger.error("Problem with renderer key for graph " + graph);
		}
		if( runRender == null) {
			render(graph, start, end);
			synchronized(rendered){
				runRender = rendered.get(key.hashCode());
			}
		}
		return (runRender != null) && runRender.isReady();
	}

	public void send(RdsGraph graph, Date start, Date end, OutputStream out) throws IOException {
		RendererRun runRender = null;
		try {
			GraphKey key = new GraphKey(graph, start, end);
			synchronized(rendered){
				runRender = rendered.get(key.hashCode());
			}
		} catch (RrdException e) {
			logger.error("Problem with renderer key for graph " + graph);
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
}
