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

package org.numenta.nupic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.Column;
import org.numenta.nupic.research.ComputeCycle;
import org.numenta.nupic.research.SpatialPooler;
import org.numenta.nupic.research.TemporalMemory;
import org.numenta.nupic.util.BeanUtil;
import org.numenta.nupic.util.MersenneTwister;

/**
 * Specifies parameters to be used as a configuration for a given {@link TemporalMemory}
 * or {@link SpatialPooler}
 *
 * @author David Ray
 * @author Kirill Solovyev
 * @see SpatialPooler
 * @see TemporalMemory
 * @see CLA
 * @see ComputeCycle
 */
public class Build<T> implements Serializable {    

    /**
     * Save guard decorator around params map
     */
    private static class ParametersMap extends EnumMap<KEY, Object> {
        /** Default serialvers */
	private static final long serialVersionUID = 1L;

	ParametersMap() {
            super(KEY.class);
        }

        @Override public Object put(KEY key, Object value) {
            if (value != null) {
                if (!key.getFieldType().isInstance(value)) {
                    throw new IllegalArgumentException(
                            "Can not set Parameters Property '" + key.getFieldName() + "' because of type mismatch. The required type is " + key.getFieldType());
                }
                if (value instanceof Number && !key.checkRange((Number)value)) {
                    throw new IllegalArgumentException(
                            "Can not set Parameters Property '" + key.getFieldName() + "' because of value '" + value + "' not in range. Range[" + key.getMin() + "-" + key.getMax() + "]");
                }
            }
            return super.put(key, value);
        }
    }

    /**
     * Map of parameters to their values
     */
    public final Map<KEY, Object> paramMap = new ConcurrentHashMap();
    //TODO apply from container to parameters






    /** from: http://stackoverflow.com/a/2434094 */
    protected T newInstance() {
        ParameterizedType superClass = (ParameterizedType) getClass().getGenericSuperclass();
        Class<T> type = (Class<T>) superClass.getActualTypeArguments()[0];
        try {
            return type.newInstance();
        }
        catch (Exception e) {
            // Oops, no default constructor
            throw new RuntimeException(e);
        }
    }
    
    final static BeanUtil beanUtil = BeanUtil.getInstance();
    
    public T apply(T t) {        
        Set<KEY> presentKeys = paramMap.keySet();
        synchronized (paramMap) {            
            for (KEY key : presentKeys) {
                beanUtil.setSimpleProperty(t, key.fieldName, get(key));
            }
        }
        return t;
    }

    /** create an empty Build configuration */
    public Build() {
        
    }
    
    public Build(Map<KEY,Object> map) {
        for (KEY key : map.keySet()) {
            set(key, map.get(key));
        }
    }
    
    /**
     * Sets the fields specified by this {@code Parameters} on the specified
     * {@link CLA} object.
     *
     * @param cn
     */
    public T build() {
        T t = newInstance();
        return apply(t);
    }


    public Object remove(KEY key) {
        return paramMap.remove(key);
    }
    
    /**
     * Set parameter by Key{@link KEY}
     *
     * @param key
     * @param value
     */
    public Build<T> set(KEY key, Object value) {
        if (value==null)
            remove(key);
        else
            paramMap.put(key, value);
        return this;
    }

    /**
     * Get parameter by Key{@link KEY}
     *
     * @param key
     * @return
     */
    public Object get(KEY key) {
        return paramMap.get(key);
    }

    /**
     * @param key
     * IMPORTANT! This is a nuclear option, should be used with care. Will knockout key's parameter from map and compromise integrity
     */
    public void clearParameter(KEY key) {
        paramMap.remove(key);
    }

    /**
     * Convenience method to log difference this {@code Parameters} and specified
     * {@link CLA} object.
     *
     * @param cn
     * @return true if find it different
     */
    public boolean logDiff(Object cn) {
        if (cn == null) {
            throw new IllegalArgumentException("cn Object is required and can not be null");
        }
        boolean result = false;
        BeanUtil beanUtil = BeanUtil.getInstance();
        BeanUtil.PropertyInfo[] properties = beanUtil.getPropertiesInfoForBean(cn.getClass());
        for (int i = 0; i < properties.length; i++) {
            BeanUtil.PropertyInfo property = properties[i];
            String fieldName = property.getName();
            KEY propKey = KEY.key(property.getName());
            if (propKey != null) {
                Object paramValue = this.get(propKey);
                Object cnValue = beanUtil.getSimpleProperty(cn, fieldName);
                if ((paramValue != null && !paramValue.equals(cnValue)) || (paramValue == null && cnValue != null)) {
                    result = true;
                    System.out.println(
                            "Property:" + fieldName + " is different - CN:" + cnValue + " | PARAM:" + paramValue);
                }
            }
        }
        return result;
    }

