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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.numenta.nupic.Build;

import org.numenta.nupic.CLA;
import org.numenta.nupic.KEY;
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.Column;
import org.numenta.nupic.model.DistalDendrite;
import org.numenta.nupic.model.Synapse;
import org.numenta.nupic.util.SparseObjectMatrix;

/**
 * Temporal Memory implementation in Java
 *
 * @author Chetan Surpur
 * @author David Ray
 */
public class TemporalMemory {

    public final static Build<TemporalMemory> Default() {
        return new Build<>(new HashMap() {
            {

                put(KEY.ACTIVATION_THRESHOLD, 13d);
                put(KEY.LEARNING_RADIUS, 2048);
                put(KEY.MIN_THRESHOLD, 10d);
                put(KEY.MAX_NEW_SYNAPSE_COUNT, 20);
                put(KEY.INITIAL_PERMANENCE, 0.21);
                put(KEY.CONNECTED_PERMANENCE, 0.5);
                put(KEY.PERMANENCE_INCREMENT, 0.10);
                put(KEY.PERMANENCE_DECREMENT, 0.10);
                put(KEY.TM_VERBOSITY, 0);
            }
        });
    }
    private CLA cla;

    /**
     * Constructs a new {@code TemporalMemory}
     */
    public TemporalMemory() {
    }

    public TemporalMemory(CLA c, Build<TemporalMemory> param) {
        super();
        param.apply(this);
        init(c);
    }

    /**
     * Uses the specified {@link CLA} object to Build the structural anatomy
     * needed by this {@code TemporalMemory} to implement its algorithms.
     *
     * @param	c	{@link CLA} object
     */
    protected void init(CLA c) {
        this.cla = c;
        
        SparseObjectMatrix<Column> matrix = c.getMemory() == null
                ? new SparseObjectMatrix<Column>(c.getColumnDimensions())
                : c.getMemory();
        c.setMemory(matrix);

        int numColumns = matrix.getMaxIndex() + 1;
        int cellsPerColumn = c.getCellsPerColumn();
        Cell[] cells = new Cell[numColumns * cellsPerColumn];

        //Used as flag to determine if Column objects have been created.
        Column colZero = matrix.getIndex(0);
        for (int i = 0; i < numColumns; i++) {
            Column column = colZero == null
                    ? new Column(cellsPerColumn, i) : matrix.getIndex(i);
            for (int j = 0; j < cellsPerColumn; j++) {
                cells[i * cellsPerColumn + j] = column.getCell(j);
            }
            //If columns have not been previously configured
            if (colZero == null) {
                matrix.set(column, i);
            }
        }
        //Only the TemporalMemory initializes cells so no need to test 
        c.setCells(cells);
    }

    /////////////////////////// CORE FUNCTIONS /////////////////////////////
    /**
     * Feeds input record through TM, performing inferencing and learning
     *
     * @param connections	the connection memory
     * @param activeColumns direct proximal dendrite input
     * @param learn learning mode flag
     * @return {@link ComputeCycle} container for one cycle of inference values.
     */
    public ComputeCycle compute(CLA connections, int[] activeColumns, boolean learn) {
        ComputeCycle result = computeFn(connections, connections.getColumnSet(activeColumns), new LinkedHashSet<>(connections.getPredictiveCells()),
                new LinkedHashSet<>(connections.getActiveSegments()), new LinkedHashMap<>(connections.getActiveSynapsesForSegment()),
                new LinkedHashSet<>(connections.getWinnerCells()), learn);

        connections.setActiveCells(result.activeCells());
        connections.setWinnerCells(result.winnerCells());
        connections.setPredictiveCells(result.predictiveCells());
        connections.setPredictedColumns(result.predictedColumns());
        connections.setActiveSegments(result.activeSegments());
        connections.setLearningSegments(result.learningSegments());
        connections.setActiveSynapsesForSegment(result.activeSynapsesForSegment());

        return result;
    }

