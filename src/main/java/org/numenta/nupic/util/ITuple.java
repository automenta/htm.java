/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.numenta.nupic.util;

/**
 *
 * @author me
 */
public interface ITuple<T> {

    /** the getter, but use an alternate name to allow implementations to extend classes that already include a get method, but with a different type */
    T the(final int index);
    
}
