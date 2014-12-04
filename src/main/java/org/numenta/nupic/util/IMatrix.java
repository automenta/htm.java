/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.numenta.nupic.util;

/**
 *
 * @author me
 */
public interface IMatrix<T> {
    
        
    /**
     * Returns the array describing the dimensionality of the configured array.
     *
     * @return the array describing the dimensionality of the configured array.
     */
    public int[] getDimensions();

    /**
     * Returns the configured number of dimensions.
     *
     * @return the configured number of dimensions.
     */
    public int getNumDimensions();

    /**
     * Uses the specified {@link TypeFactory} to return an array filled with the
     * specified object type, according this {@code SparseMatrix}'s configured
     * dimensions
     *
     * @param factory a factory to make a specific type
     * @return the dense array
     */
    public T[] toArray(TypeFactory<T> factory);
    
    public int getMaxIndex();
    
    
    
    
    /**
     * Sets the specified object to be indexed at the index computed from the
     * specified coordinates.
     *
     * @param value the value to be indexed.
     * @param coordinates the row major coordinates [outer --> ,...,..., inner]
     *
     * @return this {@code SparseMatrix} implementation
     */
    public void set(T object, int... coordinates);
    
    
    
    
    public T get(final int... coordinates);
    

        
}
