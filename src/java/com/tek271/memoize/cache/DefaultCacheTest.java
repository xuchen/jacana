package com.tek271.memoize.cache;

import junit.framework.TestCase;

public class DefaultCacheTest extends TestCase {
  private static final int MAX_SIZE=4;
  private static final long TIME_TO_LIVE= 400;
  private static final TimeUnitEnum TIME_UNIT= TimeUnitEnum.MILLI;
  
  private ICache cache;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    cache= new DefaultCache(MAX_SIZE, TIME_TO_LIVE, TIME_UNIT);
    cache.put("1", "a");
    cache.put("2", "b");
    cache.put("3", "c");
    cache.put("4", "d");
  }

  @Override
  protected void tearDown() throws Exception {
    cache= null;
    super.tearDown();
  }

  public void testClear() {
    assertEquals(4, cache.size());
    cache.clear();
    assertEquals(0, cache.size());
  }

  public void testMaxSize() {
    assertEquals(4, cache.getMaxSize());
    cache.put("5", "e");
    assertEquals(4, cache.getMaxSize());
  }

  public void testRemoveExpired() throws Exception {
    Thread.sleep(500);
    cache.removeExpired();
    assertEquals(0, cache.size());
  }

  public void testPut() throws Exception {
    cache.put("5", "e");
    Object val= cache.get("5");
    assertEquals("e", val);
    assertNull(cache.get("1"));
    Thread.sleep(200);
    cache.put("6", "f");
    Thread.sleep(300);
    cache.put("7", "g");
    assertEquals(2, cache.size());
  }

  public void testGet() {
    assertEquals(cache.get("2"), "b");
    assertNull(cache.get("zz"));
  }

  public void testRemove() {
    assertEquals("a", cache.remove("1"));
    assertNull(cache.get("a"));
  }

}
