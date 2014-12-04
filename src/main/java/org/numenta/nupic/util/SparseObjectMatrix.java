/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
 * following terms and conditions apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 *
 * http://numenta.org/licenses/
 * ---------------------------------------------------------------------
 */

package org.numenta.nupic.util;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.Arrays;

/**
 * Allows storage of array data in sparse form, meaning that the indexes
 * of the data stored are maintained while empty indexes are not. This allows
 * savings in memory and computational efficiency because iterative algorithms
 * need only query indexes containing valid data.
 * 
 * @author David Ray
 *
 * @param <T>
 */
public class SparseObjectMatrix<T> extends SparseMatrix<T> {
    
    private TIntObjectMap<T> sparseMap = new TIntObjectHashMap<>();
    

    
    public SparseObjectMatrix(int[] dimensions, boolean useColumnMajorOrdering) {
        super(dimensions, useColumnMajorOrdering);
    }
    
    /**
     * Constructs a new {@code SparseObjectMatrix}
     * @param dimensions					the dimensions of this array
     * @param useColumnMajorOrdering		where inner index increments most frequently
     */
    public SparseObjectMatrix(int[] dimensions) {
        this(dimensions, false);
    }
    
//    /**
//     * Sets the object to occupy the specified index.
//     * 
//     * @param index     the index the object will occupy
//     * @param object    the object to be indexed.
//     */
//    @SuppressWarnings("unchecked")
//    @Override
//    public void set(int index, T object) {
//        sparseMap.put(index, object);        
//    }

    /**
     * Returns an outer array of T values.
     * @return
     */
    @SuppressWarnings("unchecked")    
    public T[] values() {
    	return (T[])sparseMap.values();
    }
    
    /**
     * Returns the T at the specified index.
     * 
     * @param index     the index of the T to return
     * @return  the T at the specified index.
     */
    @Override
    public T getIndex(final int index) {
        return sparseMap.get(index);
    }
    
    /**
     * Returns the T at the index computed from the specified coordinates
     * @param coordinates   the coordinates from which to retrieve the indexed object
     * @return  the indexed object
     */
    @Override
    public T get(final int... coordinates) {
        return sparseMap.get(computeIndex(coordinates));
    }
    
    /**
     * Returns a sorted array of occupied indexes.
     * @return  a sorted array of occupied indexes.
     */
    @Override
    public int[] getSparseIndices() {
        return ArrayUtils.reverseIt(sparseMap.keys());
    }

    @Override
    public void set(T object, int... coordinates) {
        int i = computeIndex(coordinates);
        setIndex(object, i);
    }

    @Override
    public void setIndex(T object, int index) {
        sparseMap.put(index, object);
    }

    @Override
    public String toString() {
        return "m" + Arrays.toString( dimensions );
    }

    
    

    

    
    
}
