package com.tek271.memoize;

import java.util.ArrayList;
import java.util.List;

import com.tek271.memoize.cache.TimeUnitEnum;

public class ExpensiveCalcs {
  final static long DELAY= 100L;  // milli seconds
  final static List<String> LOG= new ArrayList<String>();
  
  private String prefix= "";
  
  public ExpensiveCalcs() {
    // empty
  }
  
  public ExpensiveCalcs(String prefix) {
    this.prefix= prefix;
  }
  
  private void log(String msg) {
    msg= prefix + msg;
    LOG.add(msg);
    System.out.println(msg);
  }
  
  public static void clearLogAndCache() {
    LOG.clear();
    RememberFactory.clearCache();
  }
  
  private static void sleep() throws Exception {
    Thread.sleep(DELAY + 55); // add 55 for system clock error factor
  }
  
  final static String VOID_ISNOT_MEMOIZED= "In voidIsNotMemoized()";
  @Remember
  public void voidIsNotMemoized() {
    log(VOID_ISNOT_MEMOIZED);
  }
  
  final static String NO_ANNOTATION= "noAnnotation()";
  public long noAnnotation() throws Exception {
    log(NO_ANNOTATION);
    sleep();
    return System.currentTimeMillis();
  }
  
  final static String NOPARAMS= "noParams()";
  @Remember
  public long noParams() throws Exception {
    log(NOPARAMS);
    sleep();
    return System.currentTimeMillis();
  }
  
  final static String WITHPARAMS= "withParams()";
  @Remember
  public String withParams(String param1) throws Exception {
    log(WITHPARAMS);
    sleep();
    return param1.toUpperCase();
  }
  
  private static final String[] NAMES= {"abdul", "doug", "scott", "tom", "jeff"};
  private static final int[] SALARIES= {100,     200,    300,     400,   500 };
  
  private static int lookupSalary(String name) {
    int i= UtilsArrays.searchStringArray(NAMES, name);
    if (i<0) return -1;
    return SALARIES[i];
  }
  
  @Remember(
      excludedParametersIndex={0}
  )
  public int getSalary(Object dbConnection, String name) {
    log("getSalary(" + dbConnection + ", " + name + ")");
    return lookupSalary(name);
  }
  
  @Remember (
      maxSize=2,
      timeToLive=60,
      timeUnit=TimeUnitEnum.SECOND
  )
  public int limitedCacheSize(int a) {
    log("limitedCacheSize(" + a + ")");
    return a*a;
  }
  
}
