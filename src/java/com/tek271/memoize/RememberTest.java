package com.tek271.memoize;

import static com.tek271.memoize.ExpensiveCalcs.*;
import junit.framework.TestCase;

public class RememberTest extends TestCase {
  
  private static ExpensiveCalcs getProxiedExpensiveCalcs() {
    clearLogAndCache();
    ExpensiveCalcs ec= RememberFactory.createProxy(ExpensiveCalcs.class);
    return ec;
  }
  
  public void testVoidNoParams() {
    ExpensiveCalcs ec= getProxiedExpensiveCalcs();
    ec.voidIsNotMemoized();
    assertEquals(1, LOG.size());
    assertEquals(VOID_ISNOT_MEMOIZED, LOG.get(0));
  }

  public void testNoAnnotation() throws Exception {
    ExpensiveCalcs ec= getProxiedExpensiveCalcs();
    long t1= ec.noAnnotation();
    long t2= ec.noAnnotation();
    assertEquals(2, LOG.size());
    assertTrue( t2 >= (t1 + DELAY) );
  }
  
  public void testNoParams() throws Exception {
    ExpensiveCalcs ec= getProxiedExpensiveCalcs();
    long t1= ec.noParams();
    long t2= ec.noParams();
    long t3= ec.noParams();
    assertEquals(1, LOG.size());
    assertEquals(t1, t2);
    assertEquals(t1, t3);
  }
  
  public void testWithParams() throws Exception {
    ExpensiveCalcs ec= getProxiedExpensiveCalcs();
    String s1= ec.withParams("abdul");
    assertEquals("ABDUL", s1);
    
    String s2= ec.withParams("abdul");
    assertEquals("ABDUL", s2);
    
    assertEquals(1, LOG.size());
    
    String s3= ec.withParams("java");
    assertEquals("JAVA", s3);
    assertEquals(2, LOG.size());
  }
  
  public void testGetSalary() {
    // the annotation param at index 0 is excluded in ExpensiveCalcs.getSalary()
    ExpensiveCalcs ec= getProxiedExpensiveCalcs();

    // first use new lookups
    ec.getSalary(new String("fake db connection1") , "abdul");
    int s2= ec.getSalary(new String("fake db connection2") , "tom");
    ec.getSalary(new String("fake db connection3") , "jeff");
    ec.getSalary(new String("fake db connection4") , "scott");
    
    // now use items that should have been cached
    int s5= ec.getSalary(new String("fake db connection5") , "tom");
    ec.getSalary(new String("fake db connection6") , "abdul");
    
    assertEquals(s2, s5);
    assertEquals(4, LOG.size());
  }
  
  public void testCacheMaxSize() {
    ExpensiveCalcs ec= getProxiedExpensiveCalcs();

    // first use new lookups, the annotation's maxSize=2
    ec.limitedCacheSize(1);
    ec.limitedCacheSize(2);

    // now exceed cache size, which will cause 1 & 2 to be flushed
    ec.limitedCacheSize(3);
    ec.limitedCacheSize(4);
    
    // if cache size limitation was not enforced, this should be retreived from cache
    // and the LOG.size would have been 4 instead of 5
    ec.limitedCacheSize(1);
    assertEquals(5, LOG.size());
  }
  
  public void testDecorate() throws Exception {
    clearLogAndCache();
    ExpensiveCalcs ec= new ExpensiveCalcs();
    ExpensiveCalcs decorated= RememberFactory.decorate(ec);
    String s1= decorated.withParams("abdul");
    assertEquals("ABDUL", s1);
    
    String s2= decorated.withParams("abdul");
    assertEquals("ABDUL", s2);
    
    assertEquals(1, LOG.size());
    String s3= decorated.withParams("java");
    assertEquals("JAVA", s3);
    assertEquals(2, LOG.size());
  }
  
  public void testDecorateWillActuallyUseDecoratedObject() throws Exception {
    clearLogAndCache();
    String prefix= "+";
    ExpensiveCalcs ec= new ExpensiveCalcs(prefix);
    ExpensiveCalcs decorated= RememberFactory.decorate(ec);
    
    String s1= decorated.withParams("abdul");
    assertEquals("ABDUL", s1);
    String s2= decorated.withParams("abdul");
    assertEquals("ABDUL", s2);
    
    assertEquals(1, LOG.size());
    assertEquals(prefix +  WITHPARAMS, LOG.get(0));
  }
  
  public void testDecorateNullShouldThrowNPE() {
    clearLogAndCache();
    try {
      RememberFactory.decorate(null);
      fail();
    } catch (NullPointerException e) {
      //
    }
  }
  
}
