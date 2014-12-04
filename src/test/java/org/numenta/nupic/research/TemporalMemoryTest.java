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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.Before;

import org.junit.Test;
import org.numenta.nupic.CLA;
import static org.numenta.nupic.CLA.Default;
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.Column;
import org.numenta.nupic.model.DistalDendrite;
import org.numenta.nupic.model.Synapse;


/**
 * Basic unit test for {@link TemporalMemory}
 * 
 * @author Chetan Surpur
 * @author David Ray
 */
public class TemporalMemoryTest {
    private CLA cla;
    private TemporalMemory tm;

    @Before
    public void start() {        
        cla = new CLA(Default());
        tm = new TemporalMemory(cla, TemporalMemory.Default());        
    }
    
    @Test
    public void testActivateCorrectlyPredictiveCells() {
        
        
        
        ComputeCycle c = new ComputeCycle();
        
        int[] prevPredictiveCells = new int[] { 0, 237, 1026, 26337, 26339, 55536 };
        int[] activeColumns = new int[] { 32, 47, 823 };
        
        tm.activateCorrectlyPredictiveCells(c, cla.getCellSet(prevPredictiveCells), cla.getColumnSet(activeColumns));
        Set<Cell> activeCells = cla.getActiveCells();
        Set<Cell> winnerCells = cla.getWinnerCells();
        Set<Column> predictedColumns = cla.getPredictedColumns();
        
        int[] expectedActiveWinners = new int[] { 1026, 26337, 26339 };
        int[] expectedPredictCols = new int[] { 32, 823 };
        int idx = 0;
        for(Cell cell : activeCells) {
            assertEquals(expectedActiveWinners[idx++], cell.getIndex());
        }
        idx = 0;
        for(Cell cell : winnerCells) {
            assertEquals(expectedActiveWinners[idx++], cell.getIndex());
        }
        idx = 0;
        for(Column col : predictedColumns) {
            assertEquals(expectedPredictCols[idx++], col.getIndex());
        }
    }
    
    @Test
    public void testActivateCorrectlyPredictiveCellsEmpty() {
        
        ComputeCycle c = new ComputeCycle();
        
        int[] prevPredictiveCells = new int[] {};
        int[] activeColumns = new int[] { 32, 47, 823 };
        
        tm.activateCorrectlyPredictiveCells(c, cla.getCellSet(prevPredictiveCells), cla.getColumnSet(activeColumns));
        Set<Cell> activeCells = c.activeCells();
        Set<Cell> winnerCells = c.winnerCells();
        Set<Column> predictedColumns = c.predictedColumns();
        
        assertTrue(activeCells.isEmpty());
        assertTrue(winnerCells.isEmpty());
        assertTrue(predictedColumns.isEmpty());
        
        //---
        
        prevPredictiveCells = new int[] { 0, 237, 1026, 26337, 26339, 55536 };
        activeColumns = new int[] {};
        tm.activateCorrectlyPredictiveCells(c, cla.getCellSet(prevPredictiveCells), cla.getColumnSet(activeColumns));
        activeCells = c.activeCells();
        winnerCells = c.winnerCells();
        predictedColumns = c.predictedColumns();
        
        assertTrue(activeCells.isEmpty());
        assertTrue(winnerCells.isEmpty());
        assertTrue(predictedColumns.isEmpty());
    }
    
