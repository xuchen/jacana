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
 * An enumeration of different time units: milliSeconds, seconds, minutes, and hours.
 * @author Abdul Habra
 * @version 1.0
 */
public enum TimeUnitEnum {
  MILLI(1L), 
  SECOND(1000L), 
  MINUTE(60L * 1000L), 
  HOUR(3600L * 1000L);
  
  private long pMilliSeconds;
  
  private TimeUnitEnum(long milliSeconds) {
    pMilliSeconds= milliSeconds;
  }
  
  public long getMilliSeconds() {
    return pMilliSeconds;
  }
  
}
