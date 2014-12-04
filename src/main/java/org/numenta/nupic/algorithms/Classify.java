/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.numenta.nupic.algorithms;

/** input to the classifier compute */
public class Classify<O> {
    public final int bucketIdx;
    public final O value;

    public Classify(int bucketIndex, O value) {
        this.bucketIdx = bucketIndex;
        this.value = value;
    }

    public Classify(O value) {
        this(-1, value);
    }
    
}
