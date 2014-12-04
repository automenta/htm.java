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
package org.numenta.nupic.util;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Testing to see if this is more meaningful than a generic native java data
 * structure for encoder work.
 *
 * Extends ArrayList to cache the hash code since it is meant to be immutable
 * 
 * @author metaware
 *
 */
public class Tuple<T> extends ArrayList<T> implements ITuple<T> {

    private final int hash;

    public Tuple(final T... elements) {
        super(elements.length);
        
        //TODO can use a direct array list to system array copy the elements
        for (T t : elements)
            super.add(t);
        
        this.hash = super.hashCode();
    }
   
    @Override
    public int hashCode() {
        return hash;
    }    
    
    @Override public T the(int index) {
        return get(index);
    }    

    //TODO add traps for other mutable methods
    @Override
    public boolean add(T e) {
        throw new RuntimeException(this + " immutable");
    }

    @Override
    public void clear() {
        throw new RuntimeException(this + " immutable");
    }
    
    
    @Override public T set(int index, T element) {
        throw new RuntimeException(this + " immutable");
    }    
 
    

//    final private T[] container;
//
//
//    public Tuple(T[] objects, int size) {
//        //for creating a tuple with different size array than input
//        throw new RuntimeException("Not impl yet");
//    }
//    
//    public Tuple(T... objects) {        
//        container = objects;
//    }
//
//    @Override
//    public T the(final int index) {
//        return container[index];
//    }
//
//    @Override
//    public String toString() {
//        return Arrays.toString(container);
//        /*
//         StringBuilder sb = new StringBuilder();
//         for(int i = 0;i < container.length;i++) {
//         try {
//         new Double((double) container[i]);
//         sb.append(container[i]);
//         }catch(Exception e) { sb.append("'").append(container[i]).append("'");}
//         sb.append(":");
//         }
//         sb.setLength(sb.length() - 1);
//		
//         return sb.toString();
//         */
//    }
//
//    @Override
//    public int hashCode() {
//        final int prime = 31;
//        int result = 1;
//        result = prime * result + Arrays.hashCode(container);
//        return result;
//    }
//
//    @Override
//    public boolean equals(Object obj) {
//        if (this == obj) {
//            return true;
//        }
//        if (obj == null) {
//            return false;
//        }
//        if (getClass() != obj.getClass()) {
//            return false;
//        }
//        Tuple other = (Tuple) obj;
//        return Arrays.equals(container, other.container);
//    }
}
