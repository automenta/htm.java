/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.numenta.nupic.algorithms;


/**
 * Generic classifier interface for hierarchical SDR (sparse distributed representation) systems
 */
public interface SDRClassifier {

    /**
     * Process one input sample. This method is called by outer loop code
     * outside the nupic-engine. We use this instead of the nupic engine
     * compute() because our inputs and outputs aren't fixed size vectors of
     * reals.
     *
     * @param time	Record number of this input pattern. Record numbers
     * should normally increase sequentially by 1 each time unless there are
     * missing records in the dataset. Knowing this information insures that we
     * don't get confused by missing records.
     * @param classify	classification information
     * @param patternNZ	list of the active indices from the output below
     * @param learn	if true, learn this sample
     * @param infer	if true, perform inference
     *
     * @return	dict containing inference results, there is one entry for each
     * step in self.steps, where the key is the number of steps, and the value
     * is an array containing the relative likelihood for each bucketIdx
     * starting from bucketIdx 0.
     *
     * There is also an entry containing the average actual value to use for
     * each bucket. The key is 'actualValues'.
     *
     * for example: { 1 : [0.1, 0.3, 0.2, 0.7], 4 : [0.2, 0.4, 0.3, 0.5],
     * 'actualValues': [1.5, 3,5, 5,5, 7.6], }
     */
    @SuppressWarnings(value = "unchecked")
    <T> Classification<T> compute(int time, Classify classify, int[] patternBelow, boolean learn, boolean infer);
    
}
