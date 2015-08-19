package approxlib.util;

/**
 * Keep track of the maximum overall memory usage of the programm. 
 * This class has only static members.
 * 
 * Calling <code>Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()</code>
 * returns only the heap memory. This class measures heap- and non-heap memory!
 * 
 * @author Denilson Barbosa
 *
 */
public class MemoryWatch {
	private static long maxHeap = 0;
	private static long maxNonHeap = 0;
	
	/**
	 * Set this to true if you want to measure main memory usage.
	 */
	public static boolean active = false;
	
	/**
	 * Set maximum memory usage to zero.
	 *
	 */
	public static void reset(){
		maxHeap = 0;
		maxNonHeap = 0;
	}
	
	/**
	 * Measure current memory usage if MemoryWatch.active=true. 
	 * 
	 * <p>Note: Calling System.gc() before calling this method may decrease the measured value.
	 * System.gc() is expensive and may take some seconds. Thus it is not called by {@link #measure()}.
	 *
	 */	
	public static void measure(){
		if (!active) return;
		System.gc();
		long currentHeap = 
			java.lang.management.ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
		long currentNonHeap = 
			java.lang.management.ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed();
		
		if (currentHeap > maxHeap) maxHeap = currentHeap;
		if (currentNonHeap > maxNonHeap) maxNonHeap = currentNonHeap;
	}

	/**
	 * @return maximum overall memory (heap + non-heap) measured since last reset
	 */
	public static long getMax() {
		return maxHeap + maxNonHeap;
	}
	
	/**
	 * @return the maxHeap
	 */
	public static long getMaxHeap() {
		return maxHeap;
	}
	
	/**
	 * @return the maxNonHeap
	 */
	public static long getMaxNonHeap() {
		return maxNonHeap;
	}		
	
	
	
}