    //TODO I'm not sure we need maintain implicit setters below. Kinda contradict unified access with KEYs

    /**
     * Returns the seeded random number generator.
     *
     * @param r the generator to use.
     */
    public void setRandom(Random r) {
        paramMap.put(KEY.RANDOM, r);
    }

    /**
     * Sets the number of {@link Column}.
     *
     * @param columnDimensions
     */
    public void setColumnDimensions(int[] columnDimensions) {
        paramMap.put(KEY.COLUMN_DIMENSIONS, columnDimensions);
    }

    /**
     * Sets the number of {@link Cell}s per {@link Column}
     *
     * @param cellsPerColumn
     */
    public void setCellsPerColumn(int cellsPerColumn) {
        paramMap.put(KEY.CELLS_PER_COLUMN, cellsPerColumn);
    }

    /**
     * Sets the activation threshold.
     * <p/>
     * If the number of active connected synapses on a segment
     * is at least this threshold, the segment is said to be active.
     *
     * @param activationThreshold
     */
    public void setActivationThreshold(double activationThreshold) {
        paramMap.put(KEY.ACTIVATION_THRESHOLD, activationThreshold);
    }

    /**
     * Radius around cell from which it can
     * sample to form distal dendrite connections.
     *
     * @param learningRadius
     */
    public void setLearningRadius(int learningRadius) {
        paramMap.put(KEY.LEARNING_RADIUS, learningRadius);
    }

    /**
     * If the number of synapses active on a segment is at least this
     * threshold, it is selected as the best matching
     * cell in a bursting column.
     *
     * @param minThreshold
     */
    public void setMinThreshold(double minThreshold) {
        paramMap.put(KEY.MIN_THRESHOLD, minThreshold);
    }

    /**
     * The maximum number of synapses added to a segment during learning.
     *
     * @param maxNewSynapseCount
     */
    public void setMaxNewSynapseCount(int maxNewSynapseCount) {
        paramMap.put(KEY.MAX_NEW_SYNAPSE_COUNT, maxNewSynapseCount);
    }

    /**
     * Seed for random number generator
     *
     * @param seed
     */
    public void setSeed(int seed) {
        paramMap.put(KEY.SEED, seed);
    }

    /**
     * Initial permanence of a new synapse
     *
     * @param
     */
    public void setInitialPermanence(double initialPermanence) {
        paramMap.put(KEY.INITIAL_PERMANENCE, initialPermanence);
    }

    /**
     * If the permanence value for a synapse
     * is greater than this value, it is said
     * to be connected.
     *
     * @param connectedPermanence
     */
    public void setConnectedPermanence(double connectedPermanence) {
        paramMap.put(KEY.CONNECTED_PERMANENCE, connectedPermanence);
    }

    /**
     * Amount by which permanences of synapses
     * are incremented during learning.
     *
     * @param permanenceIncrement
     */
    public void setPermanenceIncrement(double permanenceIncrement) {
        paramMap.put(KEY.PERMANENCE_INCREMENT, permanenceIncrement);
    }

    /**
     * Amount by which permanences of synapses
     * are decremented during learning.
     *
     * @param permanenceDecrement
     */
    public void setPermanenceDecrement(double permanenceDecrement) {
        paramMap.put(KEY.PERMANENCE_DECREMENT, permanenceDecrement);
    }

    ////////////////////////////// SPACIAL POOLER PARAMS //////////////////////////////////

    /**
     * A list representing the dimensions of the input
     * vector. Format is [height, width, depth, ...], where
     * each value represents the size of the dimension. For a
     * topology of one dimension with 100 inputs use 100, or
     * [100]. For a two dimensional topology of 10x5 use
     * [10,5].
     *
     * @param inputDimensions
     */
    public void setInputDimensions(int[] inputDimensions) {
        paramMap.put(KEY.INPUT_DIMENSIONS, inputDimensions);
    }

