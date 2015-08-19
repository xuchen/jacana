/*
Technology Exponent (Tek271) Memoizer (TECUJ)
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

package com.tek271.memoize.cache;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * An implementation of the ICacheFactory interface. This is used as the default 
 * cache factory when no cache factory is provided by the caller to 
 * <code>RememberFactory.createProxy()</code>.
 * <p>Note that even though this class has a public scope, you should not need to deal
 * with it directly.</p>
 * @author Abdul Habra
 * @version 1.0
 */
public class DefaultCacheFactory implements ICacheFactory {
  private static ICacheFactory pSingleton= new DefaultCacheFactory();
  private Map<String, ICache> pAllCaches= new HashMap<String, ICache>(); 
  
  
  public static ICacheFactory getInstance() {
    return pSingleton;
  }

  private DefaultCacheFactory() {
    // prevent calls to constructor
  }
  
  public ICache getCache(final String cacheName, final int maxSize, 
                         final long timeToLive, final TimeUnitEnum timeUnit) {
    if (pAllCaches.containsKey(cacheName)) {
      return pAllCaches.get(cacheName);
    }
    
    synchronized (this) {
      ICache cache= new DefaultCache(maxSize, timeToLive, timeUnit);
      pAllCaches.put(cacheName, cache);
      return cache;
    }
  }
  
  public ICache getCache(final String cacheName) {
    return getCache(cacheName, IConstants.MAX_SIZE, IConstants.TTL, IConstants.TIME_UNIT);
  }

  public synchronized void clear() {
    pAllCaches.clear();
  }

  public synchronized void remove(final String cacheName) {
    pAllCaches.remove(cacheName);
  }
  
  public synchronized void removeExpired() {
    Set<String> keys= pAllCaches.keySet();
    for (Iterator<String> i=keys.iterator(); i.hasNext();) {
      ICache cache= pAllCaches.get(i);
      cache.removeExpired();
      if (cache.size()==0) {
        i.remove();
      }
      
    }
  }
}
