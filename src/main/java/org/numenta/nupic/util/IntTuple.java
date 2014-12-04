/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.numenta.nupic.util;

import gnu.trove.list.array.TIntArrayList;

/**
 *
 * @author me
 */
public class IntTuple extends TIntArrayList implements ITuple<Integer> {

    public IntTuple(int... values) {
        super(values);
    }
    
    @Override
    public Integer the(int index) {
        return super.getQuick(index);
    }
    
}
