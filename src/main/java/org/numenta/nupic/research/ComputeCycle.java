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

package org.numenta.nupic.research;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.numenta.nupic.CLA;
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.Column;
import org.numenta.nupic.model.DistalDendrite;
import org.numenta.nupic.model.Synapse;

/**
 * Contains a snapshot of the state attained during one computational
 * call to the {@link TemporalMemory}. The {@code TemporalMemory} uses
 * data from previous compute cycles to derive new data for the current cycle
 * through a comparison between states of those different cycles, therefore
 * this state container is necessary.
 * 
 * TODO cells and segment collections can be combined using an inner class
 * or array to hold their associated state.  this should improve performance
 * 
 * @author David Ray
 */
public class ComputeCycle {
    
    public final Set<Cell> activeCells;
    public final Set<Cell> winnerCells;
    public final Set<Cell> predictiveCells;
    public final Set<Column> predictedColumns;
    public final Set<DistalDendrite> activeSegments;
    public final Set<DistalDendrite> learningSegments;
    public final Map<DistalDendrite, Set<Synapse>> activeSynapsesForSegment;
    
    
    /**
     * Constructs a new {@code ComputeCycle}
     */
    public ComputeCycle() {
        activeCells = new LinkedHashSet<>();
        winnerCells = new LinkedHashSet<>();
        predictiveCells = new LinkedHashSet<>();
        predictedColumns = new LinkedHashSet<>();
        activeSegments = new LinkedHashSet<>();
        learningSegments = new LinkedHashSet<>();
        activeSynapsesForSegment = new LinkedHashMap<>();    
    }
    
    /**
     * Constructs a new {@code ComputeCycle} initialized with
     * the connections relevant to the current calling {@link Thread} for
     * the specified {@link TemporalMemory}
     * 
     * @param   c       the current connections state of the TemporalMemory
     */
    public ComputeCycle(CLA c) {
        this.activeCells = new LinkedHashSet<>(c.getActiveCells());
        this.winnerCells = new LinkedHashSet<>(c.getWinnerCells());
        this.predictiveCells = new LinkedHashSet<>(c.getPredictiveCells());
        this.predictedColumns = new LinkedHashSet<>(c.getPredictedColumns());
        this.activeSegments = new LinkedHashSet<>(c.getActiveSegments());
        this.learningSegments = new LinkedHashSet<>(c.getLearningSegments());
        this.activeSynapsesForSegment = new LinkedHashMap<>(c.getActiveSynapsesForSegment());
    }
    
    /** resets the compute cycle so it may be re-used */
    void clear() {
        
        activeCells.clear();
        winnerCells.clear();
        this.predictiveCells.clear();
        
        this.predictedColumns.clear();
        
        this.activeSegments.clear();
        this.learningSegments.clear();
        
        this.activeSynapsesForSegment.clear();        
    }

    
    /**
     * Returns the current {@link Set} of active cells
     * 
     * @return  the current {@link Set} of active cells
     */
    public Set<Cell> activeCells() {
        return activeCells;
    }
    
    /**
     * Returns the current {@link Set} of winner cells
     * 
     * @return  the current {@link Set} of winner cells
     */
    public Set<Cell> winnerCells() {
        return winnerCells;
    }
    
    /**
     * Returns the {@link Set} of predictive cells.
     * @return
     */
    public Set<Cell> predictiveCells() {
        return predictiveCells;
    }
    
    /**
     * Returns the current {@link Set} of predicted columns
     * 
     * @return  the current {@link Set} of predicted columns
     */
    public Set<Column> predictedColumns() {
        return predictedColumns;
    }
    
    /**
     * Returns the Set of learning {@link DistalDendrite}s
     * @return
     */
    public Set<DistalDendrite> learningSegments() {
        return learningSegments;
    }
    
    /**
     * Returns the Set of active {@link DistalDendrite}s
     * @return
     */
    public Set<DistalDendrite> activeSegments() {
        return activeSegments;
    }
    
    /**
     * Returns the mapping of Segments to active synapses in t-1
     * @return
     */
    public Map<DistalDendrite, Set<Synapse>> activeSynapsesForSegment() {
        return activeSynapsesForSegment;
    }

}
