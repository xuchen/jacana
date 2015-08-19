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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.tek271.memoize.cache.ICache;
import com.tek271.memoize.cache.ICacheFactory;

/**
 * Some utility static methods.
 * <p>Note that this class has a default/package scope and is not visible outside this 
 * package.</p>
 * @author Abdul Habra
 * @version 1.1
 */
class Utils {
  
  /** Check if the method is void, or if it does not have Remember annotation */
  public static boolean isVoidOrNotAnnotatedWithRemember(Method method) {
    // if method is void then no memoize
    if (method.getReturnType() == Void.TYPE) return true;
    
    // if method has no Remember annotation then no memoize
    Remember ann= method.getAnnotation(Remember.class);
    if (ann==null) return true;
    
    return false;
  }
  
  /** There is a cache for each memoized method ! */
  public static ICache getCache(ICacheFactory cacheFactory, Method method, Remember remember) {
    String methodDesc= method.toGenericString();
    return cacheFactory.getCache(methodDesc, remember.maxSize(), 
                                 remember.timeToLive(), remember.timeUnit());
  }
  
  /** Get the arguments that will be used as part of the key to this method's cache */  
  public static List<Object> getRelevantArguments(final Object[] args,
                                                  final int[] excludedParameters) {
    if (UtilsArrays.isEmptyArray(args)) return new ArrayList<Object>();
    if (UtilsArrays.isEmptyArray(excludedParameters)) return Arrays.asList(args);
    
    List<Object> relevantArgs= new ArrayList<Object>();
    for (int i = 0, n = args.length; i < n; i++) {
      if (! UtilsArrays.isContain(excludedParameters, i)) relevantArgs.add(args[i]);
    }
    return relevantArgs;
  }
  
  
}
