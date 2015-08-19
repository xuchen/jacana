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

/**
 * Cache is a data structure that keeps a set of entries identified by their name.
 * Each entry in the cache has a unique name. The cache is similar to a Map but with<ul>
 * <li>a given maximum size after which entries will be removed automatically. The policy
 * of which items to remove from the map is determined by the class implementing this
 * interface, a popular policy is LRU (least recently used).
 * <li>a given time-to-live period after which entries will expire.
 * </ul>
 * @author Abdul Habra
 * @version 1.0
 */
public interface ICache {
  
  /** Get the maximum period of time a cache item can be used */
  long getTimeToLive();
  
  /** Get the TimeToLive time unit */
  TimeUnitEnum getTimeToLiveUnit();
  
  /** Get the maximum size of the cache */
  int getMaxSize();
  
  /**
   * Put a (key,value) pair in the cache. If the cache already contains an entry with
   * the given key, the new value will replace the existing one.
   * @return The old value of the key (if any), null otherwise.
   */
  Object put(Object key, Object value);
  
  /**
   * Get the value of the assiciated with the given key.
   * @param key
   * @return The value assiciated with the given key, null if none found.
   */
  Object get(Object key);
  
  /**
   * Check if the cache contains an entry with the given key 
   */
  boolean containsKey(Object key);
  
  /**
   * Remove the cache entry with the given key
   * @param key
   * @return The value that was removed if any. null if not found. 
   */
  Object remove(Object key);
  
  /** Remove all of this cache's entries */
  void clear();
  
  /** Remove all expired cache entries */
  void removeExpired();
  
  /** Number of entries in the cache */
  int size();
}