    @Test
    public void testBurstColumns() {
        cla.setCellsPerColumn(4);
        cla.setConnectedPermanence(0.50);
        cla.setMinThreshold(1);
        cla.setSeed(42);
        tm.clear();
        
        int segmentCounter = 0;
        int synapseCounter = 0;
        
        DistalDendrite dd = cla.getCell(0).createSegment(cla, segmentCounter++);
        Synapse s0 = dd.createSynapse(cla, cla.getCell(23), 0.6, synapseCounter++);
        Synapse s1 = dd.createSynapse(cla, cla.getCell(37), 0.4, synapseCounter++);
        dd.createSynapse(cla, cla.getCell(477), 0.9, synapseCounter++);
        
        DistalDendrite dd2 = cla.getCell(0).createSegment(cla, segmentCounter++);
        Synapse s2 = dd2.createSynapse(cla, cla.getCell(49), 0.9, synapseCounter++);
        dd2.createSynapse(cla, cla.getCell(3), 0.8, synapseCounter++);
        
        DistalDendrite dd3 = cla.getCell(1).createSegment(cla, segmentCounter++);
        Synapse s3 = dd3.createSynapse(cla, cla.getCell(733), 0.7, synapseCounter++);
        
        DistalDendrite dd4 = cla.getCell(108).createSegment(cla, segmentCounter++);
        dd4.createSynapse(cla, cla.getCell(486), 0.9, synapseCounter++);
        
        int[] activeColumns = new int[] { 0, 1, 26 };
        int[] predictedColumns = new int[] {26};
        
        Map<DistalDendrite, Set<Synapse>> activeSynapseSegments = new LinkedHashMap<>();
        activeSynapseSegments.put(dd, new LinkedHashSet<>(Arrays.asList(new Synapse[] { s0, s1 })));
        activeSynapseSegments.put(dd2, new LinkedHashSet<>(Arrays.asList(new Synapse[] { s2 })));
        activeSynapseSegments.put(dd3, new LinkedHashSet<>(Arrays.asList(new Synapse[] { s3 })));
        
        ComputeCycle cycle = new ComputeCycle();
        tm.burstColumns(cycle, cla, cla.getColumnSet(activeColumns), cla.getColumnSet(predictedColumns), activeSynapseSegments);
        
        List<Cell> activeCells = new ArrayList<>(cycle.activeCells());
        List<Cell> winnerCells = new ArrayList<>(cycle.winnerCells());
        List<DistalDendrite> learningSegments = new ArrayList<>(cycle.learningSegments());
        
        assertEquals(8, activeCells.size());
        for(int i = 0;i < 8;i++) {
            assertEquals(i, activeCells.get(i).getIndex());
        }
        assertEquals(0, winnerCells.get(0).getIndex());
        assertEquals(5, winnerCells.get(1).getIndex());
        
        assertEquals(dd, learningSegments.get(0));
        //Test that one of the learning Dendrites was created during call to burst...
        assertTrue(!dd.equals(learningSegments.get(1)));
        assertTrue(!dd2.equals(learningSegments.get(1)));
        assertTrue(!dd3.equals(learningSegments.get(1)));
        assertTrue(!dd4.equals(learningSegments.get(1)));
    }
    
    @Test
    public void testBurstColumnsEmpty() {
        cla.setCellsPerColumn(4);
        cla.setConnectedPermanence(0.50);
        cla.setMinThreshold(1);
        cla.setSeed(42);
        tm.clear();
        
        CLA c = cla;
        
        int[] activeColumns = new int[] {};
        int[] predictedColumns = new int[] {};
        
        Map<DistalDendrite, Set<Synapse>> activeSynapseSegments = new LinkedHashMap<>();
        
        ComputeCycle cycle = new ComputeCycle();
        tm.burstColumns(cycle, c, cla.getColumnSet(activeColumns), cla.getColumnSet(predictedColumns), activeSynapseSegments);
        
        List<Cell> activeCells = new ArrayList<>(c.getActiveCells());
        List<Cell> winnerCells = new ArrayList<>(c.getWinnerCells());
        List<DistalDendrite> learningSegments = new ArrayList<>(c.getLearningSegments());
        
        assertEquals(0, activeCells.size());
        assertEquals(0, winnerCells.size());
        assertEquals(0, learningSegments.size());
    }
    