    /**
     * Functional version of {@link #compute(int[], boolean)}. This method is
     * stateless and concurrency safe.
     *
     * @param c {@link CLA} object containing state of memory members
     * @param activeColumns proximal dendrite input
     * @param prevPredictiveCells cells predicting in t-1
     * @param prevActiveSegments active segments in t-1
     * @param prevActiveSynapsesForSegment {@link Synapse}s active in t-1
     * @param prevWinnerCells ` previous winners
     * @param learn whether mode is "learning" mode
     * @return
     */
    public ComputeCycle computeFn(CLA c, Set<Column> activeColumns, Set<Cell> prevPredictiveCells, Set<DistalDendrite> prevActiveSegments,
            Map<DistalDendrite, Set<Synapse>> prevActiveSynapsesForSegment, Set<Cell> prevWinnerCells, boolean learn) {

        ComputeCycle cycle = new ComputeCycle();

        activateCorrectlyPredictiveCells(cycle, prevPredictiveCells, activeColumns);

        burstColumns(cycle, c, activeColumns, cycle.predictedColumns, prevActiveSynapsesForSegment);

        if (learn) {
            learnOnSegments(c, prevActiveSegments, cycle.learningSegments, prevActiveSynapsesForSegment, cycle.winnerCells, prevWinnerCells);
        }

        cycle.activeSynapsesForSegment = computeActiveSynapses(c, cycle.activeCells);

        computePredictiveCells(c, cycle, cycle.activeSynapsesForSegment);

        return cycle;
    }

    /**
     * Phase 1: Activate the correctly predictive cells
     *
     * Pseudocode:
     *
     * - for each prev predictive cell - if in active column - mark it as active
     * - mark it as winner cell - mark column as predicted
     *
     * @param c ComputeCycle interim values container
     * @param prevPredictiveCells predictive {@link Cell}s predictive cells in
     * t-1
     * @param activeColumns active columns in t
     */
    public void activateCorrectlyPredictiveCells(ComputeCycle c, Set<Cell> prevPredictiveCells, Set<Column> activeColumns) {
        for (Cell cell : prevPredictiveCells) {
            Column column = cell.getParentColumn();
            if (activeColumns.contains(column)) {
                c.activeCells.add(cell);
                c.winnerCells.add(cell);
                c.predictedColumns.add(column);
            }
        }
    }

    /**
     * Phase 2: Burst unpredicted columns.
     *
     * Pseudocode:
     *
     * - for each unpredicted active column - mark all cells as active - mark
     * the best matching cell as winner cell - (learning) - if it has no
     * matching segment - (optimization) if there are prev winner cells - add a
     * segment to it - mark the segment as learning
     *
     * @param cycle ComputeCycle interim values container
     * @param c Connections temporal memory state
     * @param activeColumns active columns in t
     * @param predictedColumns predicted columns in t
     * @param prevActiveSynapsesForSegment LinkedHashMap of previously active
     * segments which have had synapses marked as active in t-1
     */
    public void burstColumns(ComputeCycle cycle, CLA c, Set<Column> activeColumns, Set<Column> predictedColumns,
            Map<DistalDendrite, Set<Synapse>> prevActiveSynapsesForSegment) {

        Set<Column> unpred = new LinkedHashSet<>(activeColumns);

        unpred.removeAll(predictedColumns);
        for (Column column : unpred) {
            List<Cell> cells = column.getCells();
            cycle.activeCells.addAll(cells);

            Object[] bestSegmentAndCell = getBestMatchingCell(c, column, prevActiveSynapsesForSegment);
            DistalDendrite bestSegment = (DistalDendrite) bestSegmentAndCell[0];
            Cell bestCell = (Cell) bestSegmentAndCell[1];
            if (bestCell != null) {
                cycle.winnerCells.add(bestCell);
            }

            int segmentCounter = c.getSegmentCount();
            if (bestSegment == null) {
                bestSegment = bestCell.createSegment(c, segmentCounter);
                c.setSegmentCount(segmentCounter + 1);
            }

            cycle.learningSegments.add(bestSegment);
        }
    }