    /**
     * This parameter determines the extent of the input
     * that each column can potentially be connected to.
     * This can be thought of as the input bits that
     * are visible to each column, or a 'receptiveField' of
     * the field of vision. A large enough value will result
     * in 'global coverage', meaning that each column
     * can potentially be connected to every input bit. This
     * parameter defines a square (or hyper square) area: a
     * column will have a max square potential pool with
     * sides of length 2 * potentialRadius + 1.
     *
     * @param potentialRadius
     */
    public void setPotentialRadius(int potentialRadius) {
        paramMap.put(KEY.POTENTIAL_RADIUS, potentialRadius);
    }

    /**
     * The inhibition radius determines the size of a column's local
     * neighborhood. of a column. A cortical column must overcome the overlap
     * score of columns in his neighborhood in order to become actives. This
     * radius is updated every learning round. It grows and shrinks with the
     * average number of connected synapses per column.
     *
     * @param inhibitionRadius the local group size
     */
    public void setInhibitionRadius(int inhibitionRadius) {
        paramMap.put(KEY.INHIBITION_RADIUS, inhibitionRadius);
    }

    /**
     * The percent of the inputs, within a column's
     * potential radius, that a column can be connected to.
     * If set to 1, the column will be connected to every
     * input within its potential radius. This parameter is
     * used to give each column a unique potential pool when
     * a large potentialRadius causes overlap between the
     * columns. At initialization time we choose
     * ((2*potentialRadius + 1)^(# inputDimensions) *
     * potentialPct) input bits to comprise the column's
     * potential pool.
     *
     * @param potentialPct
     */
    public void setPotentialPct(double potentialPct) {
        paramMap.put(KEY.POTENTIAL_PCT, potentialPct);
    }

    /**
     * If true, then during inhibition phase the winning
     * columns are selected as the most active columns from
     * the region as a whole. Otherwise, the winning columns
     * are selected with respect to their local
     * neighborhoods. Using global inhibition boosts
     * performance x60.
     *
     * @param globalInhibition
     */
    public void setGlobalInhibition(boolean globalInhibition) {
        paramMap.put(KEY.GLOBAL_INHIBITIONS, globalInhibition);
    }

    /**
     * The desired density of active columns within a local
     * inhibition area (the size of which is set by the
     * internally calculated inhibitionRadius, which is in
     * turn determined from the average size of the
     * connected potential pools of all columns). The
     * inhibition logic will insure that at most N columns
     * remain ON within a local inhibition area, where N =
     * localAreaDensity * (total number of columns in
     * inhibition area).
     *
     * @param localAreaDensity
     */
    public void setLocalAreaDensity(double localAreaDensity) {
        paramMap.put(KEY.LOCAL_AREA_DENSITY, localAreaDensity);
    }

    /**
     * An alternate way to control the density of the active
     * columns. If numActivePerInhArea is specified then
     * localAreaDensity must be less than 0, and vice versa.
     * When using numActivePerInhArea, the inhibition logic
     * will insure that at most 'numActivePerInhArea'
     * columns remain ON within a local inhibition area (the
     * size of which is set by the internally calculated
     * inhibitionRadius, which is in turn determined from
     * the average size of the connected receptive fields of
     * all columns). When using this method, as columns
     * learn and grow their effective receptive fields, the
     * inhibitionRadius will grow, and hence the net density
     * of the active columns will *decrease*. This is in
     * contrast to the localAreaDensity method, which keeps
     * the density of active columns the same regardless of
     * the size of their receptive fields.
     *
     * @param numActiveColumnsPerInhArea
     */
    public void setNumActiveColumnsPerInhArea(double numActiveColumnsPerInhArea) {
        paramMap.put(KEY.NUM_ACTIVE_COLUMNS_PER_INH_AREA, numActiveColumnsPerInhArea);
    }

    /**
     * This is a number specifying the minimum number of
     * synapses that must be on in order for a columns to
     * turn ON. The purpose of this is to prevent noise
     * input from activating columns. Specified as a percent
     * of a fully grown synapse.
     *
     * @param stimulusThreshold
     */
    public void setStimulusThreshold(double stimulusThreshold) {
        paramMap.put(KEY.STIMULUS_THRESHOLD, stimulusThreshold);
    }

