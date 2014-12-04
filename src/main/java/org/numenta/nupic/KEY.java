/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.numenta.nupic;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.numenta.nupic.model.DistalDendrite;
import org.numenta.nupic.research.SpatialPooler.ColumnRadius;
import org.numenta.nupic.research.TemporalMemory;

/**
 * Constant values representing configuration parameters for the
 * {@link TemporalMemory}
 */
public enum KEY implements Serializable {

     /////////// Universal Parameters ///////////
    
    /**
     * Total number of columns
     */
    COLUMN_DIMENSIONS("columnDimensions", int[].class),
    /**
     * Total number of cells per column
     */
    CELLS_PER_COLUMN("cellsPerColumn", Integer.class, 1, null),
    /**
     * Random Number Generator
     */
    RANDOM("random", Random.class),
    /**
     * Seed for random number generator
     */
    SEED("seed", Integer.class),

    /////////// Temporal Memory Parameters ///////////
    /**
     * If the number of active connected synapses on a segment
     * is at least this threshold, the segment is said to be active.
     */
    ACTIVATION_THRESHOLD("activationThreshold", Integer.class, 0, null),
    
    /**
     * Radius around cell from which it can
     * sample to form distal {@link DistalDendrite} connections.
     */
    LEARNING_RADIUS("learningRadius", Integer.class, 0, null),
    /**
     * If the number of synapses active on a segment is at least this
     * threshold, it is selected as the best matching
     * cell in a bursting column.
     */
    MIN_THRESHOLD("minThreshold", Integer.class, 0, null),
    /**
     * The maximum number of synapses added to a segment during learning.
     */
    MAX_NEW_SYNAPSE_COUNT("maxNewSynapseCount", Integer.class),
    /**
     * Initial permanence of a new synapse
     */
    INITIAL_PERMANENCE("initialPermanence", Double.class, 0.0, 1.0),
    /**
     * If the permanence value for a synapse
     * is greater than this value, it is said
     * to be connected.
     */
    CONNECTED_PERMANENCE("connectedPermanence", Double.class, 0.0, 1.0),
    /**
     * Amount by which permanence of synapses
     * are incremented during learning.
     */
    PERMANENCE_INCREMENT("permanenceIncrement", Double.class, 0.0, 1.0),
    /**
     * Amount by which permanences of synapses
     * are decremented during learning.
     */
    PERMANENCE_DECREMENT("permanenceDecrement", Double.class, 0.0, 1.0),
    TM_VERBOSITY("tmVerbosity", Integer.class, 0, 10),

    /////////// Spatial Pooler Parameters ///////////
    INPUT_DIMENSIONS("inputDimensions", int[].class),
    POTENTIAL_RADIUS("potentialRadius", ColumnRadius.class),
    POTENTIAL_PCT("potentialPct", Double.class), //TODO add range here?
    GLOBAL_INHIBITIONS("globalInhibition", Boolean.class),
    INHIBITION_RADIUS("inhibitionRadius", Integer.class, 0, null),
    LOCAL_AREA_DENSITY("localAreaDensity", Double.class), //TODO add range here?
    NUM_ACTIVE_COLUMNS_PER_INH_AREA("numActiveColumnsPerInhArea", Double.class),//TODO add range here?
    STIMULUS_THRESHOLD("stimulusThreshold", Double.class), //TODO add range here?
    SYN_PERM_INACTIVE_DEC("synPermInactiveDec", Double.class, 0.0, 1.0),
    SYN_PERM_ACTIVE_INC("synPermActiveInc", Double.class, 0.0, 1.0),
    SYN_PERM_CONNECTED("synPermConnected", Double.class, 0.0, 1.0),
    SYN_PERM_DISCONNECTED("synPermDisconnected", Double.class, -1.0, 1.0),
    SYN_PERM_BELOW_STIMULUS_INC("synPermBelowStimulusInc", Double.class, 0.0, 1.0),
    SYN_PERM_TRIM_THRESHOLD("synPermTrimThreshold", Double.class, 0.0, 1.0),
    MIN_PCT_OVERLAP_DUTY_CYCLE("minPctOverlapDutyCycles", Double.class),//TODO add range here?
    MIN_PCT_ACTIVE_DUTY_CYCLE("minPctActiveDutyCycles", Double.class),//TODO add range here?
    DUTY_CYCLE_PERIOD("dutyCyclePeriod", Integer.class),//TODO add range here?
    MAX_BOOST("maxBoost", Double.class), //TODO add range here?
    SP_VERBOSITY("spVerbosity", Integer.class, 0, 10);
    
    public static final Map<String, KEY> fieldMap = new HashMap<>();

    static {
        for (KEY key : KEY.values()) {
            fieldMap.put(key.getFieldName(), key);
        }
    }

    public static KEY key(String fieldName) {
        return fieldMap.get(fieldName);
    }
    
    final String fieldName;
    private final Class<?> fieldType;
    private final Number min;
    private final Number max;

    /**
     * Constructs a new KEY
     *
     * @param fieldName
     * @param fieldType
     */
    private KEY(String fieldName, Class<?> fieldType) {
        this(fieldName, fieldType, null, null);
    }

    /**
     * Constructs a new KEY with range check
     *
     * @param fieldName
     * @param fieldType
     * @param min
     * @param max
     */
    private KEY(String fieldName, Class<?> fieldType, Number min, Number max) {
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.min = min;
        this.max = max;
    }

    public Class<?> getFieldType() {
        return fieldType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Number getMin() {
        return min;
    }

    public Number getMax() {
        return max;
    }

    public boolean checkRange(Number value) {
        if (value == null) {
            throw new IllegalArgumentException("checkRange argument can not be null");
        }
        return (min == null && max == null) || (min != null && max == null && min.doubleValue() <= value.doubleValue()) || (max != null && min == null && value.doubleValue() < value.doubleValue()) || (min != null && min.doubleValue() <= value.doubleValue() && max != null && value.doubleValue() < max.doubleValue());
    }

}
