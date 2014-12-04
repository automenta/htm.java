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

import gnu.trove.TIntCollection;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.BitSet;

@SuppressWarnings("rawtypes")
public class SparseBinaryMatrix extends SparseMatrix<Byte> {

    FastBitSet sparseMap;
    //private TIntByteMap sparseMap = new TIntByteHashMap();
    Object backingArray;

    public SparseBinaryMatrix(int[] dimensions) {
        this(dimensions, false);
    }

    public SparseBinaryMatrix(int[] dimensions, boolean useColumnMajorOrdering) {
        super(dimensions, useColumnMajorOrdering);
        sparseMap = new FastBitSet(size());
        this.backingArray = Array.newInstance(int.class, dimensions);

    }

    /**
     * Called during mutation operations to simultaneously set the value on the
     * backing array dynamically.
     *
     * @param val
     * @param coordinates
     */
    protected void back(boolean val, int delta, int... coordinates) {
        ArrayUtils.setValue(this.backingArray, val ? (byte)1: (byte)0, coordinates);
    }

    /**
     * Returns the slice specified by the passed in coordinates. The array is
     * returned as an object, therefore it is the caller's responsibility to
     * cast the array to the appropriate dimensions.
     *
     * @param coordinates	the coordinates which specify the returned array
     * @return	the array specified
     * @throws	IllegalArgumentException if the specified coordinates address an
     * actual value instead of the array holding it.
     */
    public Object getSlice(int... coordinates) {
        Object slice = backingArray;
        for (int i = 0; i < coordinates.length; i++) {
            Object s = Array.get(slice, coordinates[i]);
            slice = s.getClass().isArray() ? s : s;
        }
        //Ensure return value is of type Array
        if (!slice.getClass().isArray()) {
            throw new IllegalArgumentException(
                    "This method only returns the array holding the specified index: "
                    + Arrays.toString(coordinates));
        }

        return slice;
    }

    /**
     * Fills the specified results array with the result of the matrix vector
     * multiplication.
     *
     * @param inputVector	the right side vector
     * @param results	the results array
     */
    public void rightVecSumAtNZ(int[] inputVector, int[] results) {
        for (int i = 0; i < dimensions[0]; i++) {
            int[] slice = (int[]) (dimensions.length > 1 ? getSlice(i) : backingArray);
            for (int j = 0; j < slice.length; j++) {
                results[i] += (inputVector[j] * slice[j]);
            }
        }
    }

    public void rightVecSumAtNZ(int[] inputVector, double[] results) {
        for (int i = 0; i < dimensions[0]; i++) {
            int[] slice = (int[]) (dimensions.length > 1 ? getSlice(i) : backingArray);
            for (int j = 0; j < slice.length; j++) {
                results[i] += (inputVector[j] * slice[j]);
            }
        }
    }

    public void rightVecSumAtNZ(int[] inputVector, double[] results, double stimulusThreshold, double[] factor) {
        for (int i = 0; i < dimensions[0]; i++) {
            int[] slice = (int[]) (dimensions.length > 1 ? getSlice(i) : backingArray);
            for (int j = 0; j < slice.length; j++) {
                results[i] += (inputVector[j] * slice[j]);
            }
        }

        for (int i = 0; i < results.length; i++) {
            if (results[i] < stimulusThreshold) {
                results[i] = 0;
            } else {
                results[i] *= factor[i];
            }
        }
    }

    /**
     * Sets the value at the specified index.
     *
     * @param index the index the object will occupy
     * @param object the object to be indexed.
     */
    @Override
    public void setIndex(Byte value, int index) {
        setIndex(value > 0 ? true : false, index);
    }

    public void setIndex(boolean value, int index) {        
        int[] coordinates = computeCoordinates(index);
        int delta = sparseMap.setAndGetChange(index, value);
        back(value, delta, coordinates);
    }

