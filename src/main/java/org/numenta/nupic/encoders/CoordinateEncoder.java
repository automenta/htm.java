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
package org.numenta.nupic.encoders;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.MersenneTwisterFast;
import org.numenta.nupic.util.SortablePair;
import org.numenta.nupic.util.Tuple;

public class CoordinateEncoder extends Encoder<Tuple> implements CoordinateOrder {

    private static final MersenneTwisterFast random = new MersenneTwisterFast();

    /**
     * Package private to encourage construction using the Builder Pattern but
     * still allow inheritance.
     */
    CoordinateEncoder() {
    }

    /**
     * @see Encoder for more information
     */
    @Override
    public int getWidth() {
        return n;
    }

    /**
     * @see Encoder for more information
     */
    @Override
    public boolean isDelta() {
        return false;
    }

    /**
     * Returns a {@link List} of {@link Tuple}s containing [String:"name",
     * int:offset]
     *
     * @return List of Tuple(String, int)'s
     * @see Encoder for more information
     */
    @Override
    public List<Tuple> getDescription() {
        return Lists.newArrayList(new Tuple("coordinate", 0), new Tuple("coordinate", 1));
    }

    /**
     * Returns a builder for building ScalarEncoders. This builder may be reused
     * to produce multiple builders
     *
     * @return a {@code CoordinateEncoder.Builder}
     */
    public static Encoder.Builder<CoordinateEncoder.Builder, CoordinateEncoder> builder() {
        return new CoordinateEncoder.Builder();
    }

    /**
     * Returns coordinates around given coordinate, within given radius.
     * Includes given coordinate.
     *
     * @param coordinate	Coordinate whose neighbors to find
     * @param radius	Radius around `coordinate`
     * @return
     */
    public int[][] neighbors(int[] coordinate, double radius) {
        int[][] ranges = new int[coordinate.length][];
        for (int i = 0; i < coordinate.length; i++) {
            ranges[i] = ArrayUtils.range(coordinate[i] - (int) radius, coordinate[i] + (int) radius + 1);
        }

        int len = ranges.length == 1 ? 1 : ranges[0].length;
        int rows = ranges[0].length * len;
        int[][] retVal = new int[rows][];
        int r = 0;
        for (int k = 0; k < ranges[0].length; k++) {
            for (int j = 0; j < len; j++) {
                int[] entry = new int[ranges.length];
                entry[0] = ranges[0][k];
                for (int i = 1; i < ranges.length; i++) {
                    entry[i] = ranges[i][j];
                }
                retVal[r++] = entry;
            }
        }
        return retVal;
    }

    /**
     * Returns the top W coordinates by order.
     *
     * @param co	Implementation of {@link CoordinateOrder}
     * @param coordinates	A 2D array, where each element is a coordinate
     * @param w	(int) Number of top coordinates to return
     * @return
     */
    @SuppressWarnings("unchecked")
    public int[][] topWCoordinates(CoordinateOrder co, int[][] coordinates, int w) {
        SortablePair<Double, Integer>[] pairs = new SortablePair[coordinates.length];
        for (int i = 0; i < coordinates.length; i++) {
            pairs[i] = new SortablePair<>(co.orderForCoordinate(coordinates[i]), i);
        }

        Arrays.sort(pairs);

        int[][] topCoordinates = new int[w][];
        for (int i = 0, wIdx = pairs.length - w; i < w; i++, wIdx++) {
            int index = pairs[wIdx].second();
            topCoordinates[i] = coordinates[index];
        }
        return topCoordinates;
    }

    /**
     * Returns the order for a coordinate.
     *
     * @param coordinate	coordinate array
     *
     * @return	A value in the interval [0, 1), representing the order of the
     * coordinate
     */
    public double orderForCoordinate(int[] coordinate) {
        random.setSeed(coordinate);
        return random.nextDouble();
    }

    /**
     * Returns the order for a coordinate.
     *
     * @param coordinate	coordinate array
     * @param n	the number of available bits in the SDR
     *
     * @return	The index to a bit in the SDR
     */
    public static int bitForCoordinate(int[] coordinate, int n) {
        random.setSeed(coordinate);
        return random.nextInt(n);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void encodeIntoArray(Tuple inputData, int[] output) {
        int[][] neighbors = neighbors((int[]) inputData.get(0), (double) inputData.get(1));

        int[][] winners = topWCoordinates(this, neighbors, w);

        for (int i = 0; i < winners.length; i++) {
            int bit = bitForCoordinate(winners[i], n);
            output[bit] = 1;
        }
    }

    @Override
    public void setLearning(boolean learningEnabled) {
        super.setLearningEnabled(learningEnabled);
    }

    @Override
    public <T> List<T> getBucketValues(Class<T> returnType) {
        return null;
    }

    /**
     * Returns a {@code Builder} for constructing {@link CoordinateEncoder}s
     *
     * The base class architecture is put together in such a way where
     * boilerplate initialization can be kept to a minimum for implementing
     * subclasses, while avoiding the mistake-proneness of extremely long
     * argument lists.
     *
     * @see ScalarEncoder.Builder#setStuff(int)
     */
    public static class Builder extends Encoder.Builder<CoordinateEncoder.Builder, CoordinateEncoder> {

        private Builder() {
        }

        @Override
        public CoordinateEncoder build() {
			//Must be instantiated so that super class can initialize 
            //boilerplate variables.
            encoder = new CoordinateEncoder();

            //Call super class here
            super.build();

			////////////////////////////////////////////////////////
            //  Implementing classes would do setting of specific //
            //  vars here together with any sanity checking       //
            ////////////////////////////////////////////////////////
            if (w <= 0 || w % 2 == 0) {
                throw new IllegalArgumentException("w must be odd, and must be a positive integer");
            }

            if (n <= 6 * w) {
                throw new IllegalArgumentException(
                        "n must be an int strictly greater than 6*w. For "
                        + "good results we recommend n be strictly greater than 11*w");
            }

            if (name == null || name.equals("None")) {
                name = new StringBuilder("[").append(n).append(":").append(w).append("]").toString();
            }

            return (CoordinateEncoder) encoder;
        }
    }
}