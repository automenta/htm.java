/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.numenta.nupic.util;

import gnu.trove.list.array.TIntArrayList;

/**
 * TIntArrayList that caches hash code when constructed
 */
public class IntTuple extends TIntArrayList implements ITuple<Integer> {
    
    private final int hash;

    public IntTuple(int... values) {
        super(values);
        
        hash = super.hashCode();
    }
    
    @Override
    public Integer the(int index) {
        return super.getQuick(index);
    }

    @Override
    public int hashCode() {
        return hash;
    }
    
    
}
