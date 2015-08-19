package com.tek271.memoize;

class UtilsArrays {
  
  /** Check of the array is null or zero-length */
  public static boolean isEmptyArray(final Object[] array) {
    return array==null || array.length==0;
  }

  /** Check of the array is null or zero-length */
  public static boolean isEmptyArray(final int[] array) {
    return array==null || array.length==0;
  }
  
  /** Find the index of target in array, -1 if not found */ 
  public static int searchIntArray(final int[]array, int target) {
    if (array==null) return -1;
    for (int i=0,n=array.length; i<n; i++) {
      if (array[i]==target) return i;
    }
    return -1;
  }
  
  /** Check if the given array contains target */  
  public static boolean isContain(final int[]array, int target) {
    return searchIntArray(array, target) >= 0;
  }
  
  /** Find the index of target in array, -1 if not found */ 
  public static int searchStringArray(final String[]array, String target) {
    if (array==null) return -1;
    for (int i=0,n=array.length; i<n; i++) {
      if (array[i].equals(target)) return i;
    }
    return -1;
  }

}
