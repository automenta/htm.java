/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.numenta.nupic.util;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.OpenMapRealVector;
import org.apache.commons.math3.linear.RealVector;

/**
 * Adds ability to row "trueCount"
 */
public class SparseBinaryMatrixTrueCount extends SparseBinaryMatrix {
    
    /** counts how many 'true' exist in each row */
    private final RealVector rowCounts;

    public static RealVector getDefaultRowCounter(int rows) {
        //TODO tune this threshold
        //if (rows < 256) {
            //dense
            return new ArrayRealVector(rows);
        /*}
        else {
            //sparse
            return new OpenMapRealVector(rows);
        }*/
    }
    
    public SparseBinaryMatrixTrueCount(int[] dimensions) {
        this(dimensions, false, 
                getDefaultRowCounter(dimensions[0])
                );
    }

    public SparseBinaryMatrixTrueCount(int[] dimensions, boolean useColumnMajorOrdering, RealVector rowCount) {
        super(dimensions, useColumnMajorOrdering);
        
        this.rowCounts = rowCount;
        
    }
    
    @Override
    protected void back(boolean val, int delta, int... coordinates) {
        super.back(val, delta, coordinates);
        
        //TODO see if there is a better way to do this than iterating
        //ex: if the bit changed, do something like:
        
        int e = coordinates[0];
        
        if (delta!=0) {
            
            double v = rowCounts.getEntry(e);
            if (Double.isNaN(v)) v = 0d;
            v += delta;
            rowCounts.setEntry(e, v);
        }
                
        /*
        //update true counts
        
        rowCounts.setEntry(e, ArrayUtils.aggregateArray(((Object[]) this.backingArray)[e]));
        */
    }    

    @Override
    public void clearStatistics(int row) {
        super.clearStatistics(row);
        rowCounts.setEntry(row, 0);        
    }
    
    /**
     * Returns the count of 1's set on the specified row.
     * @param index
     * @return
     */
    public int getTrueCount(int index) {
    	return (int)rowCounts.getEntry(index);
    }
    
    /**
     * Sets the count of 1's on the specified row.
     * @param index
     * @param count
     */
    public void setTrueCount(int index, int count) {
    	this.rowCounts.setEntry(index, count);
    }
    
    /**
     * Get the true counts for all outer indexes.     
     * This is slower than double[] getTrueCounts() 
     * if trueCounts is a sparse vector 
     * because it has to iterate through all indices
     */
    public int[] getTrueCountsInteger() {        
    	return ArrayUtils.realVectorToIntArray(rowCounts);
    }
    
    public double[] getTrueCounts() {        
    	return rowCounts.toArray();
    }
    
    
    
}
