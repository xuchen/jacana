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
 * Cache factory that creates and maintains instances of cache objects.
 * @author Abdul Habra
 * @version 1.0
 */
public interface ICacheFactory {
  
  /**
   * If the factory has a cache with the given name, return it, otherwise, create a new
   * one. 
   * <p>Note that if a cache already exists with the given name, it will be returned, 
   * ignoring the other parameters of this method: maxSize, timeToLive, and timeUnit.
   * </p> 
   * @param cacheName The name of the cache. This name must be unique for each cache.
   * @param maxSize Maximum number of entries allowed in the cache.
   * @param timeToLive The period of time after which cache items will expire, and removed
   * from the cache.
   * @param timeUnit The Unit of time used for the <code>timeToLive</code>.
   * @return A cache with the given name.
   */
  ICache getCache(String cacheName, int maxSize, long timeToLive, TimeUnitEnum timeUnit);
  
  /**
   * If the factory has a cache with the given name, return it, otherwise, create a new
   * one. The maxSize, timeToLive, and timeUnit will assume some default values
   * specified by the class implementing this interface. 
   * @param cacheName The name of the cache. This name must be unique for each cache.
   * @return A cache with the given name.
   */
  ICache getCache(String cacheName);
  
  /** Remove all cahces from this factory */
  void clear();
  
  /** Remove the cache with the given name */
  void remove(String cacheName);
  
  /** Remove all expired cache entries from all caches. */
  void removeExpired();
}
