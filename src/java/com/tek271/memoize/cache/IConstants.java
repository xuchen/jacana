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
 * Some constants used within this package.
 * @author Abdul Habra
 * @version 1.0
 */
interface IConstants {
  int INITIAL_CAPACITY= 16;
  float LOAD_FACTOR= 0.75f;
  boolean LRU_ORDER= true;
  int MAX_SIZE= 128;
  long TTL= 2;
  TimeUnitEnum TIME_UNIT= TimeUnitEnum.MINUTE;
}