/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.numenta.nupic.util;

/**
 * Matrix with additional access methods by 1D index
 */
public interface IndexedMatrix<T> extends IMatrix<T> {

    public int[] computeCoordinates(int index);
    
    public int[] get1DIndexes();
    
    public T getIndex(final int index);

    public void setIndex(T object, int index);    
    
    public int[] getSparseIndices();    
    
    public int[] getDimensionMultiples();

    public int computeIndex(int[] coordinates);
    
}