    @Test
    public void testLearnOnSegments() {
        cla.setMaxNewSynapseCount(2);
        tm.clear();
        
        int segmentCounter = 0;
        int synapseCounter = 0;
        
        DistalDendrite dd = cla.getCell(0).createSegment(cla, segmentCounter++);
        Synapse s0 = dd.createSynapse(cla, cla.getCell(23), 0.6, synapseCounter++);
        Synapse s1 = dd.createSynapse(cla, cla.getCell(37), 0.4, synapseCounter++);
        Synapse s2 = dd.createSynapse(cla, cla.getCell(477), 0.9, synapseCounter++);
        
        DistalDendrite dd1 = cla.getCell(1).createSegment(cla, segmentCounter++);
        Synapse s3 = dd1.createSynapse(cla, cla.getCell(733), 0.7, synapseCounter++);
        
        DistalDendrite dd2 = cla.getCell(8).createSegment(cla, segmentCounter++);
        Synapse s4 = dd2.createSynapse(cla, cla.getCell(486), 0.9, synapseCounter++);
        
        DistalDendrite dd3 = cla.getCell(100).createSegment(cla, segmentCounter++);
        
        Set<DistalDendrite> prevActiveSegments = new LinkedHashSet<>();
        prevActiveSegments.add(dd);
        prevActiveSegments.add(dd2);
        
        Map<DistalDendrite, Set<Synapse>> prevActiveSynapsesForSegment = new LinkedHashMap<>();
        prevActiveSynapsesForSegment.put(dd, new LinkedHashSet<>(Arrays.asList(new Synapse[] { s0, s1 })));
        prevActiveSynapsesForSegment.put(dd1, new LinkedHashSet<>(Arrays.asList(new Synapse[] { s3 })));
        
        Set<DistalDendrite> learningSegments = new LinkedHashSet<>();
        learningSegments.add(dd1);
        learningSegments.add(dd3);
        Set<Cell> winnerCells = new LinkedHashSet<>();
        winnerCells.add(cla.getCell(0));
        Set<Cell> prevWinnerCells = new LinkedHashSet<>();
        prevWinnerCells.add(cla.getCell(10));
        prevWinnerCells.add(cla.getCell(11));
        prevWinnerCells.add(cla.getCell(12));
        prevWinnerCells.add(cla.getCell(13));
        prevWinnerCells.add(cla.getCell(14));
        
        ///////////////// Validate State Before and After //////////////////////
        
        //Before
        
        //Check segment 0
        assertEquals(0.6, s0.getPermanence(), 0.01);
        assertEquals(0.4, s1.getPermanence(), 0.01);
        assertEquals(0.9, s2.getPermanence(), 0.01);
        
        //Check segment 1
        assertEquals(0.7, s3.getPermanence(), 0.01);
        assertEquals(1, dd1.getAllSynapses(cla).size(), 0);
        
        //Check segment 2
        assertEquals(0.9, s4.getPermanence(), 0.01);
        assertEquals(1, dd2.getAllSynapses(cla).size(), 0);
        
        //Check segment 3
        assertEquals(0, dd3.getAllSynapses(cla).size(), 0);
        
        tm.learnOnSegments(cla, prevActiveSegments, learningSegments, prevActiveSynapsesForSegment, winnerCells, prevWinnerCells);
        
        //After
        
        //Check segment 0
        assertEquals(0.7, s0.getPermanence(), 0.01); //was 0.6
        assertEquals(0.5, s1.getPermanence(), 0.01); //was 0.4
        assertEquals(0.8, s2.getPermanence(), 0.01); //was 0.9
        
        //Check segment 1
        assertEquals(0.8, s3.getPermanence(), 0.01); //was 0.7
        assertEquals(2, dd1.getAllSynapses(cla).size(), 0); // was 1
        
        //Check segment 2
        assertEquals(0.9, s4.getPermanence(), 0.01); //unchanged
        assertEquals(1, dd2.getAllSynapses(cla).size(), 0); //unchanged
        
        //Check segment 3
        assertEquals(2, dd3.getAllSynapses(cla).size(), 0);// was 0
        
    }
    
    @SuppressWarnings("unused")
    @Test
    public void testComputePredictiveCells() {
        cla.setActivationThreshold(2);
        tm.clear();
        
        int segmentCounter = 0;
        int synapseCounter = 0;
        
        DistalDendrite dd = cla.getCell(0).createSegment(cla, segmentCounter++);
        Synapse s0 = dd.createSynapse(cla, cla.getCell(23), 0.6, synapseCounter++);
        Synapse s1 = dd.createSynapse(cla, cla.getCell(37), 0.5, synapseCounter++);
        Synapse s2 = dd.createSynapse(cla, cla.getCell(477), 0.9, synapseCounter++);
        
        DistalDendrite dd1 = cla.getCell(1).createSegment(cla, segmentCounter++);
        Synapse s3 = dd1.createSynapse(cla, cla.getCell(733), 0.7, synapseCounter++);
        Synapse s4 = dd1.createSynapse(cla, cla.getCell(733), 0.4, synapseCounter++);
        
        DistalDendrite dd2 = cla.getCell(1).createSegment(cla, segmentCounter++);
        Synapse s5 = dd2.createSynapse(cla, cla.getCell(974), 0.9, synapseCounter++);
        
        DistalDendrite dd3 = cla.getCell(8).createSegment(cla, segmentCounter++);
        Synapse s6 = dd3.createSynapse(cla, cla.getCell(486), 0.9, synapseCounter++);
        
        DistalDendrite dd4 = cla.getCell(100).createSegment(cla, segmentCounter++);
        
        Map<DistalDendrite, Set<Synapse>> activeSynapseSegments = new LinkedHashMap<>();
        activeSynapseSegments.put(dd, new LinkedHashSet<>(Arrays.asList(new Synapse[] { s0, s1 })));
        activeSynapseSegments.put(dd2, new LinkedHashSet<>(Arrays.asList(new Synapse[] { s3, s4 })));
        activeSynapseSegments.put(dd3, new LinkedHashSet<>(Arrays.asList(new Synapse[] { s5 })));
        
        ComputeCycle cycle = new ComputeCycle();
        tm.computePredictiveCells(cla, cycle, activeSynapseSegments);
        
        assertTrue(cycle.activeSegments().contains(dd) && cycle.activeSegments().size() == 1);
        assertTrue(cycle.predictiveCells().contains(cla.getCell(0)) && cycle.predictiveCells().size() == 1);
    }
    
