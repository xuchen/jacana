package com.tek271.memoize;

import java.lang.reflect.Method;
import java.util.List;

import com.tek271.memoize.cache.ICache;
import com.tek271.memoize.cache.ICacheFactory;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

class DecoratorInterceptor implements MethodInterceptor {
  private ICacheFactory pCacheFactory;
  private Object objectTobeDecorated;
  
  public DecoratorInterceptor(ICacheFactory cacheFactory, Object objectTobeDecorated) {
    pCacheFactory= cacheFactory;
    this.objectTobeDecorated= objectTobeDecorated;
  }


  public Object intercept(@SuppressWarnings("unused") Object obj, 
                          Method method, Object[] args, 
                          @SuppressWarnings("unused") MethodProxy proxy) throws Throwable {
    if (Utils.isVoidOrNotAnnotatedWithRemember(method)) {
      return method.invoke(objectTobeDecorated, args);
    }
    
    Remember ann= method.getAnnotation(Remember.class);
    // check annotation to find excluded parameters, build a list of key parameters
    List<Object> relevantArgs= Utils.getRelevantArguments(args, ann.excludedParametersIndex());
    
    // access cache
    ICache cache= Utils.getCache(pCacheFactory, method, ann);
    cache.removeExpired();
    if (cache.containsKey(relevantArgs) ) {
      return cache.get(relevantArgs);
    }
    
    Object value = method.invoke(objectTobeDecorated, args);
    cache.put(relevantArgs, value);
    return value;
  }

}