    /**
     * Sets the value to be indexed at the index computed from the specified
     * coordinates.
     *
     * @param coordinates the row major coordinates [outer --> ,...,..., inner]
     * @param object the object to be indexed.
     */
    @Override
    public void set(Byte value, int... coordinates) {
        int index = computeIndex(coordinates);
        boolean v = value > 0 ? true : false;
        int delta = sparseMap.setAndGetChange(index, v);
        back(v, delta, coordinates);
    }

    /**
     * Sets the specified values at the specified indexes.
     *
     * @param indexes indexes of the values to be set
     * @param values the values to be indexed.
     *
     * @return this {@code SparseMatrix} implementation
     */
    public SparseBinaryMatrix set(int[] indexes, byte[] values) {
        for (int i = 0; i < indexes.length; i++) {
            set(values[i], indexes[i]);
        }
        return this;
    }

    public SparseBinaryMatrix set(int[] indexes, byte value) {
        for (int i = 0; i < indexes.length; i++) {
            setIndex(value, indexes[i]);
        }
        return this;
    }

    public void set(boolean value, int... coord) {
        set(value ? (byte) 1 : (byte) 0, coord);
    }

    /**
     * Sets the value at the specified index skipping the automatic truth
     * statistic tallying of the real method.
     *
     * @param index the index the object will occupy
     * @param object the object to be indexed.
     */
    public SparseBinaryMatrix setForTest(int index, byte value) {
        sparseMap.set(index, value > 0 ? true : false);
        return this;
    }

    /**
     * Sets the specified values at the specified indexes.
     *
     * @param indexes indexes of the values to be set
     * @param values the values to be indexed.
     *
     * @return this {@code SparseMatrix} implementation
     */
    public SparseBinaryMatrix set(int[] indexes, byte[] values, boolean isTest) {
        for (int i = 0; i < indexes.length; i++) {
            if (isTest) {
                setForTest(indexes[i], values[i]);
            } else {
                set(values[i], indexes[i]);
            }
        }
        return this;
    }

    /**
     * Clears the true counts prior to a cycle where they're being set
     * for 2D only
     */
    public void clearStatistics(int row) {
        assert(getNumDimensions() == 2);
        
        //int[] slice = (int[])Array.get(backingArray, row);
        int[] slice = ((int[][]) backingArray)[row];
        Arrays.fill(slice, 0);
        
        int start = computeIndex(row, 0);
        int end = start + getDimensions()[1];
        for (int i = start; i < end; i++)
            sparseMap.set(i, false);
    }



    /**
     * Returns an outer array of T values.
     *
     * @return
     */
//    @Override
//    public int[] values() {
//    	return sparseMap.values();
//    }
    /**
     * Returns the int value at the index computed from the specified
     * coordinates
     *
     * @param coordinates the coordinates from which to retrieve the indexed
     * object
     * @return the indexed object
     */
    @Override
    public int getIntValue(int... coordinates) {
        return sparseMap.get(computeIndex(coordinates)) ? 1 : 0;
    }

    /**
     * Returns the T at the specified index.
     *
     * @param index the index of the T to return
     * @return the T at the specified index.
     */
    @Override
    public int getIntValue(int index) {
        return sparseMap.get(index) ? 1 : 0;
    }

    /**
     * Returns a sorted array of occupied indexes.
     *
     * @return a sorted array of occupied indexes.
     */
    @Override
    public int[] getSparseIndices() {
        int[] s = new int[sparseMap.cardinality()];
        int j = 0;
        for (int i = sparseMap.nextSetBit(0); i >= 0; i = sparseMap.nextSetBit(i+1)) {          
            s[j++] = i;
        }
        return s;
        //return ArrayUtils.reverseIt(sparseMap.keys());
    }
    
    public byte[] byteArray() {
        byte[] s = new byte[sparseMap.cardinality()];
        for (int i = sparseMap.nextSetBit(0); i >= 0; i = sparseMap.nextSetBit(i+1)) {          
            s[i] = 1;
        }
        return s;
    }
    public boolean[] booleanArray() {
        boolean[] s = new boolean[sparseMap.cardinality()];
        for (int i = sparseMap.nextSetBit(0); i >= 0; i = sparseMap.nextSetBit(i+1)) {          
            s[i] = true;
        }
        return s;
    }    

