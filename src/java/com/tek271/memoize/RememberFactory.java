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

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;

import com.tek271.memoize.cache.DefaultCacheFactory;
import com.tek271.memoize.cache.ICacheFactory;

/**
   * Create a caching (memoizing) proxy (or decorator) for an object that contains methods 
   * with <code>Remember</code> annotation. 
   * Calling these methods will cause them to be cached.
   * <p>There are two approaches to utilize this library. One is to create a proxy for a 
   * given class, and the other is to decorate a given object. The following example shows the
   * first approach:</p>
   * <ol>
   * <li>Create a class that has some methods you wish to cache. The class also <b>must</b> 
   * support a <b>parameter-less constructor</b>.</li>
   * <li>Use the <code>&#64;Remember</code> annotation on the methods you like to cache.
   * For example:<pre>
import com.tek271.memoize.Remember;

public class ExpensiveCalcs {
  &#64;Remember
  public String slowMethod(String param1) {
    System.out.println("inside slowMethod(" + param1 + ")");
    return param1 + param1;
  }

}</pre>
   * Notice how the method does not have any caching related code (except for the 
   * annotation)
   * </li>
   * <li>To create an object of ExpensiveCalcs that utilizes method caching:
<pre>
import com.tek271.memoize.RememberFactory;
...

ExpensiveCalcs ec= RememberFactory.createProxy(ExpensiveCalcs.class);
</pre></li>

   *<li>Call the <code>slowMethod()</code> several times with the same parameters values:
<pre>
String s1= ec.slowMethod("hello");
String s2= ec.slowMethod("world");
String s3= ec.slowMethod("hello");
String s4= ec.slowMethod("hello");
assert s1.equals(s3);
assert s1.equals(s4);
</pre>
The console will show:<pre>
inside slowMethod(hello)
inside slowMethod(world)
</pre>
Notice how the third and fourth invocations of slowMethod did not cause any console output.
   * </li>
   * </ol>
   * <hr width="30%" align="left">
   * The second approach is to decorate a given object. This approach makes it easier
   * to integrate memoization with other frameworks like <i>Spring</i>. 
   * Next is an example:
<pre>
ExpensiveCalcs ec= new ExpensiveCalcs();  // create ec any way you like, e.g. through injection
ExpensiveCalcs decorated= RememberFactory.decorate(ec);
String s1= decorated.slowMethod("hello"); // will memoize slowMethod()
</pre>
 * <p></p>
 * @author Abdul Habra
 * @version 1.1
 */
public class RememberFactory {

  /**
   * Create a caching (memoizing) proxy for an object that contains methods 
   * with <code>Remember</code> annotation. 
   * Calling these methods will cause them to be cached.
   * @param <T> The type of the object
   * @param targetClass The class to create a proxy for. The class must provide a
   * parameter-less constructor.
   * @param cacheFactory Factory for creating cache objects. If it is null, a default
   * (simple) factory will be used. Programmers can use other cache libraries by
   * implementing the ICacheFactory and ICache interfaces.
   * @return An object of the type <code>targetClass</code> where methods that are
   * annotated by <code>Remember</code> will be cached.
   * @since 1.0
   */
  @SuppressWarnings("unchecked")
  public static <T> T createProxy(Class<T> targetClass, ICacheFactory cacheFactory) {
    cacheFactory= getCacheFactory(cacheFactory);

    MethodInterceptor interceptor= new ProxyInterceptor(cacheFactory);
    // Control which methods should go thru the interceptor
    Callback[] callbacks= RememberCallbackFilter.createCallbacks(interceptor); 
    
    Enhancer enhancer = new Enhancer();  // cglib class
    enhancer.setSuperclass(targetClass);
    enhancer.setCallbacks(callbacks );
    enhancer.setCallbackFilter(RememberCallbackFilter.INSTANCE);    
    
    return (T) enhancer.create();
  }
  
  /**
   * Create a proxy for an object that contains methods with <code>Remember</code> 
   * annotation. Calling these methods will cause them to be cached. This method
   * will use the default cache factory provided by this implementation.
   * @param <T> The type of the object
   * @param targetClass The class to create a proxy for. The class must provide a
   * parameter-less constructor.
   * @return An object of the type <code>targetClass</code> where methods that are
   * annotated by <code>Remember</code> will be cached.
   * @since 1.0
   */  
  @SuppressWarnings(value={"unchecked"})
  public static <T> T createProxy(Class<T> targetClass) {
    return createProxy(targetClass, null);
  }

  /**
   * Memoize methods of an existing object. Allows integrating with other frameworks like
   * <i>Spring</i>.
   * @param <T> The type of the object
   * @param objectTobeDecorated The object should have at least one Remember annotation
   * @param cacheFactory Factory for creating cache objects. If it is null, a default
   * (simple) factory will be used. Programmers can use other cache libraries by
   * implementing the ICacheFactory and ICache interfaces.
   * @return an object of type T which decorates <code>objectTobeDecorated</code>. Method
   * invocations will be routed to the objectTobeDecorated after checking for memoization. 
   * @since 1.1
   */
  @SuppressWarnings("unchecked")
  public static <T> T decorate(T objectTobeDecorated, ICacheFactory cacheFactory) {
    if (objectTobeDecorated==null) {
      throw new NullPointerException("Memoizer cannot decorate a null object");
    }
    cacheFactory= getCacheFactory(cacheFactory);
    
    MethodInterceptor interceptor= new DecoratorInterceptor(cacheFactory, objectTobeDecorated);
    Enhancer enhancer = new Enhancer();  // cglib class
    enhancer.setSuperclass(objectTobeDecorated.getClass());
    enhancer.setCallback(interceptor);
    return (T) enhancer.create();
  }
  
  private static ICacheFactory getCacheFactory(ICacheFactory cacheFactory) {
    if (cacheFactory!=null) return cacheFactory;
    return DefaultCacheFactory.getInstance();
  }
  
  /**
   * Memoize methods of an existing object. Allows integrating with other frameworks like
   * <i>Spring</i>.
   * @param <T> The type of the object
   * @param objectTobeDecorated The object should have at least one Remember annotation
   * @return an object of type T which decorates <code>objectTobeDecorated</code>. Method
   * invocations will be routed to the objectTobeDecorated after checking for memoization. 
   * @since 1.1
   */
  public static <T> T decorate(T objectTobeDecorated) {
    return decorate(objectTobeDecorated, null);
  }
  
  /**
   * Clear the given cache. When cacheFactory is null, then the DefaultCacheFactory
   * is cleared. Useful for unit testing.
   * @param cacheFactory
   * @since 1.1
   */
  public static void clearCache(ICacheFactory cacheFactory) {
    if (cacheFactory==null) {
      cacheFactory= DefaultCacheFactory.getInstance();
    }
    cacheFactory.clear();
  }
  
  /**
   * Clear the cache in the DefaultCacheFactory.
   * Useful for unit testing.
   * @since 1.1
   */
  public static void clearCache() {
    clearCache(null);
  }
  
}
