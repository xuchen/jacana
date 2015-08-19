/*
Technology Exponent (Tek271) Memoizer
Copyright (C) 2007  Abdul Habra
www.tek271.com

This file is part of Tek271 Memoizer

Tek271 Memoizer is free software; you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published
by the Free Software Foundation; version 2.

Tek271 Memoizer is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with Tek271 Memoizer; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

You can contact the author at ahabra at yahoo.com
*/

package com.tek271.memoize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.tek271.memoize.cache.TimeUnitEnum;

/**
 * Used for caching methods (memoization). Methods that can utilize this annotation
 * must: <ol>
 * <li>Belong to a class which support a parameter-less constructor. In other words,
 * The class must have a public constructor with no parameters, or not have any
 * declared constructors at all.
 * <li>Method must not be final.
 * <li>Method must not be static.
 * <li>Method must have a return type that is not void. IOW, the method must return something.
 * <li>For the same parameters values, the method must always return the same value. This
 * makes the method follow the mathematical definition of a function. A function's output
 * is always the same for the same inputs.
 * <li>The method should not have other side effects like setting fields or properties
 * of the class.
 * <li>The method's parameters must support correct <code>equals()</code> and 
 * <code>hashCode()</code> methods. 
 * </ol>
 * If the method has parameters which you do not want to use as part of the caching key,
 * you should exclude them with the <code>excludedParametersIndex</code> attribute. For
 * example if a method reads some value from a database, and the method has a 
 * <code>java.sql.Connection</code> paramater, this parameter must be excluded.
 * 
 * @author Abdul Habra
 * @version 1.0
 */
@Documented
@Target( ElementType.METHOD )
@Retention( RetentionPolicy.RUNTIME )
public @interface Remember {
  /** The maximum size of the cache for the annotated method. Default is 128. */
  int maxSize() default 128;
  
  /** 
   * The period of time after which, cached return values of the method will expire.
   * The default is 2 minutes.
   **/
  long timeToLive() default 2;
  
  /** The unit of time for the timeToLive attribute. The default is Minute. */
  TimeUnitEnum timeUnit() default TimeUnitEnum.MINUTE;
  
  /** Method parameters that should NOT be used as part of the cache's key.
   * The java reflection API does not provide access to method's parameters
   * names, hence we will use index. The index of the first parameter is zero.
   *  */
  int[] excludedParametersIndex() default {};

}
