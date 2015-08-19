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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An implementation of the ICache interface. This is used as the default cache when no
 * cache factory is provided by the caller to <code>RememberFactory.createProxy()</code>
 * <p>Note that this class has a default/package scope and is not visible outside this 
 * package.</p>
 * @author Abdul Habra
 * @version 1.0
 */
class DefaultCache extends LinkedHashMap<Object, Object> implements ICache {
  private static final long serialVersionUID = 1L;
  
  private int pMaxSize=128;
  private long pTimeToLive= 2;
  private TimeUnitEnum pTimeUnit= TimeUnitEnum.MINUTE;
  private long pTimeToLiveMillis= 2 * 60 * 1000;
  private Map<Object, Long> pTimeStamps= new LinkedHashMap<Object, Long>();
  
  public DefaultCache() {
    super(IConstants.INITIAL_CAPACITY, IConstants.LOAD_FACTOR, IConstants.LRU_ORDER);
  }
  
  public DefaultCache(int maxSize, long timeToLive, TimeUnitEnum timeUnit) {
    super(IConstants.INITIAL_CAPACITY, IConstants.LOAD_FACTOR, IConstants.LRU_ORDER);
    pMaxSize= maxSize;
    pTimeToLive= timeToLive;
    pTimeUnit= timeUnit;
    pTimeToLiveMillis= timeToLive * timeUnit.getMilliSeconds();
  }
  
  public int getMaxSize() {
    return pMaxSize;
  }
  
  public long getTimeToLive() {
    return pTimeToLive;
  }

  public TimeUnitEnum getTimeToLiveUnit() {
    return pTimeUnit;
  }

  private boolean isExpired(final long timeStamp) {
    return System.currentTimeMillis() - timeStamp > pTimeToLiveMillis;
  }
  
  private boolean isExpired(final Object key) {
    Long ts= pTimeStamps.get(key);
    if (ts==null) return false;
    return isExpired(ts.longValue());
  }
  
  public synchronized void removeExpired() {
    for(Iterator<Object> i= pTimeStamps.keySet().iterator(); i.hasNext(); ) {
      Object key= i.next();
      if (! isExpired(key)) break;
      super.remove(key);
      i.remove();
    }
  }
  
  private void putInTimeStamp(Object key) {
    if (pTimeStamps.containsKey(key)) {
      pTimeStamps.remove(key);
    }
    pTimeStamps.put(key, System.currentTimeMillis());
  }
  
  @Override
  protected synchronized boolean removeEldestEntry(
      @SuppressWarnings("unused") Map.Entry<Object, Object> eldest) {
    removeExpired();
    return size() > pMaxSize;
  }

  @Override
  public synchronized Object put(Object key, Object value) {
    removeExpired();
    putInTimeStamp(key);
    return super.put(key, value);
  }
  
  @Override
  public Object get(Object key) {
    removeExpired();
    return super.get(key);
  }
  
  @Override
  public synchronized Object remove(Object key) {
    pTimeStamps.remove(key);
    return super.remove(key);
  }
  
  @Override
  public synchronized void clear() {
    pTimeStamps.clear();
    super.clear();
  }

}
