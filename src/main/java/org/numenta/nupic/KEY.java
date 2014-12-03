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

/**
 * Constant values representing configuration parameters for the
 * {@link TemporalMemory}
 */
public enum KEY implements Serializable {

    COLUMN_DIMENSIONS("columnDimensions", int[].class), CELLS_PER_COLUMN("cellsPerColumn", Integer.class, 1, null), RANDOM("random", Random.class), SEED("seed", Integer.class), ACTIVATION_THRESHOLD("activationThreshold", Integer.class, 0, null), LEARNING_RADIUS("learningRadius", Integer.class, 0, null), MIN_THRESHOLD("minThreshold", Integer.class, 0, null), MAX_NEW_SYNAPSE_COUNT("maxNewSynapseCount", Integer.class), INITIAL_PERMANENCE("initialPermanence", Double.class, 0.0, 1.0), CONNECTED_PERMANENCE("connectedPermanence", Double.class, 0.0, 1.0), PERMANENCE_INCREMENT("permanenceIncrement", Double.class, 0.0, 1.0), PERMANENCE_DECREMENT("permanenceDecrement", Double.class, 0.0, 1.0), TM_VERBOSITY("tmVerbosity", Integer.class, 0, 10), INPUT_DIMENSIONS("inputDimensions", int[].class), POTENTIAL_RADIUS("potentialRadius", Integer.class), POTENTIAL_PCT("potentialPct", Double.class), GLOBAL_INHIBITIONS("globalInhibition", Boolean.class), INHIBITION_RADIUS("inhibitionRadius", Integer.class, 0, null), LOCAL_AREA_DENSITY("localAreaDensity", Double.class), NUM_ACTIVE_COLUMNS_PER_INH_AREA("numActiveColumnsPerInhArea", Double.class), STIMULUS_THRESHOLD("stimulusThreshold", Double.class), SYN_PERM_INACTIVE_DEC("synPermInactiveDec", Double.class, 0.0, 1.0), SYN_PERM_ACTIVE_INC("synPermActiveInc", Double.class, 0.0, 1.0), SYN_PERM_CONNECTED("synPermConnected", Double.class, 0.0, 1.0), SYN_PERM_BELOW_STIMULUS_INC("synPermBelowStimulusInc", Double.class, 0.0, 1.0), SYN_PERM_TRIM_THRESHOLD("synPermTrimThreshold", Double.class, 0.0, 1.0), MIN_PCT_OVERLAP_DUTY_CYCLE("minPctOverlapDutyCycles", Double.class), MIN_PCT_ACTIVE_DUTY_CYCLE("minPctActiveDutyCycles", Double.class), DUTY_CYCLE_PERIOD("dutyCyclePeriod", Integer.class), MAX_BOOST("maxBoost", Double.class), SP_VERBOSITY("spVerbosity", Integer.class, 0, 10);
    
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