    /**
     * Phase 3: Perform learning by adapting segments.
     * <pre>
     * Pseudocode:
     *
     * - (learning) for each prev active or learning segment
     *   - if learning segment or from winner cell
     *   - strengthen active synapses
     *   - weaken inactive synapses
     *   - if learning segment
     *   - add some synapses to the segment
     *     - subsample from prev winner cells
     * </pre>
     *
     * @param c the CLA state of the temporal memory
     * @param prevActiveSegments	the Set of segments active in the previous
     * cycle.
     * @param learningSegments	the Set of segments marked as learning
     * {@link #burstColumns(ComputeCycle, CLA, Set, Set, Map)}
     * @param prevActiveSynapseSegments	the map of segments which were
     * previously active to their associated {@link Synapse}s.
     * @param winnerCells	the Set of all winning cells ({@link Cell}s with the
     * most active synapses)
     * @param prevWinnerCells	the Set of cells which were winners during the
     * last compute cycle
     */
    public void learnOnSegments(CLA c, Set<DistalDendrite> prevActiveSegments, Set<DistalDendrite> learningSegments,
            Map<DistalDendrite, Set<Synapse>> prevActiveSynapseSegments, Set<Cell> winnerCells, Set<Cell> prevWinnerCells) {

        double permanenceIncrement = c.getPermanenceIncrement();
        double permanenceDecrement = c.getPermanenceDecrement();

        List<DistalDendrite> prevAndLearning = new ArrayList<>(prevActiveSegments);
        prevAndLearning.addAll(learningSegments);

        for (DistalDendrite dd : prevAndLearning) {
            boolean isLearningSegment = learningSegments.contains(dd);
            boolean isFromWinnerCell = winnerCells.contains(dd.getParentCell());

            Set<Synapse> activeSynapses = new LinkedHashSet<>(dd.getConnectedActiveSynapses(prevActiveSynapseSegments, 0));

            if (isLearningSegment || isFromWinnerCell) {
                dd.adaptSegment(c, activeSynapses, permanenceIncrement, permanenceDecrement);
            }

            int synapseCounter = c.getSynapseCount();
            if (isLearningSegment) {
                int n = c.getMaxNewSynapseCount() - activeSynapses.size();
                Set<Cell> learnCells = dd.pickCellsToLearnOn(c, n, prevWinnerCells, c.getRandom());
                for (Cell sourceCell : learnCells) {
                    dd.createSynapse(c, sourceCell, c.getInitialPermanence(), synapseCounter);
                    synapseCounter += 1;
                }
                c.setSynapseCount(synapseCounter);
            }
        }
    }

    /**
     * Phase 4: Compute predictive cells due to lateral input on distal
     * dendrites.
     *
     * Pseudocode:
     *
     * - for each distal dendrite segment with activity >= activationThreshold -
     * mark the segment as active - mark the cell as predictive
     *
     * @param c the Connections state of the temporal memory
     * @param cycle	the state during the current compute cycle
     * @param activeSegments
     */
    public void computePredictiveCells(CLA c, ComputeCycle cycle, Map<DistalDendrite, Set<Synapse>> activeDendrites) {
        for (DistalDendrite dd : activeDendrites.keySet()) {
            Set<Synapse> connectedActive = dd.getConnectedActiveSynapses(activeDendrites, c.getConnectedPermanence());
            if (connectedActive.size() >= c.getActivationThreshold()) {
                cycle.activeSegments.add(dd);
                cycle.predictiveCells.add(dd.getParentCell());
            }
        }
    }

    /**
     * Forward propagates activity from active cells to the synapses that touch
     * them, to determine which synapses are active.
     *
     * @param c the connections state of the temporal memory
     * @param cellsActive
     * @return
     */
    public Map<DistalDendrite, Set<Synapse>> computeActiveSynapses(CLA c, Set<Cell> cellsActive) {
        Map<DistalDendrite, Set<Synapse>> activesSynapses = new LinkedHashMap<>();

        for (Cell cell : cellsActive) {
            for (Synapse s : cell.getReceptorSynapses(c)) {
                Set<Synapse> set = null;
                if ((set = activesSynapses.get(s.getSegment())) == null) {
                    activesSynapses.put((DistalDendrite) s.getSegment(), set = new LinkedHashSet<>());
                }
                set.add(s);
            }
        }

        return activesSynapses;
    }

    /**
     *  call reset
     */
    public void clear() {
        if (this.cla == null)
            throw new RuntimeException(this + " has no associated CLA instance");
        clear(this.cla);
    }
    
    /**
     * Called to start the input of a new sequence.
     *
     * @param cla the Connections state of the temporal memory
     */
    public void clear(CLA cla) {
        this.cla = cla;
        
        cla.getActiveCells().clear();
        cla.getPredictiveCells().clear();
        cla.getActiveSegments().clear();
        cla.getActiveSynapsesForSegment().clear();
        cla.getWinnerCells().clear();
        
        init(cla); //TODO find if this isnt necessary sometimes
    }

