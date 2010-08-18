/*
 * Copyright (C) 2001 Ciaran Treanor <ciaran@codeloop.com>
 *
 * Distributable under GPL license.
 * See terms of license at gnu.org.
 *
 * $Id: DataChunk.java,v 1.1 2006/02/03 08:27:16 sasam Exp $
 */
package jrds.jrrd;

import java.util.Map;

import org.rrd4j.data.LinearInterpolator;
import org.rrd4j.data.Plottable;

/**
 * Models a chunk of result data from an RRDatabase.
 *
 * @author <a href="mailto:ciaran@codeloop.com">Ciaran Treanor</a>
 * @version $Revision: 1.1 $
 */
public class DataChunk {

    private static final String NEWLINE = System.getProperty("line.separator");
    long startTime;
    int start;
    int end;
    long step;
    int dsCount;
    double[][] data;
    int rows;
    private Map<String, Integer> nameindex;

    DataChunk(Map<String, Integer> nameindex, long startTime, int start, int end, long step, int dsCount, int rows) {
    	this.nameindex = nameindex;
        this.startTime = startTime;
        this.start = start;
        this.end = end;
        this.step = step;
        this.dsCount = dsCount;
        this.rows = rows;
        data = new double[rows][dsCount];
    }

    /**
     * Returns a summary of the contents of this data chunk. The first column is
     * the time (RRD format) and the following columns are the data source
     * values.
     *
     * @return a summary of the contents of this data chunk.
     */
    public String toString() {

        StringBuilder sb = new StringBuilder();
        long time = startTime;

        for (int row = 0; row < rows; row++, time += step) {
            sb.append(time);
            sb.append(": ");

            for (int ds = 0; ds < dsCount; ds++) {
                sb.append(data[row][ds]);
                sb.append(" ");
            }

            sb.append(NEWLINE);
        }

        return sb.toString();
    }
    
    /**
     * @return the data
     */
    public double[][] getData() {
        return data;
    }


	public Plottable toPlottable(String name) {
		int dsId = nameindex.get(name);
		long[] date =  new long[rows];
		double[] results =  new double[rows];
		if (dsId < dsCount && dsId >= 0) {
			long time = startTime;
			for (int row = 0; row < rows; row++, time += step) {
				date[row] = time;
				results[row] = data[row][dsId];
			}
		} else {
			throw new RuntimeException("Invalid dsId value requested <" + dsId
					+ "> (must be >=0 and <" + dsCount);
		}
		return new LinearInterpolator(date, results);
	}

}