    @SuppressWarnings("unused")
    @Test
    public void testComputeActiveSynapses() {
        
        int segmentCounter = 0;
        int synapseCounter = 0;
        
        DistalDendrite dd = cla.getCell(0).createSegment(cla, segmentCounter++);
        Synapse s0 = dd.createSynapse(cla, cla.getCell(23), 0.6, synapseCounter++);
        Synapse s1 = dd.createSynapse(cla, cla.getCell(37), 0.4, synapseCounter++);
        Synapse s2 = dd.createSynapse(cla, cla.getCell(477), 0.9, synapseCounter++);
        
        DistalDendrite dd1 = cla.getCell(1).createSegment(cla, segmentCounter++);
        Synapse s3 = dd1.createSynapse(cla, cla.getCell(733), 0.7, synapseCounter++);
        
        DistalDendrite dd2 = cla.getCell(8).createSegment(cla, segmentCounter++);
        Synapse s4 = dd2.createSynapse(cla, cla.getCell(486), 0.9, synapseCounter++);
        
        Set<Cell> activeCells = new LinkedHashSet<>(
            Arrays.asList(
                new Cell[] {
                	cla.getCell(23), cla.getCell(37), cla.getCell(733), cla.getCell(4973) 
                } 
            )
        );
        
        Map<DistalDendrite, Set<Synapse>> activeSegmentSynapses = tm.computeActiveSynapses(cla, activeCells);
        
        Set<Synapse> syns = activeSegmentSynapses.get(dd);
        assertEquals(2, syns.size());
        assertTrue(syns.contains(s0));
        assertTrue(syns.contains(s1));
        
        syns = activeSegmentSynapses.get(dd1);
        assertEquals(1, syns.size());
        assertTrue(syns.contains(s3));
    }
    
    @SuppressWarnings("unused")
    @Test
    public void testGetBestMatchingCell() {
        cla.setConnectedPermanence(0.50);
        cla.setMinThreshold(1);
        cla.setSeed(42);
        tm.clear();
       
        int segmentCounter = 0;
        int synapseCounter = 0;
        
        DistalDendrite dd = cla.getCell(0).createSegment(cla, segmentCounter++);
        Synapse s0 = dd.createSynapse(cla, cla.getCell(23), 0.6, synapseCounter++);
        Synapse s1 = dd.createSynapse(cla, cla.getCell(37), 0.4, synapseCounter++);
        Synapse s2 = dd.createSynapse(cla, cla.getCell(477), 0.9, synapseCounter++);
        
        DistalDendrite dd1 = cla.getCell(0).createSegment(cla, segmentCounter++);
        Synapse s3 = dd1.createSynapse(cla, cla.getCell(49), 0.9, synapseCounter++);
        Synapse s4 = dd1.createSynapse(cla, cla.getCell(3), 0.8, synapseCounter++);
        
        DistalDendrite dd2 = cla.getCell(1).createSegment(cla, segmentCounter++);
        Synapse s5 = dd2.createSynapse(cla, cla.getCell(733), 0.7, synapseCounter++);
        
        DistalDendrite dd3 = cla.getCell(1).createSegment(cla, segmentCounter++);
        Synapse s6 = dd3.createSynapse(cla, cla.getCell(486), 0.9, synapseCounter++);
        
        Map<DistalDendrite, Set<Synapse>> activeSegments = new LinkedHashMap<>();
        activeSegments.put(dd, new LinkedHashSet<>(Arrays.asList(new Synapse[] { s0, s1 })));
        activeSegments.put(dd1, new LinkedHashSet<>(Arrays.asList(new Synapse[] { s3 })));
        activeSegments.put(dd2, new LinkedHashSet<>(Arrays.asList(new Synapse[] { s5 })));
        
        Object[] result = tm.getBestMatchingCell(cla, cla.getColumn(0), activeSegments);
        assertEquals(dd, result[0]);
        assertEquals(0, ((Cell)result[1]).getIndex());
        
        result = tm.getBestMatchingCell(cla, cla.getColumn(3), activeSegments);
        assertNull(result[0]);
        assertEquals(107, ((Cell)result[1]).getIndex());
        
        result = tm.getBestMatchingCell(cla, cla.getColumn(999), activeSegments);
        assertNull(result[0]);
        assertEquals(31993, ((Cell)result[1]).getIndex());
        
    }
    
