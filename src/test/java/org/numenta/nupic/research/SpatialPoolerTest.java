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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.hash.TIntHashSet;

import java.util.Arrays;

import org.junit.Test;
import org.numenta.nupic.CLA;
import org.numenta.nupic.Build;
import org.numenta.nupic.KEY;
import org.numenta.nupic.model.Pool;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.Condition;
import org.numenta.nupic.util.IndexedMatrix;
import org.numenta.nupic.util.MersenneTwister;
import org.numenta.nupic.util.SparseBinaryMatrix;
import org.numenta.nupic.util.SparseBinaryMatrixTrueCount;
import org.numenta.nupic.util.SparseObjectMatrix;

public class SpatialPoolerTest {
    private Build param;
    private SpatialPooler sp;
    private CLA cla;
    
    public void setupParameters() {
        param = CLA.Default();
        param.set(KEY.INPUT_DIMENSIONS, new int[] { 5 });//5
        param.set(KEY.COLUMN_DIMENSIONS, new int[] { 5 });//5
        
        param.add(SpatialPooler.Default());
        param.set(KEY.POTENTIAL_RADIUS, 3);//3
        param.set(KEY.POTENTIAL_PCT, 0.5);//0.5
        param.set(KEY.GLOBAL_INHIBITIONS, false);
        param.set(KEY.LOCAL_AREA_DENSITY, -1.0);
        param.set(KEY.NUM_ACTIVE_COLUMNS_PER_INH_AREA, 3.0);
        param.set(KEY.STIMULUS_THRESHOLD, 1.0);
        param.set(KEY.SYN_PERM_INACTIVE_DEC, 0.01);
        param.set(KEY.SYN_PERM_ACTIVE_INC, 0.1);
        param.set(KEY.SYN_PERM_TRIM_THRESHOLD, 0.05);
        param.set(KEY.SYN_PERM_CONNECTED, 0.1);
        param.set(KEY.MIN_PCT_OVERLAP_DUTY_CYCLE, 0.1);
        param.set(KEY.MIN_PCT_ACTIVE_DUTY_CYCLE, 0.1);
        param.set(KEY.DUTY_CYCLE_PERIOD, 10);
        param.set(KEY.MAX_BOOST, 10.0);
        param.set(KEY.SEED, 42);
        param.set(KEY.SP_VERBOSITY, 0);
    }
    
    private void initSP() {        
        cla = new CLA(param);        
        sp = new SpatialPooler(cla, param);
    }
    
    @Test
    public void confirmSPConstruction() {
        setupParameters();
        
        initSP();
        
        assertEquals(5, cla.getInputDimensions()[0]);
        assertEquals(5, cla.getColumnDimensions()[0]);
        assertEquals(3, cla.getPotentialRadius());
        assertEquals(0.5, cla.getPotentialPct(), 0);
        assertEquals(false, cla.getGlobalInhibition());
        assertEquals(-1.0, cla.getLocalAreaDensity(), 0);
        assertEquals(3, cla.getNumActiveColumnsPerInhArea(), 0);
        assertEquals(1, cla.getStimulusThreshold(), 1);
        assertEquals(0.01, cla.getSynPermInactiveDec(), 0);
        assertEquals(0.1, cla.getSynPermActiveInc(), 0);
        assertEquals(0.1, cla.getSynPermConnected(), 0);
        assertEquals(0.1, cla.getMinPctOverlapDutyCycles(), 0);
        assertEquals(0.1, cla.getMinPctActiveDutyCycles(), 0);
        assertEquals(10, cla.getDutyCyclePeriod(), 0);
        assertEquals(10.0, cla.getMaxBoost(), 0);
        assertEquals(42, cla.getSeed());
        assertEquals(0, cla.getSpVerbosity());
        
        assertEquals(5, cla.getNumInputs());
        assertEquals(5, cla.getNumColumns());
    }
    
    /**
     * Checks that feeding in the same input vector leads to polarized
     * permanence values: either zeros or ones, but no fractions
     */
    @Test
    public void testCompute1() {
        setupParameters();
        param.setInputDimensions(new int[] { 9 });
        param.setColumnDimensions(new int[] { 5 });
        param.setPotentialRadius(5);
        
        //This is 0.3 in Python version due to use of dense 
        // permanence instead of sparse (as it should be)
        param.setPotentialPct(0.5); 
        
        param.setGlobalInhibition(false);
        param.setLocalAreaDensity(-1.0);
        param.setNumActiveColumnsPerInhArea(3);
        param.setStimulusThreshold(1);
        param.setSynPermInactiveDec(0.01);
        param.setSynPermActiveInc(0.1);
        param.setMinPctOverlapDutyCycle(0.1);
        param.setMinPctActiveDutyCycle(0.1);
        param.setDutyCyclePeriod(10);
        param.setMaxBoost(10);
        param.setSynPermTrimThreshold(0);
        
        //This is 0.5 in Python version due to use of dense 
        // permanence instead of sparse (as it should be)
        param.setPotentialPct(1);
        
        param.setSynPermConnected(0.1);
        
    	initSP();
    	
    	SpatialPooler mock = new SpatialPooler() {
    		public int[] inhibitColumns(CLA c, double[] overlaps) {
    			return new int[] { 0, 1, 2, 3, 4 };
    		}
    	};
    	
    	int[] inputVector = new int[] { 1, 0, 1, 0, 1, 0, 0, 1, 1 };
    	int[] activeArray = new int[] { 0, 0, 0, 0, 0 };
    	for(int i = 0;i < 20;i++) {
    		mock.compute(cla, inputVector, activeArray, true, true);
    	}
    	
    	for(int i = 0;i < cla.getNumColumns();i++) {
    		System.out.println(Arrays.toString((int[])cla.getConnectedCounts().getSlice(i)));
    		System.out.println(Arrays.toString(cla.getPotentialPools().getIndex(i).getPermanencesDense(cla)));
    		assertTrue(Arrays.equals(inputVector, ((int[])cla.getConnectedCounts().getSlice(i))));
    	}
    }
    
    /**
     * Checks that columns only change the permanence values for 
     * inputs that are within their potential pool
     */
    @Test
    public void testCompute2() {
    	setupParameters();
        param.setInputDimensions(new int[] { 10 });
        param.setColumnDimensions(new int[] { 5 });
        param.setPotentialRadius(3);
        param.setPotentialPct(0.3); 
        param.setGlobalInhibition(false);
        param.setLocalAreaDensity(-1.0);
        param.setNumActiveColumnsPerInhArea(3);
        param.setStimulusThreshold(1);
        param.setSynPermInactiveDec(0.01);
        param.setSynPermActiveInc(0.1);
        param.setMinPctOverlapDutyCycle(0.1);
        param.setMinPctActiveDutyCycle(0.1);
        param.setDutyCyclePeriod(10);
        param.setMaxBoost(10);
        param.setSynPermConnected(0.1);
         
     	initSP();
     	
     	SpatialPooler mock = new SpatialPooler() {
     		public int[] inhibitColumns(CLA c, double[] overlaps) {
     			return new int[] { 0, 1, 2, 3, 4 };
     		}
     	};
     	
     	int[] inputVector = new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 };
     	int[] activeArray = new int[] { 0, 0, 0, 0, 0 };
     	for(int i = 0;i < 20;i++) {
     		mock.compute(cla, inputVector, activeArray, true, true);
     	}
     	