    /**
     * The amount by which an inactive synapse is
     * decremented in each round. Specified as a percent of
     * a fully grown synapse.
     *
     * @param synPermInactiveDec
     */
    public void setSynPermInactiveDec(double synPermInactiveDec) {
        paramMap.put(KEY.SYN_PERM_INACTIVE_DEC, synPermInactiveDec);
    }

    /**
     * The amount by which an active synapse is incremented
     * in each round. Specified as a percent of a
     * fully grown synapse.
     *
     * @param synPermActiveInc
     */
    public void setSynPermActiveInc(double synPermActiveInc) {
        paramMap.put(KEY.SYN_PERM_ACTIVE_INC, synPermActiveInc);
    }

    /**
     * The default connected threshold. Any synapse whose
     * permanence value is above the connected threshold is
     * a "connected synapse", meaning it can contribute to
     * the cell's firing.
     *
     * @param synPermConnected
     */
    public void setSynPermConnected(double synPermConnected) {
        paramMap.put(KEY.SYN_PERM_CONNECTED, synPermConnected);
    }

    /**
     * Sets the increment of synapse permanences below the stimulus
     * threshold
     *
     * @param synPermBelowStimulusInc
     */
    public void setSynPermBelowStimulusInc(double synPermBelowStimulusInc) {
        paramMap.put(KEY.SYN_PERM_BELOW_STIMULUS_INC, synPermBelowStimulusInc);
    }

    /**
     * @param synPermTrimThreshold
     */
    public void setSynPermTrimThreshold(double synPermTrimThreshold) {
        paramMap.put(KEY.SYN_PERM_TRIM_THRESHOLD, synPermTrimThreshold);
    }

    /**
     * A number between 0 and 1.0, used to set a floor on
     * how often a column should have at least
     * stimulusThreshold active inputs. Periodically, each
     * column looks at the overlap duty cycle of
     * all other columns within its inhibition radius and
     * sets its own internal minimal acceptable duty cycle
     * to: minPctDutyCycleBeforeInh * max(other columns'
     * duty cycles).
     * On each iteration, any column whose overlap duty
     * cycle falls below this computed value will  get
     * all of its permanence values boosted up by
     * synPermActiveInc. Raising all permanences in response
     * to a sub-par duty cycle before  inhibition allows a
     * cell to search for new inputs when either its
     * previously learned inputs are no longer ever active,
     * or when the vast majority of them have been
     * "hijacked" by other columns.
     *
     * @param minPctOverlapDutyCycles
     */
    public void setMinPctOverlapDutyCycle(double minPctOverlapDutyCycles) {
        paramMap.put(KEY.MIN_PCT_OVERLAP_DUTY_CYCLE, minPctOverlapDutyCycles);
    }

    /**
     * A number between 0 and 1.0, used to set a floor on
     * how often a column should be activate.
     * Periodically, each column looks at the activity duty
     * cycle of all other columns within its inhibition
     * radius and sets its own internal minimal acceptable
     * duty cycle to:
     * minPctDutyCycleAfterInh *
     * max(other columns' duty cycles).
     * On each iteration, any column whose duty cycle after
     * inhibition falls below this computed value will get
     * its internal boost factor increased.
     *
     * @param minPctActiveDutyCycles
     */
    public void setMinPctActiveDutyCycle(double minPctActiveDutyCycles) {
        paramMap.put(KEY.MIN_PCT_ACTIVE_DUTY_CYCLE, minPctActiveDutyCycles);
    }

    /**
     * The period used to calculate duty cycles. Higher
     * values make it take longer to respond to changes in
     * boost or synPerConnectedCell. Shorter values make it
     * more unstable and likely to oscillate.
     *
     * @param dutyCyclePeriod
     */
    public void setDutyCyclePeriod(int dutyCyclePeriod) {
        paramMap.put(KEY.DUTY_CYCLE_PERIOD, dutyCyclePeriod);
    }

    /**
     * The maximum overlap boost factor. Each column's
     * overlap gets multiplied by a boost factor
     * before it gets considered for inhibition.
     * The actual boost factor for a column is number
     * between 1.0 and maxBoost. A boost factor of 1.0 is
     * used if the duty cycle is >= minOverlapDutyCycle,
     * maxBoost is used if the duty cycle is 0, and any duty
     * cycle in between is linearly extrapolated from these
     * 2 end points.
     *
     * @param maxBoost
     */
    public void setMaxBoost(double maxBoost) {
        paramMap.put(KEY.MAX_BOOST, maxBoost);
    }

