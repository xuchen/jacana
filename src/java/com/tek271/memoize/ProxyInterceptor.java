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
import java.util.List;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import com.tek271.memoize.cache.ICache;
import com.tek271.memoize.cache.ICacheFactory;

/**
 * Implements the MethodInterceptor from cglib. Intercepts methods which have 
 * <code>Remember</code> annotation and attempts to cache their outputs.
 * <p>Note that this class has a default/package scope and is not visible outside this 
 * package.</p>
 * @author Abdul Habra
 * @version 1.0
 */
class ProxyInterceptor implements MethodInterceptor {
  private ICacheFactory pCacheFactory;
  
  public ProxyInterceptor(ICacheFactory cacheFactory) {
    pCacheFactory= cacheFactory;
  }
  
 /**
 * @param obj the enhanced object
 * @param method intercepted Method
 * @param args argument array; primitive types are wrapped
 * @param proxy used to invoke super (non-intercepted method); may be called as many 
 *        times as needed
 * @return a value compatible with the signature of the proxied method. Methods returning 
 *         void will ignore this value. 
 */  
  public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) 
         throws Throwable {
    // There is no need to check if the method should be memoized or not.
    // That is determined by the RememberCallbackFilter
    
    Remember ann= method.getAnnotation(Remember.class);
    // check annotation to find excluded parameters, build a list of key parameters
    List<Object> relevantArgs= Utils.getRelevantArguments(args, ann.excludedParametersIndex());
    
    // access cache
    ICache cache= Utils.getCache(pCacheFactory, method, ann);
    cache.removeExpired();
    if (cache.containsKey(relevantArgs) ) {
      return cache.get(relevantArgs);
    }
    Object value = proxy.invokeSuper(obj, args);
    cache.put(relevantArgs, value);
    
    return value;
  }

}
