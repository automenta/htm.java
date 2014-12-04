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
package org.numenta.nupic.model;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntDoubleHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.numenta.nupic.CLA;
import org.numenta.nupic.util.ArrayUtils;

/**
 * Convenience container for "bound" {@link Synapse} values which can be
 * dereferenced from both a Synapse and the {@link CLA} object. All
 * Synapses will have a reference to a {@code Pool} to retrieve relevant values.
 * In addition, that same pool can be referenced from the Connections object
 * externally which will update the Synapse's internal reference.
 *
 * @author David Ray
 * @see Synapse
 * @see CLA
 */
public class Pool {

    int size;

    final TIntArrayList synapseConnections = new TIntArrayList();

    /**
     * input index -> synapse, and synapse can be iterated by addition order
     */
    final LinkedHashMap<Integer, Synapse> synapseOrdering = new LinkedHashMap<>();

    /**
     * The permanence of a synapse represents a range of connectedness between
     * an axon and a dendrite.
     *
     * Biologically, the range would go from completely unconnected, to starting
     * to form a synapse but not connected yet, to a minimally connected
     * synapse, to a large fully connected synapse. The permanence of a synapse
     * is a scalar value ranging from 0.0 to 1.0. Learning involves incrementing
     * and decrementing a synapse?s permanence. When a synapse?s permanence is
     * above a threshold, it is connected with a weight of ?1?. When it is below
     * the threshold, it is unconnected with a weight of ?0?.
     */
    final TIntDoubleHashMap connectionPerms = new TIntDoubleHashMap();

    public Pool(int size) {
        this.size = size;
    }

    /**
     * Returns the permanence value for the {@link Synapse} specified.
     *
     * @param s	the Synapse
     * @return	the permanence
     */
    public double getPermanence(Synapse s) {
        return connectionPerms.get(s.getInputIndex());
    }

    /**
     * Sets the specified permanence value for the specified {@link Synapse}
     *
     * @param s
     * @param permanence
     */
    public void setPermanence(CLA c, Synapse s, double permanence) {

        int index = s.getInputIndex();        
        
        if (permanence < c.getSynPermDisconnected()) {
            disconnectSynapse(index);
            return;
        }
        
        boolean existed = connectionPerms.put(index, permanence) == connectionPerms.getNoEntryValue();
                
        if (!existed) {
            synapseOrdering.put(index, s);
        }

        if (permanence > c.getSynPermConnected()) {
            connectSynapse(index);
        }

        //TODO allow disconnection
    }

    /**
     * Resets the current connections in preparation for new permanence
     * adjustments.
     */
    public void resetConnections() {
        synapseConnections.clear();
    }

    /**
     * Returns the {@link Synapse} connected to the specified input bit index.
     *
     * @param inputIndex	the input vector connection's index.
     * @return
     */
    public Synapse getSynapse(int inputIndex) {
        return synapseOrdering.get(inputIndex);
    }

    /**
     * Returns an array of permanence values
     *
     * @return
     */
    public double[] getPermanencesSparse() {
        int i = 0;
        double[] retVal = new double[size];

        for (Synapse s : synapseOrdering.values()) {
            retVal[i++] = connectionPerms.get(s.getInputIndex());
        }
        
        return retVal;

    }

    /**
     * Returns the a dense array representing the potential pool permanences
     *
     * Note: Only called from tests for now...
     *
     * @param c
     * @return
     */
    public double[] getPermanencesDense(CLA c) {
        double[] retVal = new double[c.getNumInputs()];
		//Arrays.fill(retVal, 0); //already zero

        for (int inputIndex : connectionPerms.keys()) {
            retVal[inputIndex] = connectionPerms.get(inputIndex);
        }
        return retVal;
    }

    /**
     * Returns an array of input bit indexes.
     *
     * @return
     */
    public int[] getSparseConnections() {
        int[] c = connectionPerms.keys();
        ArrayUtils.reverse(c);
        return c;
    }

    /**
     * Returns the a dense array representing the potential pool bits with the
     * connected bits set to 1.
     *
     * Note: Only called from tests for now...
     *
     * @param c
     * @return
     */
    public int[] getConnectionsDense(CLA c) {
        int[] retVal = new int[c.getNumInputs()];
        //Arrays.fill(retVal, 0); //already zero
        int numSyn = synapseConnections.size();
        for (int i = 0; i < numSyn; i++) {
            retVal[synapseConnections.getQuick(i)] = 1;
        }
        return retVal;
    }

    /**
     * @return removed synapse, or null if none existed for the index
     */
    protected Synapse disconnectSynapse(int index) {
        Synapse existing = synapseOrdering.remove(index);
        if (existing!=null) {
            synapseConnections.remove(index);
            connectionPerms.remove(index);
        }
        return existing;
    }

    protected void connectSynapse(int index) {
        synapseConnections.add(index);
    }

    /**
     * Used internally to associated a {@link Synapse} with its current
     * permanence value.
     *
     * @author David Ray
     */
    /*private static class SynapsePair {
     public final Synapse synapse;
     double permanence;
		
     public SynapsePair(Synapse s, double p) {
     this.synapse = s;
     this.permanence = p;
     }
		
     public Synapse getSynapse() {
     return synapse;
     }
		
     public double getPermanence() {
     return permanence;
     }
		
     public void setPermanence(double permanence) {
     this.permanence = permanence;
     }
     }*/
}