    @SuppressWarnings("unused")
    @Test
    public void testGetBestMatchingCellFewestSegments() {
        cla.setColumnDimensions(new int[] { 2 });
        cla.setCellsPerColumn(2);
        cla.setConnectedPermanence(0.50);
        cla.setMinThreshold(1);
        cla.setSeed(42);
        cla.clear();
        tm.clear();
        
        DistalDendrite dd = cla.getCell(0).createSegment(cla, 0);
        Synapse s0 = dd.createSynapse(cla, cla.getCell(3), 0.3, 0);
        
        Map<DistalDendrite, Set<Synapse>> activeSegments = new LinkedHashMap<>();
        
        //Never pick cell 0, always pick cell 1
        for(int i = 0;i < 100;i++) {
            Object[] result = tm.getBestMatchingCell(cla, cla.getColumn(0), activeSegments);
            assertEquals(1, ((Cell)result[1]).getIndex());
        }
    }
    
    @SuppressWarnings("unused")
    @Test
    public void testGetBestMatchingSegment() {
        cla.setConnectedPermanence(0.50);
        cla.setMinThreshold(1);
        tm.clear();
        
        int segmentCounter = 0;
        int synapseCounter = 0;
        
        DistalDendrite dd = cla.getCell(0).createSegment(cla, segmentCounter++);
        Synapse s0 = dd.createSynapse(cla, cla.getCell(23), 0.6, synapseCounter++);
        Synapse s1 = dd.createSynapse(cla, cla.getCell(37), 0.4, synapseCounter++);
        Synapse s2 = dd.createSynapse(cla, cla.getCell(477), 0.9, synapseCounter++);
        
        DistalDendrite dd1 = cla.getCell(0).createSegment(cla, segmentCounter++);
        Synapse s3 = dd1.createSynapse(cla, cla.getCell(49), 0.9, synapseCounter++);
        Synapse s4 = dd1.createSynapse(cla, cla.getCell(3), 0.8, synapseCounter++);
        
        DistalDendrite dd2 = cla.getCell(1).createSegment(cla, segmentCounter++);
        Synapse s5 = dd2.createSynapse(cla, cla.getCell(733), 0.7, synapseCounter++);
        
        DistalDendrite dd3 = cla.getCell(8).createSegment(cla, segmentCounter++);
        Synapse s6 = dd3.createSynapse(cla, cla.getCell(486), 0.9, synapseCounter++);
        
        Map<DistalDendrite, Set<Synapse>> activeSegments = new LinkedHashMap<>();
        activeSegments.put(dd, new LinkedHashSet<>(Arrays.asList(new Synapse[] { s0, s1 })));
        activeSegments.put(dd1, new LinkedHashSet<>(Arrays.asList(new Synapse[] { s3 })));
        activeSegments.put(dd2, new LinkedHashSet<>(Arrays.asList(new Synapse[] { s5 })));
        
        DistalDendrite result = tm.getBestMatchingSegment(cla, cla.getCell(0), activeSegments);
        List<Synapse> resultSynapses = new ArrayList<>(result.getConnectedActiveSynapses(activeSegments, 0));
        assertEquals(dd, result);
        assertEquals(s0, resultSynapses.get(0));
        assertEquals(s1, resultSynapses.get(1));
        
        result = tm.getBestMatchingSegment(cla, cla.getCell(1), activeSegments);
        resultSynapses = new ArrayList<>(result.getConnectedActiveSynapses(activeSegments, 0));
        assertEquals(dd2, result);
        assertEquals(s5, resultSynapses.get(0));
        
        result = tm.getBestMatchingSegment(cla, cla.getCell(8), activeSegments);
        assertEquals(null, result);
        
        result = tm.getBestMatchingSegment(cla, cla.getCell(100), activeSegments);
        assertEquals(null, result);
        
    }
    