    /**
     * spVerbosity level: 0, 1, 2, or 3
     *
     * @param spVerbosity
     */
    public void setSpVerbosity(int spVerbosity) {
        paramMap.put(KEY.SP_VERBOSITY, spVerbosity);
    }

    final static ObjectMapper mapper = new ObjectMapper();
    
    @Override
    public String toString() {
	
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException ex) {
            return ex.toString();
        }
    }
    

    
    /**
     * {@inheritDoc}
     */
    public String toString2() {
        StringBuilder sb = new StringBuilder();
        sb.append("{ Spatial\n")
            .append("\t").append("inputDimensions :  ").append(get(KEY.INPUT_DIMENSIONS)).append("\n")
            .append("\t").append("potentialRadius :  ").append(get(KEY.POTENTIAL_RADIUS)).append("\n")
            .append("\t").append("potentialPct :  ").append(get(KEY.POTENTIAL_PCT)).append("\n")
            .append("\t").append("globalInhibition :  ").append(get(KEY.GLOBAL_INHIBITIONS)).append("\n")
            .append("\t").append("inhibitionRadius :  ").append(get(KEY.INHIBITION_RADIUS)).append("\n")
            .append("\t").append("localAreaDensity :  ").append(get(KEY.LOCAL_AREA_DENSITY)).append("\n")
            .append("\t").append("numActiveColumnsPerInhArea :  ").append(get(KEY.NUM_ACTIVE_COLUMNS_PER_INH_AREA)).append("\n")
            .append("\t").append("stimulusThreshold :  ").append(get(KEY.STIMULUS_THRESHOLD)).append("\n")
            .append("\t").append("synPermInactiveDec :  ").append(get(KEY.SYN_PERM_INACTIVE_DEC)).append("\n")
            .append("\t").append("synPermActiveInc :  ").append(get(KEY.SYN_PERM_ACTIVE_INC)).append("\n")
            .append("\t").append("synPermConnected :  ").append(get(KEY.SYN_PERM_CONNECTED)).append("\n")
            .append("\t").append("synPermBelowStimulusInc :  ").append(get(KEY.SYN_PERM_BELOW_STIMULUS_INC)).append("\n")
            .append("\t").append("minPctOverlapDutyCycles :  ").append(get(KEY.MIN_PCT_OVERLAP_DUTY_CYCLE)).append("\n")
            .append("\t").append("minPctActiveDutyCycles :  ").append(get(KEY.MIN_PCT_ACTIVE_DUTY_CYCLE)).append("\n")
            .append("\t").append("dutyCyclePeriod :  ").append(get(KEY.DUTY_CYCLE_PERIOD)).append("\n")
            .append("\t").append("maxBoost :  ").append(get(KEY.MAX_BOOST)).append("\n")
            .append("\t").append("spVerbosity :  ").append(get(KEY.SP_VERBOSITY)).append("\n")
            .append("}\n\n")

            .append("{ Temporal\n")
            .append("\t").append("activationThreshold :  ").append(get(KEY.ACTIVATION_THRESHOLD)).append("\n")
            .append("\t").append("cellsPerColumn :  ").append(get(KEY.CELLS_PER_COLUMN)).append("\n")
            .append("\t").append("columnDimensions :  ").append(get(KEY.COLUMN_DIMENSIONS)).append("\n")
            .append("\t").append("connectedPermanence :  ").append(get(KEY.CONNECTED_PERMANENCE)).append("\n")
            .append("\t").append("initialPermanence :  ").append(get(KEY.INITIAL_PERMANENCE)).append("\n")
            .append("\t").append("maxNewSynapseCount :  ").append(get(KEY.MAX_NEW_SYNAPSE_COUNT)).append("\n")
            .append("\t").append("minThreshold :  ").append(get(KEY.MIN_THRESHOLD)).append("\n")
            .append("\t").append("permanenceIncrement :  ").append(get(KEY.PERMANENCE_INCREMENT)).append("\n")
            .append("\t").append("permanenceDecrement :  ").append(get(KEY.PERMANENCE_DECREMENT)).append("\n")
            .append("}\n\n");

        return sb.toString();
    }

}
