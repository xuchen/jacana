/*
Technology Exponent (Tek271) Memoizer
Copyright (C) 2009  Abdul Habra
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

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.CallbackFilter;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.NoOp;

/**
 * Determine which methods on a class should be memoized.
 * This class provides an enhancement over the original implementation which intercepted
 * all methods, then determined if they should be memoized.
 * This enhancement, and code was provided by Christian Semrau. Abdul Habra did
 * some refactoring. 
 * @author Christian Semrau. 
 * @version 1.1
 * @since 1.1
 */
class RememberCallbackFilter implements CallbackFilter {
  public static final CallbackFilter INSTANCE = new RememberCallbackFilter();
  
  private static final int INDEX_OF_NOOP= 0;
  private static final int INDEX_OF_INTECEPTOR= 1;
  
  public int accept(Method method) {
    if (Utils.isVoidOrNotAnnotatedWithRemember(method)) return INDEX_OF_NOOP;
    return INDEX_OF_INTECEPTOR;
  }

  
  public static Callback[] createCallbacks(MethodInterceptor interceptor) {
    Callback[] callbacks= new Callback[2];
    
    callbacks[RememberCallbackFilter.INDEX_OF_NOOP]= NoOp.INSTANCE;
    callbacks[RememberCallbackFilter.INDEX_OF_INTECEPTOR]= interceptor;
    return callbacks;
  }
}