     	for(int i = 0;i < cla.getNumColumns();i++) {
//     		System.out.println(Arrays.toString((int[])mem.getConnectedCounts().getSlice(i)));
//     		System.out.println(Arrays.toString(mem.getPotentialPools().getObject(i).getDensePermanences(mem)));
     		int[] permanences = ArrayUtils.toIntArray(cla.getPotentialPools().getIndex(i).getPermanencesDense(cla));
     		int[] potential = (int[])cla.getConnectedCounts().getSlice(i);
     		assertTrue(Arrays.equals(permanences, potential));
     	}
    }
    
    /**
     * Given a specific input and initialization params the SP should return this
     * exact output.
     *
     * Previously output varied between platforms (OSX/Linux etc) == (in Python)
     */
    @Test
    public void testExactOutput() {
    	setupParameters();
        param.setInputDimensions(new int[] { 1, 188});
        param.setColumnDimensions(new int[] { 2048, 1 });
        param.setPotentialRadius(94);
        param.setPotentialPct(0.5); 
        param.setGlobalInhibition(true);
        param.setLocalAreaDensity(-1.0);
        param.setNumActiveColumnsPerInhArea(40);
        param.setStimulusThreshold(1);
        param.setSynPermInactiveDec(0.01);
        param.setSynPermActiveInc(0.1);
        param.setMinPctOverlapDutyCycle(0.1);
        param.setMinPctActiveDutyCycle(0.1);
        param.setDutyCyclePeriod(1000);
        param.setMaxBoost(10);
        param.setSynPermConnected(0.1);
        param.setSynPermTrimThreshold(0);
        param.setRandom(new MersenneTwister(42));
        initSP();
        
        int[] inputVector = {
        		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0	
        };
        
       
        int[] activeArray = new int[2048];
        
        sp.compute(cla, inputVector, activeArray, true, false);
        
        int[] real = ArrayUtils.where(activeArray, new Condition.Adapter<Object>() {
        	public boolean eval(int n) {
        		return n > 0;
        	}
        });
        
        int[] expected = new int[] {
        		46, 61, 86, 216, 314, 543, 554, 587, 630, 675, 736, 
        		745, 834, 931, 990, 1131, 1285, 1305, 1307, 1326, 1411, 1414, 
        		1431, 1471, 1547, 1579, 1603, 1687, 1698, 1730, 1847, 
        		1859, 1885, 1893, 1895, 1907, 1934, 1978, 1984, 1990 };
        
        assertTrue(Arrays.equals(expected, real));
    }
    
    @Test
    public void testMapColumn() {
    	// Test 1D
    	setupParameters();
    	param.setColumnDimensions(new int[] { 4 });
    	param.setInputDimensions(new int[] { 12 });
    	initSP();
    	
    	assertEquals(1, sp.mapColumn(cla, 0));
    	assertEquals(4, sp.mapColumn(cla, 1));
    	assertEquals(7, sp.mapColumn(cla, 2));
    	assertEquals(10, sp.mapColumn(cla, 3));
    	
    	// Test 1D with same dimension of columns and inputs
    	setupParameters();
    	param.setColumnDimensions(new int[] { 4 });
    	param.setInputDimensions(new int[] { 4 });
    	initSP();
    	
    	assertEquals(0, sp.mapColumn(cla, 0));
    	assertEquals(1, sp.mapColumn(cla, 1));
    	assertEquals(2, sp.mapColumn(cla, 2));
    	assertEquals(3, sp.mapColumn(cla, 3));
    	
    	// Test 1D with same dimensions of length 1
    	setupParameters();
    	param.setColumnDimensions(new int[] { 1 });
    	param.setInputDimensions(new int[] { 1 });
    	initSP();
    	
    	assertEquals(0, sp.mapColumn(cla, 0));
    	
    	// Test 2D
    	setupParameters();
    	param.setColumnDimensions(new int[] { 12, 4 });
    	param.setInputDimensions(new int[] { 36, 12 });
    	initSP();
    	
    	assertEquals(13, sp.mapColumn(cla, 0));
    	assertEquals(49, sp.mapColumn(cla, 4));
    	assertEquals(52, sp.mapColumn(cla, 5));
    	assertEquals(58, sp.mapColumn(cla, 7));
    	assertEquals(418, sp.mapColumn(cla, 47));
    }
    
    @Test
    public void testStripNeverLearned() {
    	setupParameters();
    	param.setColumnDimensions(new int[] { 6 });
    	param.setInputDimensions(new int[] { 9 });
    	initSP();
    	
    	cla.updateActiveDutyCycles(new double[] { 0.5, 0.1, 0, 0.2, 0.4, 0 });
    	int[] activeColumns = new int[] { 0, 1, 2, 4 };
    	TIntArrayList stripped = sp.stripUnlearnedColumns(cla, activeColumns);
    	TIntArrayList trueStripped = new TIntArrayList(new int[] { 0, 1, 4 });
    	assertEquals(trueStripped, stripped);
    	
    	cla.updateActiveDutyCycles(new double[] { 0.9, 0, 0, 0, 0.4, 0.3 });
    	activeColumns = ArrayUtils.range(0,  6);
    	stripped = sp.stripUnlearnedColumns(cla, activeColumns);
    	trueStripped = new TIntArrayList(new int[] { 0, 4, 5 });
    	assertEquals(trueStripped, stripped);
    	
    	cla.updateActiveDutyCycles(new double[] { 0, 0, 0, 0, 0, 0 });
    	activeColumns = ArrayUtils.range(0,  6);
    	stripped = sp.stripUnlearnedColumns(cla, activeColumns);
    	trueStripped = new TIntArrayList();
    	assertEquals(trueStripped, stripped);
    	
    	cla.updateActiveDutyCycles(new double[] { 1, 1, 1, 1, 1, 1 });
    	activeColumns = ArrayUtils.range(0,  6);
    	stripped = sp.stripUnlearnedColumns(cla, activeColumns);
    	trueStripped = new TIntArrayList(ArrayUtils.range(0,  6));
    	assertEquals(trueStripped, stripped);
    	
    	
    }
    
    @Test
    public void testMapPotential1D() {
    	setupParameters();
        param.setInputDimensions(new int[] { 12 });
        param.setColumnDimensions(new int[] { 4 });
        param.setPotentialRadius(2);
        param.setPotentialPct(1);
        initSP();
        
        assertEquals(12, cla.getInputDimensions()[0]);
        assertEquals(4, cla.getColumnDimensions()[0]);
        assertEquals(2, cla.getPotentialRadius());
        
        // Test without wrapAround and potentialPct = 1
        int[] expected = new int[] { 0, 1, 2, 3 };
        int[] mask = sp.mapPotential(cla, 0, false);
        assertTrue(Arrays.equals(expected, mask));
        
        expected = new int[] { 5, 6, 7, 8, 9 };
        mask = sp.mapPotential(cla, 2, false);
        assertTrue(Arrays.equals(expected, mask));
        
        // Test with wrapAround and potentialPct = 1        
        expected = new int[] { 0, 1, 2, 3, 11 };
        mask = sp.mapPotential(cla, 0, true);
        assertTrue(Arrays.equals(expected, mask));
        
        expected = new int[] { 0, 8, 9, 10, 11 };
        mask = sp.mapPotential(cla, 3, true);
        assertTrue(Arrays.equals(expected, mask));
        
        // Test with wrapAround and potentialPct < 1
        param.setPotentialPct(0.5);
        initSP();
        
        int[] supersetMask = new int[] { 0, 1, 2, 3, 11 }; 
        mask = sp.mapPotential(cla, 0, true);
        assertEquals(mask.length, 3);
        TIntArrayList unionList = new TIntArrayList(supersetMask);
        unionList.addAll(mask);
        int[] unionMask = ArrayUtils.unique(unionList.toArray());
        assertTrue(Arrays.equals(unionMask, supersetMask));
    }
    
    @Test
    public void testMapPotential2D() {
    	setupParameters();
        param.setInputDimensions(new int[] { 6, 12 });
        param.setColumnDimensions(new int[] { 2, 4 });
        param.setPotentialRadius(1);
        param.setPotentialPct(1);
        initSP();
        
        //Test without wrapAround
        int[] mask = sp.mapPotential(cla, 0, false);
        TIntHashSet trueIndices = new TIntHashSet(new int[] { 0, 1, 2, 12, 13, 14, 24, 25, 26 });
        TIntHashSet maskSet = new TIntHashSet(mask);
        assertTrue(trueIndices.equals(maskSet));
        
        trueIndices.clear();
        maskSet.clear();
        trueIndices.addAll(new int[] { 6, 7, 8, 18, 19, 20, 30, 31, 32 });
        mask = sp.mapPotential(cla, 2, false);
        maskSet.addAll(mask);
        assertTrue(trueIndices.equals(maskSet));
        
        //Test with wrapAround
        trueIndices.clear();
        maskSet.clear();
        param.setPotentialRadius(2);
        initSP();
        trueIndices.addAll(
        	new int[] { 0, 1, 2, 3, 11, 
        				12, 13, 14, 15, 23,
        				24, 25, 26, 27, 35, 
        				36, 37, 38, 39, 47, 
        				60, 61, 62, 63, 71 });
        mask = sp.mapPotential(cla, 0, true);
        maskSet.addAll(mask);
        assertTrue(trueIndices.equals(maskSet));
        
        trueIndices.clear();
        maskSet.clear();
        trueIndices.addAll(
        	new int[] { 0, 8, 9, 10, 11, 
        				12, 20, 21, 22, 23, 
        				24, 32, 33, 34, 35, 
        				36, 44, 45, 46, 47, 
        				60, 68, 69, 70, 71 });
        mask = sp.mapPotential(cla, 3, true);
        maskSet.addAll(mask);
        assertTrue(trueIndices.equals(maskSet));
    }
    
    @Test
    public void testMapPotential1Column1Input() {
    	setupParameters();
        param.setInputDimensions(new int[] { 1 });
        param.setColumnDimensions(new int[] { 1 });
        param.setPotentialRadius(2);
        param.setPotentialPct(1);
        initSP();
        
        //Test without wrapAround and potentialPct = 1
        int[] expectedMask = new int[] { 0 }; 
        int[] mask = sp.mapPotential(cla, 0, false);
        TIntHashSet trueIndices = new TIntHashSet(expectedMask);
        TIntHashSet maskSet = new TIntHashSet(mask);
        // The *position* of the one "on" bit expected. 
        // Python version returns [1] which is the on bit in the zero'th position
        assertTrue(trueIndices.equals(maskSet));
    }
    
    //////////////////////////////////////////////////////////////
    /**
     * Local test apparatus for {@link #testInhibitColumns()}
     */
    boolean globalCalled = false;
    boolean localCalled = false;
    double _density = 0;
	public void reset() {
		this.globalCalled = false;
		this.localCalled = false;
		this._density = 0;
	}
	public void setGlobalCalled(boolean b) {
		this.globalCalled = b;
	}
	public void setLocalCalled(boolean b) {
		this.localCalled = b;
	}
	//////////////////////////////////////////////////////////////
	
    @Test
    public void testInhibitColumns() {
    	setupParameters();
    	param.setColumnDimensions(new int[] { 5 });
    	param.setInhibitionRadius(10);
    	initSP();
    	
    	//Mocks to test which method gets called
    	SpatialPooler inhibitColumnsGlobal = new SpatialPooler() {
    		@Override public int[] inhibitColumnsGlobal(CLA c, double[] overlap, double density) {
    			setGlobalCalled(true);
    			_density = density;
    			return new int[] { 1 };
    		}
    	};
    	SpatialPooler inhibitColumnsLocal = new SpatialPooler() {
    		@Override public int[] inhibitColumnsLocal(CLA c, double[] overlap, double density) {
    			setLocalCalled(true);
    			_density = density;
    			return new int[] { 2 };
    		}
    	};
    	
    	double[] overlaps = ArrayUtils.sample(cla.getNumColumns(), cla.getRandom());
    	cla.setNumActiveColumnsPerInhArea(5);
    	cla.setLocalAreaDensity(0.1);
    	cla.setGlobalInhibition(true);
    	cla.setInhibitionRadius(5);
    	double trueDensity = cla.getLocalAreaDensity();
    	inhibitColumnsGlobal.inhibitColumns(cla, overlaps);
    	assertTrue(globalCalled);
    	assertTrue(!localCalled);
    	assertEquals(trueDensity, _density, .01d);
    	
    	//////
    	reset();
    	param.setInputDimensions(new int[] { 50, 10 });
    	param.setColumnDimensions(new int[] { 50, 10 });
    	param.setGlobalInhibition(false);
    	param.setLocalAreaDensity(0.1);
    	initSP();
    	//Internally calculated during init, to overwrite we put after init
    	param.setInhibitionRadius(7);
    	
    	double[] tieBreaker = new double[500];
    	Arrays.fill(tieBreaker, 0);
    	cla.setTieBreaker(tieBreaker);
    	overlaps = ArrayUtils.sample(cla.getNumColumns(), cla.getRandom());
    	inhibitColumnsLocal.inhibitColumns(cla, overlaps);
    	trueDensity = cla.getLocalAreaDensity();
    	assertTrue(!globalCalled);
    	assertTrue(localCalled);
    	assertEquals(trueDensity, _density, .01d);
    	
    	//////
    	reset();
    	param.setInputDimensions(new int[] { 100, 10 });
    	param.setColumnDimensions(new int[] { 100, 10 });
    	param.setGlobalInhibition(false);
    	param.setLocalAreaDensity(-1);
    	param.setNumActiveColumnsPerInhArea(3);
    	initSP();
    	
    	//Internally calculated during init, to overwrite we put after init
    	cla.setInhibitionRadius(4);
    	tieBreaker = new double[1000];
    	Arrays.fill(tieBreaker, 0);
    	cla.setTieBreaker(tieBreaker);
    	overlaps = ArrayUtils.sample(cla.getNumColumns(), cla.getRandom());
    	inhibitColumnsLocal.inhibitColumns(cla, overlaps);
    	trueDensity = 3.0 / 81.0;
    	assertTrue(!globalCalled);
    	assertTrue(localCalled);
    	assertEquals(trueDensity, _density, .01d);
    	
    	//////
    	reset();
    	param.setInputDimensions(new int[] { 100, 10 });
    	param.setColumnDimensions(new int[] { 100, 10 });
    	param.setGlobalInhibition(false);
    	param.setLocalAreaDensity(-1);
    	param.setNumActiveColumnsPerInhArea(7);
    	initSP();
    	
    	//Internally calculated during init, to overwrite we put after init
    	cla.setInhibitionRadius(1);
    	tieBreaker = new double[1000];
    	Arrays.fill(tieBreaker, 0);
    	cla.setTieBreaker(tieBreaker);
    	overlaps = ArrayUtils.sample(cla.getNumColumns(), cla.getRandom());
    	inhibitColumnsLocal.inhibitColumns(cla, overlaps);
    	trueDensity = 0.5;
    	assertTrue(!globalCalled);
    	assertTrue(localCalled);
    	assertEquals(trueDensity, _density, .01d);
    	
    }
    
    @Test
    public void testInhibitColumnsGlobal() {
    	setupParameters();
    	param.setColumnDimensions(new int[] { 10 });
    	initSP();
    	//Internally calculated during init, to overwrite we put after init
    	param.setInhibitionRadius(2);
    	double density = 0.3;
    	double[] overlaps = new double[] { 1, 2, 1, 4, 8, 3, 12, 5, 4, 1 };
    	int[] active = sp.inhibitColumnsGlobal(cla, overlaps, density);
    	int[] trueActive = new int[] { 4, 6, 7 };
    	assertTrue(Arrays.equals(trueActive, active));
    }
    
    @Test
    public void testInhibitColumnsLocal() {
    	setupParameters();
    	param.setInputDimensions(new int[] { 10 });
    	param.setColumnDimensions(new int[] { 10 });
    	initSP();
    	
    	//Internally calculated during init, to overwrite we put after init
    	cla.setInhibitionRadius(2);
    	double density = 0.5;
    	double[] overlaps = new double[] { 1, 2, 7, 0, 3, 4, 16, 1, 1.5, 1.7 };
    	int[] trueActive = new int[] {1, 2, 5, 6, 9};
    	int[] active = sp.inhibitColumnsLocal(cla, overlaps, density);
    	assertTrue(Arrays.equals(trueActive, active));
    	
    	setupParameters();
    	param.setInputDimensions(new int[] { 10 });
    	param.setColumnDimensions(new int[] { 10 });
    	initSP();
    	//Internally calculated during init, to overwrite we put after init
    	cla.setInhibitionRadius(3);
    	overlaps = new double[] { 1, 2, 7, 0, 3, 4, 16, 1, 1.5, 1.7 };
    	trueActive = new int[] { 1, 2, 5, 6 };
    	active = sp.inhibitColumnsLocal(cla, overlaps, density);
    	//Commented out in Python version because it is wrong?
    	//assertTrue(Arrays.equals(trueActive, active));
    	
    	// Test add to winners
    	density = 0.3333;
    	setupParameters();
    	param.setInputDimensions(new int[] { 10 });
    	param.setColumnDimensions(new int[] { 10 });
    	initSP();
    	//Internally calculated during init, to overwrite we put after init
    	cla.setInhibitionRadius(3);
    	overlaps = new double[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 };
    	trueActive = new int[] { 0, 1, 4, 5, 8 };
    	active = sp.inhibitColumnsLocal(cla, overlaps, density);
    	assertTrue(Arrays.equals(trueActive, active));
    }
    
    @Test
    public void testUpdateBoostFactors() {
    	setupParameters();
    	param.setInputDimensions(new int[] { 6/*Don't care*/ });
    	param.setColumnDimensions(new int[] { 6 });
    	param.setMaxBoost(10.0);
    	initSP();
    	
    	double[] minActiveDutyCycles = new double[6];
    	Arrays.fill(minActiveDutyCycles, 0.000001D);
    	cla.setMinActiveDutyCycles(minActiveDutyCycles);
    	
    	double[] activeDutyCycles = new double[] { 0.1, 0.3, 0.02, 0.04, 0.7, 0.12 };
    	cla.setActiveDutyCycles(activeDutyCycles);
    	
    	double[] trueBoostFactors = new double[] { 1, 1, 1, 1, 1, 1 };
    	sp.updateBoostFactors(cla);
    	double[] boostFactors = cla.getBoostFactors();
    	for(int i = 0;i < boostFactors.length;i++) {
    		assertEquals(trueBoostFactors[i], boostFactors[i], 0.1D);
    	}
    	
    	////////////////
    	minActiveDutyCycles = new double[] { 0.1, 0.3, 0.02, 0.04, 0.7, 0.12 };
    	cla.setMinActiveDutyCycles(minActiveDutyCycles);
    	Arrays.fill(cla.getBoostFactors(), 0);
    	sp.updateBoostFactors(cla);
    	boostFactors = cla.getBoostFactors();
    	for(int i = 0;i < boostFactors.length;i++) {
    		assertEquals(trueBoostFactors[i], boostFactors[i], 0.1D);
    	}
    	
    	////////////////
    	minActiveDutyCycles = new double[] { 0.1, 0.2, 0.02, 0.03, 0.7, 0.12 };
    	cla.setMinActiveDutyCycles(minActiveDutyCycles);
    	activeDutyCycles = new double[] { 0.01, 0.02, 0.002, 0.003, 0.07, 0.012 };
    	cla.setActiveDutyCycles(activeDutyCycles);
    	trueBoostFactors = new double[] { 9.1, 9.1, 9.1, 9.1, 9.1, 9.1 };
    	sp.updateBoostFactors(cla);
    	boostFactors = cla.getBoostFactors();
    	for(int i = 0;i < boostFactors.length;i++) {
    		assertEquals(trueBoostFactors[i], boostFactors[i], 0.1D);
    	}
    	
    	////////////////
		minActiveDutyCycles = new double[] { 0.1, 0.2, 0.02, 0.03, 0.7, 0.12 };
		cla.setMinActiveDutyCycles(minActiveDutyCycles);
		Arrays.fill(activeDutyCycles, 0);
		cla.setActiveDutyCycles(activeDutyCycles);
		Arrays.fill(trueBoostFactors, 10.0);
		sp.updateBoostFactors(cla);
		boostFactors = cla.getBoostFactors();
		for(int i = 0;i < boostFactors.length;i++) {
			assertEquals(trueBoostFactors[i], boostFactors[i], 0.1D);
		}
    }
    
    @Test
    public void testAvgConnectedSpanForColumnND() {
    	sp = new SpatialPooler();
    	
    	int[] inputDimensions = new int[] { 4, 4, 2, 5 };
        cla.setInputDimensions(inputDimensions);
        cla.setColumnDimensions(5);
        sp.initMatrices(cla);
        
        TIntArrayList connected = new TIntArrayList();
        connected.add(cla.getInputMatrix().computeIndex(new int[] { 1, 0, 1, 0 }, false));
        connected.add(cla.getInputMatrix().computeIndex(new int[] { 1, 0, 1, 1 }, false));
        connected.add(cla.getInputMatrix().computeIndex(new int[] { 3, 2, 1, 0 }, false));
        connected.add(cla.getInputMatrix().computeIndex(new int[] { 3, 0, 1, 0 }, false));
        connected.add(cla.getInputMatrix().computeIndex(new int[] { 1, 0, 1, 3 }, false));
        connected.add(cla.getInputMatrix().computeIndex(new int[] { 2, 2, 1, 0 }, false));
        connected.sort(0, connected.size());
        //[ 45  46  48 105 125 145]
        //mem.getConnectedSynapses().set(0, connected.toArray());
        cla.getPotentialPools().set(new Pool(6), 0);
        cla.getColumn(0).setProximalConnectedSynapsesForTest(cla, connected.toArray());
        
        connected.clear();
        connected.add(cla.getInputMatrix().computeIndex(new int[] { 2, 0, 1, 0 }, false));
        connected.add(cla.getInputMatrix().computeIndex(new int[] { 2, 0, 0, 0 }, false));
        connected.add(cla.getInputMatrix().computeIndex(new int[] { 3, 0, 0, 0 }, false));
        connected.add(cla.getInputMatrix().computeIndex(new int[] { 3, 0, 1, 0 }, false));
        connected.sort(0, connected.size());
        //[ 80  85 120 125]
        //mem.getConnectedSynapses().set(1, connected.toArray());
        cla.getPotentialPools().set(new Pool(4), 1);
        cla.getColumn(1).setProximalConnectedSynapsesForTest(cla, connected.toArray());
        
        connected.clear();
        connected.add(cla.getInputMatrix().computeIndex(new int[] { 0, 0, 1, 4 }, false));
        connected.add(cla.getInputMatrix().computeIndex(new int[] { 0, 0, 0, 3 }, false));
        connected.add(cla.getInputMatrix().computeIndex(new int[] { 0, 0, 0, 1 }, false));
        connected.add(cla.getInputMatrix().computeIndex(new int[] { 1, 0, 0, 2 }, false));
        connected.add(cla.getInputMatrix().computeIndex(new int[] { 0, 0, 1, 1 }, false));
        connected.add(cla.getInputMatrix().computeIndex(new int[] { 3, 3, 1, 1 }, false));
        connected.sort(0, connected.size());
        //[  1   3   6   9  42 156]
        //mem.getConnectedSynapses().set(2, connected.toArray());
        cla.getPotentialPools().set(new Pool(4), 2);
        cla.getColumn(2).setProximalConnectedSynapsesForTest(cla, connected.toArray());
        
        connected.clear();
        connected.add(cla.getInputMatrix().computeIndex(new int[] { 3, 3, 1, 4 }, false));
        connected.add(cla.getInputMatrix().computeIndex(new int[] { 0, 0, 0, 0 }, false));
        connected.sort(0, connected.size());
        //[  0 159]
        //mem.getConnectedSynapses().set(3, connected.toArray());
        cla.getPotentialPools().set(new Pool(4), 3);
        cla.getColumn(3).setProximalConnectedSynapsesForTest(cla, connected.toArray());
        
        //[]
        connected.clear();
        cla.getPotentialPools().set(new Pool(4), 4);
        cla.getColumn(4).setProximalConnectedSynapsesForTest(cla, connected.toArray());
        
        double[] trueAvgConnectedSpan = new double[] { 11.0/4d, 6.0/4d, 14.0/4d, 15.0/4d, 0d };
        for(int i = 0;i < cla.getNumColumns();i++) {
        	double connectedSpan = sp.avgConnectedSpanForColumnND(cla, i);
        	assertEquals(trueAvgConnectedSpan[i], connectedSpan, 0);
        }
    }
    
    @Test
    public void testBumpUpWeakColumns() {
    	setupParameters();
    	param.setInputDimensions(new int[] { 8 });
    	param.setColumnDimensions(new int[] { 5 });
        initSP();
    	
    	cla.setSynPermBelowStimulusInc(0.01);
    	cla.setSynPermTrimThreshold(0.05);
    	cla.setOverlapDutyCycles(new double[] { 0, 0.009, 0.1, 0.001, 0.002 });
    	cla.setMinOverlapDutyCycles(new double[] { .01, .01, .01, .01, .01 });
    	
    	int[][] potentialPools = new int[][] {
			{ 1, 1, 1, 1, 0, 0, 0, 0 },
	        { 1, 0, 0, 0, 1, 1, 0, 1 },
	        { 0, 0, 1, 0, 1, 1, 1, 0 },
	        { 1, 1, 1, 0, 0, 0, 1, 0 },
	        { 1, 1, 1, 1, 1, 1, 1, 1 }
    	};
    	
    	double[][] permanences = new double[][] {
    	    { 0.200, 0.120, 0.090, 0.040, 0.000, 0.000, 0.000, 0.000 },
	        { 0.150, 0.000, 0.000, 0.000, 0.180, 0.120, 0.000, 0.450 },
	        { 0.000, 0.000, 0.014, 0.000, 0.032, 0.044, 0.110, 0.000 },
	        { 0.041, 0.000, 0.000, 0.000, 0.000, 0.000, 0.178, 0.000 },
	        { 0.100, 0.738, 0.045, 0.002, 0.050, 0.008, 0.208, 0.034 }	
    	};
    	
    	double[][] truePermanences = new double[][] {
    	    { 0.210, 0.130, 0.100, 0.000, 0.000, 0.000, 0.000, 0.000 },
	        { 0.160, 0.000, 0.000, 0.000, 0.190, 0.130, 0.000, 0.460 },
	        { 0.000, 0.000, 0.014, 0.000, 0.032, 0.044, 0.110, 0.000 },
	        { 0.051, 0.000, 0.000, 0.000, 0.000, 0.000, 0.188, 0.000 },
	        { 0.110, 0.748, 0.055, 0.000, 0.060, 0.000, 0.218, 0.000 }	
    	};
    	
    	Condition<?> cond = new Condition.Adapter<Integer>() {
    		public boolean eval(int n) {
    			return n == 1;
    		}
    	};
    	for(int i = 0;i < cla.getNumColumns();i++) {
    		int[] indexes = ArrayUtils.where(potentialPools[i], cond);
    		cla.getColumn(i).setProximalConnectedSynapsesForTest(cla, indexes);
    		cla.getColumn(i).setProximalPermanences(cla, permanences[i]);
    	}
    	
    	//Execute method being tested
    	sp.bumpUpWeakColumns(cla);
    	
    	for(int i = 0;i < cla.getNumColumns();i++) {
    		double[] perms = cla.getPotentialPools().getIndex(i).getPermanencesDense(cla);
                System.out.println(Arrays.toString(perms));
    		for(int j = 0;j < truePermanences[i].length;j++) {
    			assertEquals(truePermanences[i][j], perms[j], 0.01);
    		}
    	}
    }
    
    @Test public void testArrayReversal() {
        assertTrue( Arrays.equals( new int[] { 1, 2, 3 }, ArrayUtils.reverse( new int[] { 3, 2, 1 } ) ) );
    }
    
    @Test
    public void testUpdateMinDutyCycleLocal() {
    	setupParameters();
    	param.setInputDimensions(new int[] { 5 });
    	param.setColumnDimensions(new int[] { 5 });
    	initSP();
    	
    	SpatialPooler mockSP = new SpatialPooler() {
    		int returnIndex = 0;
    		int[][] returnVals =  {
				{0, 1, 2},
				{1, 2, 3},
				{2, 3, 4},
				{0, 2, 3},
				{0, 1, 3}};
    		@Override
    		public TIntArrayList getNeighborsND(
    			CLA c, int columnIndex, IndexedMatrix<?> topology, double radius, boolean wrapAround) {
    			return new TIntArrayList(returnVals[returnIndex++]);
    		}
    	};
    	
    	cla.setMinPctOverlapDutyCycles(0.04);
    	cla.setOverlapDutyCycles(new double[] { 1.4, 0.5, 1.2, 0.8, 0.1 });
    	double[] trueMinOverlapDutyCycles = new double[] {
    		0.04*1.4, 0.04*1.2, 0.04*1.2, 0.04*1.4, 0.04*1.4 };
    	
    	cla.setMinPctActiveDutyCycles(0.02);
    	cla.setActiveDutyCycles(new double[] { 0.4, 0.5, 0.2, 0.18, 0.1 });
    	double[] trueMinActiveDutyCycles = new double[] {
    		0.02*0.5, 0.02*0.5, 0.02*0.2, 0.02*0.4, 0.02*0.5 };
    	
    	double[] mins = new double[cla.getNumColumns()];
    	Arrays.fill(mins, 0);
    	cla.setMinOverlapDutyCycles(mins);
    	cla.setMinActiveDutyCycles(Arrays.copyOf(mins, mins.length));
    	mockSP.updateMinDutyCyclesLocal(cla);
    	for(int i = 0;i < trueMinOverlapDutyCycles.length;i++) {
    		assertEquals(trueMinOverlapDutyCycles[i], cla.getMinOverlapDutyCycles()[i], 0.01);
    		assertEquals(trueMinActiveDutyCycles[i], cla.getMinActiveDutyCycles()[i], 0.01);
    	}
    	
    	///////////////////////
    	
    	setupParameters();
    	param.setInputDimensions(new int[] { 8 });
    	param.setColumnDimensions(new int[] { 8 });
    	initSP();
    	
    	mockSP = new SpatialPooler() {
    		int returnIndex = 0;
    		int[][] returnVals =  {
				{0, 1, 2, 3, 4},
				{1, 2, 3, 4, 5},
				{2, 3, 4, 6, 7},
				{0, 2, 4, 6},
				{1, 6},
				{3, 5, 7},
				{1, 4, 5, 6},
				{2, 3, 6, 7}};
    		@Override
    		public TIntArrayList getNeighborsND(
    			CLA c, int columnIndex, IndexedMatrix<?> topology, double radius, boolean wrapAround) {
    			return new TIntArrayList(returnVals[returnIndex++]);
    		}
    	};
    	
    	cla.setMinPctOverlapDutyCycles(0.01);
    	cla.setOverlapDutyCycles(new double[] { 1.2, 2.7, 0.9, 1.1, 4.3, 7.1, 2.3, 0.0 });
    	trueMinOverlapDutyCycles = new double[] {
    		0.01*4.3, 0.01*7.1, 0.01*4.3, 0.01*4.3, 
    		0.01*2.7, 0.01*7.1, 0.01*7.1, 0.01*2.3 };
    	
    	cla.setMinPctActiveDutyCycles(0.03);
    	cla.setActiveDutyCycles(new double[] { 0.14, 0.25, 0.125, 0.33, 0.27, 0.11, 0.76, 0.31 });
    	trueMinActiveDutyCycles = new double[] {
    		0.03*0.33, 0.03*0.33, 0.03*0.76, 0.03*0.76, 
    		0.03*0.76, 0.03*0.33, 0.03*0.76, 0.03*0.76 };
    	
    	mins = new double[cla.getNumColumns()];
    	Arrays.fill(mins, 0);
    	cla.setMinOverlapDutyCycles(mins);
    	cla.setMinActiveDutyCycles(Arrays.copyOf(mins, mins.length));
    	mockSP.updateMinDutyCyclesLocal(cla);
    	for(int i = 0;i < trueMinOverlapDutyCycles.length;i++) {
//    		System.out.println(i + ") " + trueMinOverlapDutyCycles[i] + "  -  " +  mem.getMinOverlapDutyCycles()[i]);
//    		System.out.println(i + ") " + trueMinActiveDutyCycles[i] + "  -  " +  mem.getMinActiveDutyCycles()[i]);
    		assertEquals(trueMinOverlapDutyCycles[i], cla.getMinOverlapDutyCycles()[i], 0.01);
    		assertEquals(trueMinActiveDutyCycles[i], cla.getMinActiveDutyCycles()[i], 0.01);
    	}
    }
    
    @Test
    public void testUpdateMinDutyCycleGlobal() {
    	setupParameters();
    	param.setInputDimensions(new int[] { 5 });
    	param.setColumnDimensions(new int[] { 5 });
    	initSP();
    	
    	cla.setMinPctOverlapDutyCycles(0.01);
    	cla.setMinPctActiveDutyCycles(0.02);
    	cla.setOverlapDutyCycles(new double[] { 0.06, 1, 3, 6, 0.5 });
    	cla.setActiveDutyCycles(new double[] { 0.6, 0.07, 0.5, 0.4, 0.3 });
    	
    	sp.updateMinDutyCyclesGlobal(cla);
    	double[] trueMinActiveDutyCycles = new double[cla.getNumColumns()];
    	Arrays.fill(trueMinActiveDutyCycles, 0.02*0.6);
    	double[] trueMinOverlapDutyCycles = new double[cla.getNumColumns()];
    	Arrays.fill(trueMinOverlapDutyCycles, 0.01*6);
    	for(int i = 0;i < cla.getNumColumns();i++) {
//    		System.out.println(i + ") " + trueMinOverlapDutyCycles[i] + "  -  " +  mem.getMinOverlapDutyCycles()[i]);
//    		System.out.println(i + ") " + trueMinActiveDutyCycles[i] + "  -  " +  mem.getMinActiveDutyCycles()[i]);
    		assertEquals(trueMinOverlapDutyCycles[i], cla.getMinOverlapDutyCycles()[i], 0.01);
    		assertEquals(trueMinActiveDutyCycles[i], cla.getMinActiveDutyCycles()[i], 0.01);
    	}
    }
    
    /**
     * Tests that duty cycles are updated properly according
     * to the mathematical formula. also check the effects of
     * supplying a maxPeriod to the function.
     */
    @Test
    public void testUpdateDutyCycleHelper() {
    	setupParameters();
    	param.setInputDimensions(new int[] { 5 });
    	param.setColumnDimensions(new int[] { 5 });
    	initSP();
    	
    	double[] dc = new double[5];
    	Arrays.fill(dc, 1000.0);
    	double[] newvals = new double[5];
    	int period = 1000;
    	double[] newDc = sp.updateDutyCyclesHelper(cla, dc, newvals, period);
    	double[] trueNewDc = new double[] { 999, 999, 999, 999, 999 };
    	assertTrue(Arrays.equals(trueNewDc, newDc));
    	
    	dc = new double[5];
    	Arrays.fill(dc, 1000.0);
    	newvals = new double[5];
    	Arrays.fill(newvals, 1000);
    	period = 1000;
    	newDc = sp.updateDutyCyclesHelper(cla, dc, newvals, period);
    	trueNewDc = Arrays.copyOf(dc, 5);
    	assertTrue(Arrays.equals(trueNewDc, newDc));
    	
    	dc = new double[5];
    	Arrays.fill(dc, 1000.0);
    	newvals = new double[] { 2000, 4000, 5000, 6000, 7000 };
    	period = 1000;
    	newDc = sp.updateDutyCyclesHelper(cla, dc, newvals, period);
    	trueNewDc = new double[] { 1001, 1003, 1004, 1005, 1006 };
    	assertTrue(Arrays.equals(trueNewDc, newDc));
    	
    	dc = new double[] { 1000, 800, 600, 400, 2000 };
    	newvals = new double[5];
    	period = 2;
    	newDc = sp.updateDutyCyclesHelper(cla, dc, newvals, period);
    	trueNewDc = new double[] { 500, 400, 300, 200, 1000 };
    	assertTrue(Arrays.equals(trueNewDc, newDc));
    }
    
    @Test
    public void testIsUpdateRound() {
    	setupParameters();
    	param.setInputDimensions(new int[] { 5 });
    	param.setColumnDimensions(new int[] { 5 });
    	initSP();
    	
    	cla.setUpdatePeriod(50);
    	cla.setIterationNum(1);
    	assertFalse(sp.isUpdateRound(cla));
    	cla.setIterationNum(39);
    	assertFalse(sp.isUpdateRound(cla));
    	cla.setIterationNum(50);
    	assertTrue(sp.isUpdateRound(cla));
    	cla.setIterationNum(1009);
    	assertFalse(sp.isUpdateRound(cla));
    	cla.setIterationNum(1250);
    	assertTrue(sp.isUpdateRound(cla));
    	
    	cla.setUpdatePeriod(125);
    	cla.setIterationNum(0);
    	assertTrue(sp.isUpdateRound(cla));
    	cla.setIterationNum(200);
    	assertFalse(sp.isUpdateRound(cla));
    	cla.setIterationNum(249);
    	assertFalse(sp.isUpdateRound(cla));
    	cla.setIterationNum(1330);
    	assertFalse(sp.isUpdateRound(cla));
    	cla.setIterationNum(1249);
    	assertFalse(sp.isUpdateRound(cla));
    	cla.setIterationNum(1375);
    	assertTrue(sp.isUpdateRound(cla));
    	
    }
    
    @Test
    public void testAdaptSynapses() {
    	setupParameters();
    	param.setInputDimensions(new int[] { 8 });
    	param.setColumnDimensions(new int[] { 4 });
    	param.setSynPermInactiveDec(0.01);
    	param.setSynPermActiveInc(0.1);
    	initSP();
    	
    	cla.setSynPermTrimThreshold(0.05);
    	
    	int[][] potentialPools = new int[][] {
			{ 1, 1, 1, 1, 0, 0, 0, 0 },
	        { 1, 0, 0, 0, 1, 1, 0, 1 },
	        { 0, 0, 1, 0, 0, 0, 1, 0 },
	        { 1, 0, 0, 0, 0, 0, 1, 0 }
    	};
    	
    	double[][] permanences = new double[][] {
    	    { 0.200, 0.120, 0.090, 0.040, 0.000, 0.000, 0.000, 0.000 },
	        { 0.150, 0.000, 0.000, 0.000, 0.180, 0.120, 0.000, 0.450 },
	        { 0.000, 0.000, 0.014, 0.000, 0.000, 0.000, 0.110, 0.000 },
	        { 0.040, 0.000, 0.000, 0.000, 0.000, 0.000, 0.178, 0.000 }
	    };
    	
    	double[][] truePermanences = new double[][] {
    	    { 0.300, 0.110, 0.080, 0.140, 0.000, 0.000, 0.000, 0.000 },
	        { 0.250, 0.000, 0.000, 0.000, 0.280, 0.110, 0.000, 0.440 },
	        { 0.000, 0.000, 0.000, 0.000, 0.000, 0.000, 0.210, 0.000 },
	        { 0.040, 0.000, 0.000, 0.000, 0.000, 0.000, 0.178, 0.000 }
	    };
    	
    	Condition<?> cond = new Condition.Adapter<Integer>() {
    		public boolean eval(int n) {
    			return n == 1;
    		}
    	};
    	for(int i = 0;i < cla.getNumColumns();i++) {
    		int[] indexes = ArrayUtils.where(potentialPools[i], cond);
    		cla.getColumn(i).setProximalConnectedSynapsesForTest(cla, indexes);
    		cla.getColumn(i).setProximalPermanences(cla, permanences[i]);
    	}
    	
    	int[] inputVector = new int[] { 1, 0, 0, 1, 1, 0, 1, 0 };
    	int[] activeColumns = new int[] { 0, 1, 2 };
    	
    	sp.adaptSynapses(cla, inputVector, activeColumns);
    	
    	for(int i = 0;i < cla.getNumColumns();i++) {
    		double[] perms = cla.getPotentialPools().getIndex(i).getPermanencesDense(cla);
    		for(int j = 0;j < truePermanences[i].length;j++) {
    			assertEquals(truePermanences[i][j], perms[j], 0.01);
    		}
    	}
    	
    	//////////////////////////////
    	
    	potentialPools = new int[][] {
			{ 1, 1, 1, 0, 0, 0, 0, 0 },
	        { 0, 1, 1, 1, 0, 0, 0, 0 },
	        { 0, 0, 1, 1, 1, 0, 0, 0 },
	        { 1, 0, 0, 0, 0, 0, 1, 0 }
    	};
    	
    	permanences = new double[][] {
    	    { 0.200, 0.120, 0.090, 0.000, 0.000, 0.000, 0.000, 0.000 },
	        { 0.000, 0.017, 0.232, 0.400, 0.180, 0.120, 0.000, 0.450 },
	        { 0.000, 0.000, 0.014, 0.051, 0.730, 0.000, 0.000, 0.000 },
	        { 0.170, 0.000, 0.000, 0.000, 0.000, 0.000, 0.380, 0.000 }
	    };
    	
    	truePermanences = new double[][] {
    	    { 0.300, 0.110, 0.080, 0.000, 0.000, 0.000, 0.000, 0.000 },
	        { 0.000, 0.000, 0.222, 0.500, 0.000, 0.000, 0.000, 0.000 },
	        { 0.000, 0.000, 0.000, 0.151, 0.830, 0.000, 0.000, 0.000 },
	        { 0.170, 0.000, 0.000, 0.000, 0.000, 0.000, 0.380, 0.000 }
	    };
    	
    	for(int i = 0;i < cla.getNumColumns();i++) {
    		int[] indexes = ArrayUtils.where(potentialPools[i], cond);
    		cla.getColumn(i).setProximalConnectedSynapsesForTest(cla, indexes);
    		cla.getColumn(i).setProximalPermanences(cla, permanences[i]);
    	}
    	
    	sp.adaptSynapses(cla, inputVector, activeColumns);
    	
    	for(int i = 0;i < cla.getNumColumns();i++) {
    		double[] perms = cla.getPotentialPools().getIndex(i).getPermanencesDense(cla);
    		for(int j = 0;j < truePermanences[i].length;j++) {
    			assertEquals(truePermanences[i][j], perms[j], 0.01);
    		}
    	}
    }
    
    @Test
    public void testUpdateInhibitionRadius() {
    	setupParameters();
    	initSP();
    	 
    	//Test global inhibition case
    	cla.setGlobalInhibition(true);
    	cla.setColumnDimensions(57, 31, 2);
    	sp.updateInhibitionRadius(cla);
    	assertEquals(57, cla.getInhibitionRadius());
    	
    	// ((3 * 4) - 1) / 2 => round up
    	SpatialPooler mock = new SpatialPooler() {
    		public double avgConnectedSpanForColumnND(CLA c, int columnIndex) {
    			return 3;
    		}
    		
    		public double avgColumnsPerInput(CLA c) {
    			return 4;
    		}
    	};
    	cla.setGlobalInhibition(false);
    	sp = mock;
    	sp.updateInhibitionRadius(cla);
    	assertEquals(6, cla.getInhibitionRadius());
    	
    	//Test clipping at 1.0
    	mock = new SpatialPooler() {
    		public double avgConnectedSpanForColumnND(CLA c, int columnIndex) {
    			return 0.5;
    		}
    		
    		public double avgColumnsPerInput(CLA c) {
    			return 1.2;
    		}
    	};
    	cla.setGlobalInhibition(false);
    	sp = mock;
    	sp.updateInhibitionRadius(cla);
    	assertEquals(1, cla.getInhibitionRadius());
    	
    	//Test rounding up
    	mock = new SpatialPooler() {
    		public double avgConnectedSpanForColumnND(CLA c, int columnIndex) {
    			return 2.4;
    		}
    		
    		public double avgColumnsPerInput(CLA c) {
    			return 2;
    		}
    	};
    	cla.setGlobalInhibition(false);
    	sp = mock;
    	//((2 * 2.4) - 1) / 2.0 => round up
    	sp.updateInhibitionRadius(cla);
    	assertEquals(2, cla.getInhibitionRadius());
    }
    
    @Test
    public void testAvgColumnsPerInput() {
    	setupParameters();
    	initSP();
    	 
    	cla.setColumnDimensions(new int[] { 2, 2, 2, 2 });
    	cla.setInputDimensions(new int[] { 4, 4, 4, 4 });
    	assertEquals(0.5, sp.avgColumnsPerInput(cla), 0);
    	
    	cla.setColumnDimensions(new int[] { 2, 2, 2, 2 });
    	cla.setInputDimensions(new int[] { 7, 5, 1, 3 });
    	double trueAvgColumnPerInput = (2.0/7 + 2.0/5 + 2.0/1 + 2/3.0) / 4.0d;
    	assertEquals(trueAvgColumnPerInput, sp.avgColumnsPerInput(cla), 0);
    	
    	cla.setColumnDimensions(new int[] { 3, 3 });
    	cla.setInputDimensions(new int[] { 3, 3 });
    	trueAvgColumnPerInput = 1;
    	assertEquals(trueAvgColumnPerInput, sp.avgColumnsPerInput(cla), 0);
    	
    	cla.setColumnDimensions(new int[] { 25 });
    	cla.setInputDimensions(new int[] { 5 });
    	trueAvgColumnPerInput = 5;
    	assertEquals(trueAvgColumnPerInput, sp.avgColumnsPerInput(cla), 0);
    	
    	cla.setColumnDimensions(new int[] { 3, 3, 3, 5, 5, 6, 6 });
    	cla.setInputDimensions(new int[] { 3, 3, 3, 5, 5, 6, 6 });
    	trueAvgColumnPerInput = 1;
    	assertEquals(trueAvgColumnPerInput, sp.avgColumnsPerInput(cla), 0);
    	
    	cla.setColumnDimensions(new int[] { 3, 6, 9, 12 });
    	cla.setInputDimensions(new int[] { 3, 3, 3 , 3 });
    	trueAvgColumnPerInput = 2.5;
    	assertEquals(trueAvgColumnPerInput, sp.avgColumnsPerInput(cla), 0);
    }
    
    /**
     * As coded in the Python test
     */
    @Test
    public void testGetNeighborsND() {
        //This setup isn't relevant to this test
        setupParameters();
        param.setInputDimensions(new int[] { 9, 5 });
        param.setColumnDimensions(new int[] { 5, 5 });
        initSP();
        ////////////////////// Test not part of Python port /////////////////////
        int[] result = sp.getNeighborsND(cla, 2, cla.getInputMatrix(), 3, true).toArray();
        int[] expected = new int[] { 
            0, 1, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 
            13, 14, 15, 16, 17, 18, 19, 30, 31, 32, 33, 
            34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44 
        };
        for(int i = 0;i < result.length;i++) {
            assertEquals(expected[i], result[i]);
        }
        /////////////////////////////////////////////////////////////////////////
        setupParameters();
        int[] dimensions = new int[] { 5, 7, 2 };
        param.setInputDimensions(dimensions);
        param.setColumnDimensions(dimensions);
        initSP();
        int radius = 1;
        int x = 1;
        int y = 3;
        int z = 2;
        int columnIndex = cla.getInputMatrix().computeIndex(new int[] { z, y, x });
        int[] neighbors = sp.getNeighborsND(cla, columnIndex, cla.getInputMatrix(), radius, true).toArray();
        String expect = "[18, 19, 20, 21, 22, 23, 32, 33, 34, 36, 37, 46, 47, 48, 49, 50, 51]";
        assertEquals(expect, ArrayUtils.print1DArray(neighbors));
        
        /////////////////////////////////////////
        setupParameters();
        dimensions = new int[] { 5, 7, 9 };
        param.setInputDimensions(dimensions);
        param.setColumnDimensions(dimensions);
        initSP();
        radius = 3;
        x = 0;
        y = 0;
        z = 3;
        columnIndex = cla.getInputMatrix().computeIndex(new int[] { z, y, x });
        neighbors = sp.getNeighborsND(cla, columnIndex, cla.getInputMatrix(), radius, true).toArray();
        expect = "[0, 1, 2, 3, 6, 7, 8, 9, 10, 11, 12, 15, 16, 17, 18, 19, 20, 21, 24, 25, 26, "
                + "27, 28, 29, 30, 33, 34, 35, 36, 37, 38, 39, 42, 43, 44, 45, 46, 47, 48, 51, "
                + "52, 53, 54, 55, 56, 57, 60, 61, 62, 63, 64, 65, 66, 69, 70, 71, 72, 73, 74, "
                + "75, 78, 79, 80, 81, 82, 83, 84, 87, 88, 89, 90, 91, 92, 93, 96, 97, 98, 99, "
                + "100, 101, 102, 105, 106, 107, 108, 109, 110, 111, 114, 115, 116, 117, 118, 119, "
                + "120, 123, 124, 125, 126, 127, 128, 129, 132, 133, 134, 135, 136, 137, 138, 141, "
                + "142, 143, 144, 145, 146, 147, 150, 151, 152, 153, 154, 155, 156, 159, 160, 161, "
                + "162, 163, 164, 165, 168, 169, 170, 171, 172, 173, 174, 177, 178, 179, 180, 181, "
                + "182, 183, 186, 187, 188, 190, 191, 192, 195, 196, 197, 198, 199, 200, 201, 204, "
                + "205, 206, 207, 208, 209, 210, 213, 214, 215, 216, 217, 218, 219, 222, 223, 224, "
                + "225, 226, 227, 228, 231, 232, 233, 234, 235, 236, 237, 240, 241, 242, 243, 244, "
                + "245, 246, 249, 250, 251, 252, 253, 254, 255, 258, 259, 260, 261, 262, 263, 264, "
                + "267, 268, 269, 270, 271, 272, 273, 276, 277, 278, 279, 280, 281, 282, 285, 286, "
                + "287, 288, 289, 290, 291, 294, 295, 296, 297, 298, 299, 300, 303, 304, 305, 306, "
                + "307, 308, 309, 312, 313, 314]";
        assertEquals(expect, ArrayUtils.print1DArray(neighbors));
        
        /////////////////////////////////////////
        setupParameters();
        dimensions = new int[] { 5, 10, 7, 6 };
        param.setInputDimensions(dimensions);
        param.setColumnDimensions(dimensions);
        initSP();
        
        radius = 4;
        int w = 2;
        x = 5;
        y = 6;
        z = 2;
        columnIndex = cla.getInputMatrix().computeIndex(new int[] { z, y, x, w });
        neighbors = sp.getNeighborsND(cla, columnIndex, cla.getInputMatrix(), radius, true).toArray();
        TIntHashSet trueNeighbors = new TIntHashSet();
        for(int i = -radius;i <= radius;i++) {
            for(int j = -radius;j <= radius;j++) {
                for(int k = -radius;k <= radius;k++) {
                    for(int m = -radius;m <= radius;m++) {
                        int zprime = (int)ArrayUtils.positiveRemainder((z + i), dimensions[0]);
                        int yprime = (int)ArrayUtils.positiveRemainder((y + j), dimensions[1]);
                        int xprime = (int)ArrayUtils.positiveRemainder((x + k), dimensions[2]);
                        int wprime = (int)ArrayUtils.positiveRemainder((w + m), dimensions[3]);
                        trueNeighbors.add(cla.getInputMatrix().computeIndex(new int[] { zprime, yprime, xprime, wprime }));
                    }
                }
            }
        }
        trueNeighbors.remove(columnIndex);
        int[] tneighbors = ArrayUtils.unique(trueNeighbors.toArray());
        assertEquals(ArrayUtils.print1DArray(tneighbors), ArrayUtils.print1DArray(neighbors));
        
        /////////////////////////////////////////
        //Tests from getNeighbors1D from Python unit test
        setupParameters();
        dimensions = new int[] { 8 };
        param.setColumnDimensions(dimensions);
        param.setInputDimensions(dimensions);
        initSP();
        SparseBinaryMatrix sbm = (SparseBinaryMatrix)cla.getInputMatrix();
        sbm.set(new int[] { 2, 4 }, new byte[] { 1, 1 }, true);
        radius = 1;
        columnIndex = 3;
        int[] mask = sp.getNeighborsND(cla, columnIndex, cla.getInputMatrix(), radius, true).toArray();
        TIntArrayList msk = new TIntArrayList(mask);
        TIntArrayList neg = new TIntArrayList(ArrayUtils.range(0, dimensions[0]));
        neg.removeAll(msk);
        assertTrue(sbm.all(mask));
        assertFalse(sbm.any(neg));
        
        //////
        setupParameters();
        dimensions = new int[] { 8 };
        param.setInputDimensions(dimensions);
        initSP();
        sbm = (SparseBinaryMatrix)cla.getInputMatrix();
        sbm.set(new int[] { 1, 2, 4, 5 }, new byte[] { 1, 1, 1, 1 }, true);
        radius = 2;
        columnIndex = 3;
        mask = sp.getNeighborsND(cla, columnIndex, cla.getInputMatrix(), radius, true).toArray();
        msk = new TIntArrayList(mask);
        neg = new TIntArrayList(ArrayUtils.range(0, dimensions[0]));
        neg.removeAll(msk);
        assertTrue(sbm.all(mask));
        assertFalse(sbm.any(neg));
        
        //Wrap around
        setupParameters();
        dimensions = new int[] { 8 };
        param.setInputDimensions(dimensions);
        initSP();
        sbm = (SparseBinaryMatrix)cla.getInputMatrix();
        sbm.set(new int[] { 1, 2, 6, 7 }, new byte[] { 1, 1, 1, 1 }, true);
        radius = 2;
        columnIndex = 0;
        mask = sp.getNeighborsND(cla, columnIndex, cla.getInputMatrix(), radius, true).toArray();
        msk = new TIntArrayList(mask);
        neg = new TIntArrayList(ArrayUtils.range(0, dimensions[0]));
        neg.removeAll(msk);
        assertTrue(sbm.all(mask));
        assertFalse(sbm.any(neg));
        
        //Radius too big
        setupParameters();
        dimensions = new int[] { 8 };
        param.setInputDimensions(dimensions);
        initSP();
        sbm = (SparseBinaryMatrix)cla.getInputMatrix();
        sbm.set(new int[] { 0, 1, 2, 3, 4, 5, 7 }, new byte[] { 1, 1, 1, 1, 1, 1, 1 }, true);
        radius = 20;
        columnIndex = 6;
        mask = sp.getNeighborsND(cla, columnIndex, cla.getInputMatrix(), radius, true).toArray();
        msk = new TIntArrayList(mask);
        neg = new TIntArrayList(ArrayUtils.range(0, dimensions[0]));
        neg.removeAll(msk);
        assertTrue(sbm.all(mask));
        assertFalse(sbm.any(neg));
        
        //These are all the same tests from 2D
        setupParameters();
        dimensions = new int[] { 6, 5 };
        param.setInputDimensions(dimensions);
        param.setColumnDimensions(dimensions);
        initSP();
        sbm = (SparseBinaryMatrix)cla.getInputMatrix();
        int[][] input = new int[][] { {0, 0, 0, 0, 0},
                                  {0, 0, 0, 0, 0},
                                  {0, 1, 1, 1, 0},
                                  {0, 1, 0, 1, 0},
                                  {0, 1, 1, 1, 0},
                                  {0, 0, 0, 0, 0}};
        for(int i = 0;i < input.length;i++) {
            for(int j = 0;j < input[i].length;j++) {
                if(input[i][j] == 1) 
                    sbm.set(true, i, j);
            }
        }
        radius = 1;
        columnIndex = 3*5 + 2;
        mask = sp.getNeighborsND(cla, columnIndex, cla.getInputMatrix(), radius, true).toArray();
        msk = new TIntArrayList(mask);
        neg = new TIntArrayList(ArrayUtils.range(0, dimensions[0]));
        neg.removeAll(msk);
        assertTrue(sbm.all(mask));
        assertFalse(sbm.any(neg));
        
        ////////
        setupParameters();
        dimensions = new int[] { 6, 5 };
        param.setInputDimensions(dimensions);
        param.setColumnDimensions(dimensions);
        initSP();
        sbm = (SparseBinaryMatrix)cla.getInputMatrix();
        input = new int[][] { {0, 0, 0, 0, 0},
                              {1, 1, 1, 1, 1},
                              {1, 1, 1, 1, 1},
                              {1, 1, 0, 1, 1},
                              {1, 1, 1, 1, 1},
                              {1, 1, 1, 1, 1}};
        for(int i = 0;i < input.length;i++) {
            for(int j = 0;j < input[i].length;j++) {
                if(input[i][j] == 1) 
                    sbm.setIndex(true, sbm.computeIndex(new int[] { i, j }));
            }
        }
        radius = 2;
        columnIndex = 3*5 + 2;
        mask = sp.getNeighborsND(cla, columnIndex, cla.getInputMatrix(), radius, true).toArray();
        msk = new TIntArrayList(mask);
        neg = new TIntArrayList(ArrayUtils.range(0, dimensions[0]));
        neg.removeAll(msk);
        assertTrue(sbm.all(mask));
        assertFalse(sbm.any(neg));
        
        //Radius too big
        setupParameters();
        dimensions = new int[] { 6, 5 };
        param.setInputDimensions(dimensions);
        param.setColumnDimensions(dimensions);
        initSP();
        sbm = (SparseBinaryMatrix)cla.getInputMatrix();
        input = new int[][] { {1, 1, 1, 1, 1},
                              {1, 1, 1, 1, 1},
                              {1, 1, 1, 1, 1},
                              {1, 1, 0, 1, 1},
                              {1, 1, 1, 1, 1},
                              {1, 1, 1, 1, 1}};
        for(int i = 0;i < input.length;i++) {
            for(int j = 0;j < input[i].length;j++) {
                if(input[i][j] == 1) 
                    sbm.setIndex(true, sbm.computeIndex(new int[] { i, j }));
            }
        }
        radius = 7;
        columnIndex = 3*5 + 2;
        mask = sp.getNeighborsND(cla, columnIndex, cla.getInputMatrix(), radius, true).toArray();
        msk = new TIntArrayList(mask);
        neg = new TIntArrayList(ArrayUtils.range(0, dimensions[0]));
        neg.removeAll(msk);
        assertTrue(sbm.all(mask));
        assertFalse(sbm.any(neg));
        
        //Wrap-around
        setupParameters();
        dimensions = new int[] { 6, 5 };
        param.setInputDimensions(dimensions);
        param.setColumnDimensions(dimensions);
        initSP();
        sbm = (SparseBinaryMatrix)cla.getInputMatrix();
        input = new int[][] { {1, 0, 0, 1, 1},
                              {0, 0, 0, 0, 0},
                              {0, 0, 0, 0, 0},
                              {0, 0, 0, 0, 0},
                              {1, 0, 0, 1, 1},
                              {1, 0, 0, 1, 0}};
        for(int i = 0;i < input.length;i++) {
            for(int j = 0;j < input[i].length;j++) {
                if(input[i][j] == 1) 
                    sbm.setIndex(true, sbm.computeIndex(new int[] { i, j }));
            }
        }
        radius = 1;
        columnIndex = sbm.getMaxIndex();
        mask = sp.getNeighborsND(cla, columnIndex, cla.getInputMatrix(), radius, true).toArray();
        msk = new TIntArrayList(mask);
        neg = new TIntArrayList(ArrayUtils.range(0, dimensions[0]));
        neg.removeAll(msk);
        assertTrue(sbm.all(mask));
        assertFalse(sbm.any(neg));
    }
    
    @Test
    public void testRaisePermanenceThreshold() {
    	setupParameters();
    	param.setInputDimensions(new int[] { 5 });
    	param.setColumnDimensions(new int[] { 5 });
    	param.setSynPermConnected(0.1);
    	param.setStimulusThreshold(3);
    	param.setSynPermBelowStimulusInc(0.01);
    	//The following parameter is not set to "1" in the Python version
    	//This is necessary to reproduce the test conditions of having as
    	//many pool members as Input Bits, which would never happen under
    	//normal circumstances because we want to enforce sparsity
    	param.setPotentialPct(1);
    	
    	initSP();
    	
    	//We set the values on the Connections permanences here just for illustration
    	SparseObjectMatrix<double[]> objMatrix = new SparseObjectMatrix<>(new int[] { 5, 5 });
    	objMatrix.setIndex(new double[] { 0.0, 0.11, 0.095, 0.092, 0.01 }, 0);
    	objMatrix.setIndex(new double[] { 0.12, 0.15, 0.02, 0.12, 0.09 }, 1);
    	objMatrix.setIndex(new double[] { 0.51, 0.081, 0.025, 0.089, 0.31 }, 2);
    	objMatrix.setIndex(new double[] { 0.18, 0.0601, 0.11, 0.011, 0.03 }, 3);
    	objMatrix.setIndex(new double[] { 0.011, 0.011, 0.011, 0.011, 0.011 }, 4);
    	cla.setPermanences(objMatrix);
    	
//    	mem.setConnectedSynapses(new SparseObjectMatrix<int[]>(new int[] { 5, 5 }));
//    	SparseObjectMatrix<int[]> syns = mem.getConnectedSynapses();
//    	syns.set(0, new int[] { 0, 1, 0, 0, 0 });
//    	syns.set(1, new int[] { 1, 1, 0, 1, 0 });
//    	syns.set(2, new int[] { 1, 0, 0, 0, 1 });
//    	syns.set(3, new int[] { 1, 0, 1, 0, 0 });
//    	syns.set(4, new int[] { 0, 0, 0, 0, 0 });
    	
    	cla.setConnectedCounts(new int[] { 1, 3, 2, 2, 0 });
    	
    	double[][] truePermanences = new double[][] { 
    		{0.01, 0.12, 0.105, 0.102, 0.02},  		// incremented once
            {0.12, 0.15, 0.02, 0.12, 0.09},  		// no change
            {0.53, 0.101, 0.045, 0.109, 0.33},  	// increment twice
            {0.22, 0.1001, 0.15, 0.051, 0.07},  	// increment four times
            {0.101, 0.101, 0.101, 0.101, 0.101}};	// increment 9 times
    	
    	//FORGOT TO SET PERMANENCES ABOVE - DON'T USE mem.setPermanences() 
    	int[] indices = cla.getMemory().getSparseIndices();
    	for(int i = 0;i < cla.getNumColumns();i++) {
    		double[] perm = cla.getPotentialPools().getIndex(i).getPermanencesSparse();
    		sp.raisePermanenceToThreshold(cla, perm, indices);
    		
    		for(int j = 0;j < perm.length;j++) {
    			assertEquals(truePermanences[i][j], perm[j], 0.001);
    		}
    	}
    }
    
    @Test
    public void testUpdatePermanencesForColumn() {
    	setupParameters();
    	param.setInputDimensions(new int[] { 5 });
    	param.setColumnDimensions(new int[] { 5 });
    	param.setSynPermTrimThreshold(0.05);
    	//The following parameter is not set to "1" in the Python version
    	//This is necessary to reproduce the test conditions of having as
    	//many pool members as Input Bits, which would never happen under
    	//normal circumstances because we want to enforce sparsity
    	param.setPotentialPct(1);
    	initSP();

    	double[][] permanences = new double[][] {
    		{-0.10, 0.500, 0.400, 0.010, 0.020},
	        {0.300, 0.010, 0.020, 0.120, 0.090},
	        {0.070, 0.050, 1.030, 0.190, 0.060},
	        {0.180, 0.090, 0.110, 0.010, 0.030},
	        {0.200, 0.101, 0.050, -0.09, 1.100}};
    	
    	int[][] trueConnectedSynapses = new int[][] {
            {0, 1, 1, 0, 0},
            {1, 0, 0, 1, 0},
            {0, 0, 1, 1, 0},
            {1, 0, 1, 0, 0},
            {1, 1, 0, 0, 1}};
    	
    	int[][] connectedDense = new int[][] {
    		{ 1, 2 },
    		{ 0, 3 },
    		{ 2, 3 },
    		{ 0, 2 },
    		{ 0, 1, 4 }
    	};
    	
    	double[] trueConnectedCounts = new double[] {2, 2, 2, 2, 3};
    
    	for(int i = 0;i < cla.getNumColumns();i++) {
    		cla.getColumn(i).setProximalPermanences(cla, permanences[i]);
    		sp.updatePermanencesForColumn(cla, permanences[i], cla.getColumn(i), connectedDense[i], true);
    		int[] dense = cla.getColumn(i).getProximalDendrite().getConnectedSynapsesDense(cla);
    		assertEquals(Arrays.toString(trueConnectedSynapses[i]), Arrays.toString(dense));
    	}
    	
    	assertEquals(Arrays.toString(trueConnectedCounts), Arrays.toString(cla.getConnectedCounts().getTrueCounts()));
    }

    @Test
    public void testCalculateOverlap() {
    	setupParameters();
    	param.setInputDimensions(new int[] { 10 });
    	param.setColumnDimensions(new int[] { 5 });
    	initSP();
    	
    	int[] dimensions = new int[] { 5, 10 };
    	byte[][] connectedSynapses = new byte[][] {
			{1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		    {0, 0, 1, 1, 1, 1, 1, 1, 1, 1},
		    {0, 0, 0, 0, 1, 1, 1, 1, 1, 1},
		    {0, 0, 0, 0, 0, 0, 1, 1, 1, 1},
		    {0, 0, 0, 0, 0, 0, 0, 0, 1, 1}};
    	SparseBinaryMatrixTrueCount sm = new SparseBinaryMatrixTrueCount(dimensions);
		for(int i = 0;i < sm.getDimensions()[0];i++) {
			for(int j = 0;j < sm.getDimensions()[1];j++) {
				sm.set(connectedSynapses[i][j], i, j);
			}
		}
		
		cla.setConnectedMatrix(sm);
		
		for(int i = 0;i < 5;i++) {
			for(int j = 0;j < 10;j++) {
				assertEquals(connectedSynapses[i][j], sm.getIntValue(i, j));
			}
		}
		
		int[] inputVector = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		int[] overlaps = sp.overlapInt(cla, inputVector);
		int[] trueOverlaps = new int[5];
		double[] overlapsPct = sp.calculateOverlapPct(cla, overlaps);
		double[] trueOverlapsPct = new double[5];
		assertTrue(Arrays.equals(trueOverlaps, overlaps));
		assertTrue(Arrays.equals(trueOverlapsPct, overlapsPct));
		
		/////////////////
		
		dimensions = new int[] { 5, 10 };
    	connectedSynapses = new byte[][] {
			{1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		    {0, 0, 1, 1, 1, 1, 1, 1, 1, 1},
		    {0, 0, 0, 0, 1, 1, 1, 1, 1, 1},
		    {0, 0, 0, 0, 0, 0, 1, 1, 1, 1},
		    {0, 0, 0, 0, 0, 0, 0, 0, 1, 1}};
    	sm = new SparseBinaryMatrixTrueCount(dimensions);
		for(int i = 0;i < sm.getDimensions()[0];i++) {
			for(int j = 0;j < sm.getDimensions()[1];j++) {
				sm.set(connectedSynapses[i][j], i, j);
			}
		}
		
		cla.setConnectedMatrix(sm);
		
		for(int i = 0;i < 5;i++) {
			for(int j = 0;j < 10;j++) {
				assertEquals(connectedSynapses[i][j], sm.getIntValue(i, j));
			}
		}
		
		inputVector = new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 };
		overlaps = sp.overlapInt(cla, inputVector);
		trueOverlaps = new int[] { 10, 8, 6, 4, 2 };
		overlapsPct = sp.calculateOverlapPct(cla, overlaps);
		trueOverlapsPct = new double[] { 1, 1, 1, 1, 1 };
		assertTrue(Arrays.equals(trueOverlaps, overlaps));
		assertTrue(Arrays.equals(trueOverlapsPct, overlapsPct));
		
		/////////////////
				
		dimensions = new int[] { 5, 10 };
		connectedSynapses = new byte[][] {
			{1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		    {0, 0, 1, 1, 1, 1, 1, 1, 1, 1},
		    {0, 0, 0, 0, 1, 1, 1, 1, 1, 1},
		    {0, 0, 0, 0, 0, 0, 1, 1, 1, 1},
		    {0, 0, 0, 0, 0, 0, 0, 0, 1, 1}};
		sm = new SparseBinaryMatrixTrueCount(dimensions);
		for(int i = 0;i < sm.getDimensions()[0];i++) {
			for(int j = 0;j < sm.getDimensions()[1];j++) {
				sm.set(connectedSynapses[i][j], i, j);
			}
		}
		
		cla.setConnectedMatrix(sm);
		
		for(int i = 0;i < 5;i++) {
			for(int j = 0;j < 10;j++) {
				assertEquals(connectedSynapses[i][j], sm.getIntValue(i, j));
			}
		}
		
		inputVector = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 };
		overlaps = sp.overlapInt(cla, inputVector);
		trueOverlaps = new int[] { 1, 1, 1, 1, 1 };
		overlapsPct = sp.calculateOverlapPct(cla, overlaps);
		trueOverlapsPct = new double[] { 0.1, 0.125, 1.0/6, 0.25, 0.5 };
		assertTrue(Arrays.equals(trueOverlaps, overlaps));
		assertTrue(Arrays.equals(trueOverlapsPct, overlapsPct));
		
		/////////////////
	    // Zig-zag
		dimensions = new int[] { 5, 10 };
		connectedSynapses = new byte[][] {
			{1, 0, 0, 0, 0, 1, 0, 0, 0, 0},
		    {0, 1, 0, 0, 0, 0, 1, 0, 0, 0},
		    {0, 0, 1, 0, 0, 0, 0, 1, 0, 0},
		    {0, 0, 0, 1, 0, 0, 0, 0, 1, 0},
		    {0, 0, 0, 0, 1, 0, 0, 0, 0, 1}};
		sm = new SparseBinaryMatrixTrueCount(dimensions);
		for(int i = 0;i < sm.getDimensions()[0];i++) {
			for(int j = 0;j < sm.getDimensions()[1];j++) {
				sm.set(connectedSynapses[i][j], i, j);
			}
		}
		
		cla.setConnectedMatrix(sm);
		
		for(int i = 0;i < 5;i++) {
			for(int j = 0;j < 10;j++) {
				assertEquals(connectedSynapses[i][j], sm.getIntValue(i, j));
			}
		}
		
		inputVector = new int[] { 1, 0, 1, 0, 1, 0, 1, 0, 1, 0 };
		overlaps = sp.overlapInt(cla, inputVector);
		trueOverlaps = new int[] { 1, 1, 1, 1, 1 };
		overlapsPct = sp.calculateOverlapPct(cla, overlaps);
		trueOverlapsPct = new double[] { 0.5, 0.5, 0.5, 0.5, 0.5 };
		assertTrue(Arrays.equals(trueOverlaps, overlaps));
		assertTrue(Arrays.equals(trueOverlapsPct, overlapsPct));
    }
    
    /**
     * test initial permanence generation. ensure that
     * a correct amount of synapses are initialized in 
     * a connected state, with permanence values drawn from
     * the correct ranges
     */
    @Test
    public void testInitPermanence() {
    	setupParameters();
    	param.setInputDimensions(new int[] { 10 });
    	param.setColumnDimensions(new int[] { 5 });
    	param.setSynPermTrimThreshold(0);
    	initSP();
    	
    	cla.setPotentialRadius(2);
    	double connectedPct = 1;
    	int[] mask = new int[] { 0, 1, 2, 8, 9 };
    	double[] perm = sp.initPermanence(cla, mask, 0, connectedPct);
    	int numcon = ArrayUtils.valueGreaterCount(cla.getSynPermConnected(), perm);
    	assertEquals(5, numcon, 0);
    	
    	connectedPct = 0;
    	perm = sp.initPermanence(cla, mask, 0, connectedPct);
    	numcon = ArrayUtils.valueGreaterCount(cla.getSynPermConnected(), perm);
    	assertEquals(0, numcon, 0);
    	
    	setupParameters();
    	param.setInputDimensions(new int[] { 100 });
    	param.setColumnDimensions(new int[] { 5 });
    	param.setSynPermTrimThreshold(0);
    	initSP();
    	cla.setPotentialRadius(100);
    	connectedPct = 0.5;
    	mask = new int[100];
    	for(int i = 0;i < 100;i++) mask[i] = i;
    	final double[] perma = sp.initPermanence(cla, mask, 0, connectedPct);
    	numcon = ArrayUtils.valueGreaterCount(cla.getSynPermConnected(), perma);
    	assertTrue(numcon > 0);
    	assertTrue(numcon < cla.getNumInputs());
    	
    	final double minThresh = cla.getSynPermActiveInc() / 2.0d;
    	final double connThresh = cla.getSynPermConnected();
    	double[] results = ArrayUtils.retainLogicalAnd(perma, new Condition[] {
    		new Condition.Adapter<Object>() {
    			public boolean eval(double d) {
    				return d >= minThresh;
    			}
    		},
    		new Condition.Adapter<Object>() {
    			public boolean eval(double d) {
    				return d < connThresh;
    			}
    		}
    	});
    	assertTrue(results.length > 0);
    }
    
    /**
     * Test initial permanence generation. ensure that permanence values
     * are only assigned to bits within a column's potential pool. 
     */
    @Test
    public void testInitPermanence2() {
    	setupParameters();
    	param.setInputDimensions(new int[] { 10 });
    	param.setColumnDimensions(new int[] { 5 });
    	param.setSynPermTrimThreshold(0);
    	initSP();
    	
    	sp = new SpatialPooler() {
    		public void raisePermanenceToThresholdSparse(CLA c, double[] perm) {
    			//Mocked to do nothing as per Python version of test
    		}
    	};
    	
    	double connectedPct = 1;
    	int[] mask = new int[] { 0, 1 };
    	double[] perm = sp.initPermanence(cla, mask, 0, connectedPct);
    	int[] trueConnected = new int[] { 0, 1 };
    	Condition<?> cond = new Condition.Adapter<Object>() {
    		public boolean eval(double d) {
    			return d >= cla.getSynPermConnected();
    		}
    	};
    	assertTrue(Arrays.equals(trueConnected, ArrayUtils.where(perm, cond)));
    	
    	connectedPct = 1;
    	mask = new int[] { 4, 5, 6 };
    	perm = sp.initPermanence(cla, mask, 0, connectedPct);
    	trueConnected = new int[] { 4, 5, 6 };
    	assertTrue(Arrays.equals(trueConnected, ArrayUtils.where(perm, cond)));
    	
    	connectedPct = 1;
    	mask = new int[] { 8, 9 };
    	perm = sp.initPermanence(cla, mask, 0, connectedPct);
    	trueConnected = new int[] { 8, 9 };
    	assertTrue(Arrays.equals(trueConnected, ArrayUtils.where(perm, cond)));
    	
    	connectedPct = 1;
    	mask = new int[] { 0, 1, 2, 3, 4, 5, 6, 8, 9 };
    	perm = sp.initPermanence(cla, mask, 0, connectedPct);
    	trueConnected = new int[] { 0, 1, 2, 3, 4, 5, 6, 8, 9 };
    	assertTrue(Arrays.equals(trueConnected, ArrayUtils.where(perm, cond)));
    }
}