    /////////////////////////// HELPER FUNCTIONS ///////////////////////////
    /**
     * Gets the cell with the best matching segment (see
     * `TM.getBestMatchingSegment`) that has the largest number of active
     * synapses of all best matching segments.
     *
     * @param c	encapsulated memory and state
     * @param column	{@link Column} within which to search for best cell
     * @param prevActiveSynapsesForSegment	a {@link DistalDendrite}'s previously
     * active {@link Synapse}s
     * @return	an object array whose first index contains a segment, and the
     * second contains a cell
     */
    public Object[] getBestMatchingCell(CLA c, Column column, Map<DistalDendrite, Set<Synapse>> prevActiveSynapsesForSegment) {
        Object[] retVal = new Object[2];
        Cell bestCell = null;
        DistalDendrite bestSegment = null;
        int maxSynapses = 0;
        for (Cell cell : column.getCells()) {
            DistalDendrite dd = getBestMatchingSegment(c, cell, prevActiveSynapsesForSegment);
            if (dd != null) {
                Set<Synapse> connectedActiveSynapses = dd.getConnectedActiveSynapses(prevActiveSynapsesForSegment, 0);
                if (connectedActiveSynapses.size() > maxSynapses) {
                    maxSynapses = connectedActiveSynapses.size();
                    bestCell = cell;
                    bestSegment = dd;
                }
            }
        }

        if (bestCell == null) {
            bestCell = column.getLeastUsedCell(c, c.getRandom());
        }

        retVal[0] = bestSegment;
        retVal[1] = bestCell;
        return retVal;
    }

    public static final UnivariateFunction GreaterThanZero = new UnivariateFunction() {
        @Override
        public double value(final double d) {
            if (d > 0) {
                return 1.0;
            }
            return 0.0;
        }
    };

    /**
     * Gets the segment on a cell with the largest number of activate synapses,
     * including all synapses with non-zero permanences.
     *
     * @param c	encapsulated memory and state
     * @param column	{@link Column} within which to search for best cell
     * @param activeSynapseSegments	a {@link DistalDendrite}'s active
     * {@link Synapse}s
     * @return	the best segment
     */
    public DistalDendrite getBestMatchingSegment(CLA c, Cell cell, Map<DistalDendrite, Set<Synapse>> activeSynapseSegments) {

        double maxSynapses = c.getMinThreshold();

        DistalDendrite bestSegment = null;
        for (DistalDendrite dd : cell.getSegments(c)) {
            //Set<Synapse> activeSyns = dd.getConnectedActiveSynapses(activeSynapseSegments, 0);
            double synActivation = dd.getConnectedSynapseActivation(activeSynapseSegments, GreaterThanZero);

            if (synActivation >= maxSynapses) {
                maxSynapses = synActivation;
                bestSegment = dd;
            }
        }

        return bestSegment;
    }

    /**
     * Returns the column index given the cells per column and the cell index
     * passed in.
     *
     * @param c	{@link CLA} memory
     * @param cellIndex	the index where the requested cell resides
     * @return
     */
    protected int columnForCell(CLA c, int cellIndex) {
        return cellIndex / c.getCellsPerColumn();
    }

    /**
     * Returns the cell at the specified index.
     *
     * @param index
     * @return
     */
    public Cell getCell(CLA c, int index) {
        return c.getCells()[index];
    }

    /**
     * Returns a {@link LinkedHashSet} of {@link Cell}s from a sorted array of
     * cell indexes.
     *
     * @param`c	the {@link CLA} object
     * @param cellIndexes indexes of the {@link Cell}s to return
     * @return
     */
    public LinkedHashSet<Cell> getCells(CLA c, int[] cellIndexes) {
        LinkedHashSet<Cell> cellSet = new LinkedHashSet<>();
        for (int cell : cellIndexes) {
            cellSet.add(getCell(c, cell));
        }
        return cellSet;
    }

    /**
     * Returns a {@link LinkedHashSet} of {@link Column}s from a sorted array of
     * Column indexes.
     *
     * @param cellIndexes indexes of the {@link Column}s to return
     * @return
     */
    public LinkedHashSet<Column> getColumns(CLA c, int[] columnIndexes) {
        return c.getColumnSet(columnIndexes);
    }
}