    @SuppressWarnings("unused")
    @Test
    public void testGetLeastUsedCell() {
        cla.setColumnDimensions(new int[] { 2 });
        cla.setCellsPerColumn(2);
        cla.setSeed(42);
        
        tm.clear();
        
        DistalDendrite dd = cla.getCell(0).createSegment(cla, 0);
        Synapse s0 = dd.createSynapse(cla, cla.getCell(3), 0.3, 0);
        
        Column column0 = cla.getColumn(0);
        Random random = cla.getRandom();
        for(int i = 0;i < 100;i++) {
            Cell leastUsed = column0.getLeastUsedCell(cla, cla.getRandom());
            assertEquals(1, leastUsed.getIndex());
        }
    }
    
    @SuppressWarnings("unused")
    @Test
    public void testComputeActiveSynapsesNoActivity() {
        
        int segmentCounter = 0;
        int synapseCounter = 0;
        
        DistalDendrite dd = cla.getCell(0).createSegment(cla, segmentCounter++);
        Synapse s0 = dd.createSynapse(cla, cla.getCell(23), 0.6, synapseCounter++);
        Synapse s1 = dd.createSynapse(cla, cla.getCell(37), 0.4, synapseCounter++);
        Synapse s2 = dd.createSynapse(cla, cla.getCell(477), 0.9, synapseCounter++);
        
        DistalDendrite dd1 = cla.getCell(1).createSegment(cla, segmentCounter++);
        Synapse s3 = dd1.createSynapse(cla, cla.getCell(733), 0.7, synapseCounter++);
        
        DistalDendrite dd2 = cla.getCell(8).createSegment(cla, segmentCounter++);
        Synapse s4 = dd2.createSynapse(cla, cla.getCell(486), 0.9, synapseCounter++);
        
        Map<DistalDendrite, Set<Synapse>> result = tm.computeActiveSynapses(cla, new LinkedHashSet<Cell>());
        assertTrue(result.isEmpty());
    }
    
    @SuppressWarnings("unused")
    @Test
    public void testGetConnectedActiveSynapsesForSegment() {
        
        int segmentCounter = 0;
        int synapseCounter = 0;
        
        DistalDendrite dd = cla.getCell(0).createSegment(cla, segmentCounter++);
        Synapse s0 = dd.createSynapse(cla, cla.getCell(23), 0.6, synapseCounter++);
        Synapse s1 = dd.createSynapse(cla, cla.getCell(37), 0.4, synapseCounter++);
        Synapse s2 = dd.createSynapse(cla, cla.getCell(477), 0.9, synapseCounter++);
        
        DistalDendrite dd1 = cla.getCell(1).createSegment(cla, segmentCounter++);
        Synapse s3 = dd1.createSynapse(cla, cla.getCell(733), 0.7, synapseCounter++);
        
        DistalDendrite dd2 = cla.getCell(8).createSegment(cla, segmentCounter++);
        Synapse s4 = dd2.createSynapse(cla, cla.getCell(486), 0.9, synapseCounter++);
        
        Map<DistalDendrite, Set<Synapse>> activeSegments = new LinkedHashMap<>();
        activeSegments.put(dd, new LinkedHashSet<>(Arrays.asList(new Synapse[] { s0, s1 })));
        activeSegments.put(dd1, new LinkedHashSet<>(Arrays.asList(new Synapse[] { s3 })));
        
        List<Synapse> connectActive = new ArrayList<>(dd.getConnectedActiveSynapses(activeSegments, 0.5));
        assertEquals(1, connectActive.size());
        assertEquals(s0, connectActive.get(0));
        
        connectActive = new ArrayList<>(dd1.getConnectedActiveSynapses(activeSegments, 0.5));
        assertEquals(1, connectActive.size());
        assertEquals(s3, connectActive.get(0));
    }
    