    /**
     * This {@code SparseBinaryMatrix} will contain the operation of or-ing the
     * inputMatrix with the contents of this matrix; returning this matrix as
     * the result.
     *
     * @param inputMatrix the matrix containing the "on" bits to or
     * @return this matrix
     */
    public SparseBinaryMatrix or(SparseBinaryMatrix inputMatrix) {
        int[] mask = inputMatrix.getSparseIndices();
        return set(mask, (byte) 1);
    }

    /**
     * This {@code SparseBinaryMatrix} will contain the operation of or-ing the
     * sparse list with the contents of this matrix; returning this matrix as
     * the result.
     *
     * @param onBitIndexes the matrix containing the "on" bits to or
     * @return this matrix
     */
    public SparseBinaryMatrix or(TIntCollection onBitIndexes) {
        return set(onBitIndexes.toArray(), (byte) 1);
    }

    /**
     * This {@code SparseBinaryMatrix} will contain the operation of or-ing the
     * sparse array with the contents of this matrix; returning this matrix as
     * the result.
     *
     * @param onBitIndexes the int array containing the "on" bits to or
     * @return this matrix
     */
    public SparseBinaryMatrix or(int[] onBitIndexes) {
        return set(onBitIndexes, (byte) 1);
    }

    /**
     * Returns true if the on bits of the specified matrix are matched by the on
     * bits of this matrix. It is allowed that this matrix have more on bits
     * than the specified matrix.
     *
     * @param m
     * @return
     */
    public boolean all(SparseBinaryMatrix m) {
        FastBitSet b = (FastBitSet) m.sparseMap.clone();
        //Clears all of the bits in this BitSet whose corresponding bit is set in the specified BitSet.
        b.andNot(sparseMap);
        
        //all of the bits will be cleared if every bit in m was cleared by a bit in sparseMap
        return !(b.cardinality() > 0);
    }

//    /**
//     * Returns true if the on bits of the specified list are matched by the on
//     * bits of this matrix. It is allowed that this matrix have more on bits
//     * than the specified matrix.
//     *
//     * @param matrix
//     * @return
//     */
//    public boolean all(TIntCollection onBits) {
//        return sparseMap.keySet().containsAll(onBits);
//    }

    /**
     * Returns true if the on bits of the specified array are matched by the on
     * bits of this matrix. It is allowed that this matrix have more on bits
     * than the specified matrix.
     *
     * @param matrix
     * @return
     */
    public boolean all(int[] onBits) {
        for (int i : onBits)
            if (!sparseMap.get(i))
                return false;
        return true;
    }

    /**
     * Returns true if any of the on bits of the specified matrix are matched by
     * the on bits of this matrix. It is allowed that this matrix have more on
     * bits than the specified matrix.
     *
     * @param matrix
     * @return
     */
    public boolean any(SparseBinaryMatrix matrix) {
        //TODO use bitvector boolean AND/OR operation
        throw new RuntimeException("Not implemented");
    }

    /**
     * Returns true if any of the on bit indexes of the specified collection are
     * matched by the on bits of this matrix. It is allowed that this matrix
     * have more on bits than the specified matrix.
     *
     * @param matrix
     * @return
     */
    public boolean any(TIntList onBits) {
        for (TIntIterator i = onBits.iterator(); i.hasNext();) {
            if (sparseMap.get(i.next())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if any of the on bit indexes of the specified matrix are
     * matched by the on bits of this matrix. It is allowed that this matrix
     * have more on bits than the specified matrix.
     *
     * @param matrix
     * @return
     */
    public boolean any(int[] onBits) {
        for (int i : onBits) {
            if (sparseMap.get(i)) {
                return true;
            }
        }
        return false;
    }

}
