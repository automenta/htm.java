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

package org.numenta.nupic.examples.sp;

import java.util.Arrays;
import java.util.Random;

import org.numenta.nupic.CLA;
import org.numenta.nupic.Build;
import org.numenta.nupic.KEY;
import org.numenta.nupic.research.SpatialPooler;
import org.numenta.nupic.research.SpatialPooler.ColumnRadius;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.Condition;


/**
 * A simple program that demonstrates the working of the spatial pooler
 * 
 * @author Neal Miller
 */
public class HelloSP {
    private Build<SpatialPooler> spParam;
    private final SpatialPooler sp;
    private final CLA cla;
    private int[] inputArray;
    private final int[] activeArray;
    private int inputSize;
    private int columnNumber;
    
    /**
     * 
     * @param inputDimensions         The size of the input.  {m, n} will give a size of m x n
     * @param columnDimensions        The size of the 2 dimensional array of columns
     */
    HelloSP(int[] inputDimensions, int[] columnDimensions) {
        
        columnNumber = 1;
        for (int x : columnDimensions)
            columnNumber *= x;
        
        activeArray = new int[columnNumber];
        
        
        cla = new CLA(CLA.Default().
            set(KEY.INPUT_DIMENSIONS, inputDimensions).
            set(KEY.COLUMN_DIMENSIONS, columnDimensions)
        );
        
        sp = new SpatialPooler(cla, spParam = SpatialPooler.Default().
            set(KEY.POTENTIAL_RADIUS, ColumnRadius.factorOfNumInputs(1.0)).
            set(KEY.GLOBAL_INHIBITIONS, true).
            set(KEY.NUM_ACTIVE_COLUMNS_PER_INH_AREA, 0.02*columnNumber).
            set(KEY.SYN_PERM_ACTIVE_INC, 0.01).
            set(KEY.SYN_PERM_TRIM_THRESHOLD, 0.005)
        );
        
        inputSize = cla.getNumInputs();

        System.out.println(spParam);
    }
    
    public static void msg(String message) {
        for (int i = 0; i < 5; i++) System.out.print("-----");
        System.out.print(message);
        for (int i = 0; i < 5; i++) System.out.print("-----");
        System.out.println();
        
    }
    
    /**
     * Create a random input vector
     */
    public void createInput() {
        msg("Creating a random input vector");
        inputArray = ArrayUtils.randomInt(new Random(), inputSize, 2);
    }
    
    
    /**
     * Run the spatial pooler with the input vector
     */
    public int[] run() {
        
        msg("Computing the SDR");
        
        sp.compute(cla, inputArray, activeArray, true, true);        
        
        System.out.println(Arrays.toString(activeArray));
        int[] res = ArrayUtils.where(activeArray, Condition.GreaterThanZero);
        System.out.println(Arrays.toString(res));
        return res;
    }

    /**
     * Flip the value of a fraction of input bits (add noise)
     * @param noiseLevel        The percentage of total input bits that should be flipped
     */
    public void addNoise(double noiseLevel) {
        Random rand = new Random();
        for (int i = 0; i < noiseLevel*inputSize; i++) {
            int randomPosition = rand.nextInt(inputSize);
            // Flipping the bit at the randomly picked position
            inputArray[randomPosition] = 1 - inputArray[randomPosition];
        }
    }
    
    public static void main(String args[]) {
        HelloSP example = new HelloSP(new int[]{32, 32}, new int[]{64, 64});
        
        // Lesson 1
        msg("\n \nFollowing columns represent the SDR " +
        "Different set of columns each time since we randomize the input " +
        "Lesson - different input vectors give different SDRs\n\n");
        
        //Trying random vectors
        for (int i = 0; i < 3; i++) {
            example.createInput();
            example.run();
        }
        
        //Lesson 2
        System.out.println("\n\nIdentical SDRs because we give identical inputs");
        System.out.println("Lesson - identical inputs give identical SDRs\n\n");

        for (int i = 0; i < 75; i++) System.out.print("-");
        System.out.print("Using identical input vectors");
        for (int i = 0; i < 75; i++) System.out.print("-");
        System.out.println();
    
        //Trying identical vectors
        for (int i = 0; i < 2; i++) {
          example.run();
        }
        
        // Lesson 3
        System.out.println("\n\nNow we are changing the input vector slightly.");
        System.out.println("We change a small percentage of 1s to 0s and 0s to 1s.");
        System.out.println("The resulting SDRs are similar, but not identical to the original SDR");
        System.out.println("Lesson - Similar input vectors give similar SDRs\n\n");

        // Adding 10% noise to the input vector
        // Notice how the output SDR hardly changes at all
        for (int i = 0; i < 75; i++) System.out.print("-");
        System.out.print("After adding 10% noise to the input vector");
        for (int i = 0; i < 75; i++) System.out.print("-");
        example.addNoise(0.1);
        example.run();

        // Adding another 20% noise to the already modified input vector
        // The output SDR should differ considerably from that of the previous output
        for (int i = 0; i < 75; i++) System.out.print("-");
        System.out.print("After adding another 20% noise to the input vector");
        for (int i = 0; i < 75; i++) System.out.print("-");
        example.addNoise(0.2);
        example.run();
    }
}