    @Test
    public void testAdaptSegment() {
        
        int segmentCounter = 0;
        int synapseCounter = 0;
        
        DistalDendrite dd = cla.getCell(0).createSegment(cla, segmentCounter);
        Synapse s0 = dd.createSynapse(cla, cla.getCell(23), 0.6, synapseCounter++);
        Synapse s1 = dd.createSynapse(cla, cla.getCell(37), 0.4, synapseCounter++);
        Synapse s2 = dd.createSynapse(cla, cla.getCell(477), 0.9, synapseCounter++);
        
        Set<Synapse> activeSynapses = new LinkedHashSet<>();
        activeSynapses.add(s0);
        activeSynapses.add(s1);
        dd.adaptSegment(cla, activeSynapses, cla.getPermanenceIncrement(), cla.getPermanenceDecrement());
        
        assertEquals(0.7, s0.getPermanence(), 0.01);
        assertEquals(0.5, s1.getPermanence(), 0.01);
        assertEquals(0.8, s2.getPermanence(), 0.01);
    }
    
    @Test
    public void testAdaptSegmentToMax() {
        
        DistalDendrite dd = cla.getCell(0).createSegment(cla, 0);
        Synapse s0 = dd.createSynapse(cla, cla.getCell(23), 0.9, 0);
        
        Set<Synapse> activeSynapses = new LinkedHashSet<>();
        activeSynapses.add(s0);
        
        dd.adaptSegment(cla, activeSynapses, cla.getPermanenceIncrement(), cla.getPermanenceDecrement());
        assertEquals(1.0, s0.getPermanence(), 0.01);
        
        dd.adaptSegment(cla, activeSynapses, cla.getPermanenceIncrement(), cla.getPermanenceDecrement());
        assertEquals(1.0, s0.getPermanence(), 0.01);
    }

    @Test
    public void testAdaptSegmentToMin() {
        
        DistalDendrite dd = cla.getCell(0).createSegment(cla, 0);
        Synapse s0 = dd.createSynapse(cla, cla.getCell(23), 0.1, 0);
        
        Set<Synapse> activeSynapses = new LinkedHashSet<>();
        
        dd.adaptSegment(cla, activeSynapses, cla.getPermanenceIncrement(), cla.getPermanenceDecrement());
        assertEquals(0.0, s0.getPermanence(), 0.01);
        
        dd.adaptSegment(cla, activeSynapses, cla.getPermanenceIncrement(), cla.getPermanenceDecrement());
        assertEquals(0.0, s0.getPermanence(), 0.01);
    }
    
    @Test
    public void testPickCellsToLearnOn() {
        
        DistalDendrite dd = cla.getCell(0).createSegment(cla, 0);
        
        Set<Cell> winnerCells = new LinkedHashSet<>();
        winnerCells.add(cla.getCell(4));
        winnerCells.add(cla.getCell(47));
        winnerCells.add(cla.getCell(58));
        winnerCells.add(cla.getCell(93));
        
        List<Cell> learnCells = new ArrayList<>(dd.pickCellsToLearnOn(cla, 2, winnerCells, cla.getRandom()));
        assertEquals(2, learnCells.size());
        assertTrue(learnCells.contains(cla.getCell(47)));
        assertTrue(learnCells.contains(cla.getCell(93)));
        
        learnCells = new ArrayList<>(dd.pickCellsToLearnOn(cla, 100, winnerCells, cla.getRandom()));
        assertEquals(4, learnCells.size());
        assertEquals(93, learnCells.get(0).getIndex());
        assertEquals(58, learnCells.get(1).getIndex());
        assertEquals(47, learnCells.get(2).getIndex());
        assertEquals(4, learnCells.get(3).getIndex());
        
        learnCells = new ArrayList<>(dd.pickCellsToLearnOn(cla, 0, winnerCells, cla.getRandom()));
        assertEquals(0, learnCells.size());
    }
    
    @Test
    public void testPickCellsToLearnOnAvoidDuplicates() {

        DistalDendrite dd = cla.getCell(0).createSegment(cla, 0);
        dd.createSynapse(cla, cla.getCell(23), 0.6, 0);
        
        Set<Cell> winnerCells = new LinkedHashSet<>();
        winnerCells.add(cla.getCell(23));
        
        List<Cell> learnCells = new ArrayList<>(dd.pickCellsToLearnOn(cla, 2, winnerCells, cla.getRandom()));
        assertTrue(learnCells.isEmpty());
    }
}
