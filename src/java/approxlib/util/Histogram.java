/**
 * 
 */
package approxlib.util;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

/**
 * @author Nikolaus Augsten
 *
 */
public class Histogram {
	
	private static HashMap<Integer,Integer> map;
	public static boolean active = false;
	
	/**
	 * 
	 * @param map
	 */
	public static void init(HashMap<Integer,Integer> map) {
		Histogram.map = map;
	}

	public static void put(Integer key) {
		if (!Histogram.active) return;
		if (map.containsKey(key)) {
			map.put(key, map.get(key) + 1);			
		} else {
			map.put(key, 1);
		}
	}
	
	public static void print(PrintStream out) {
		if (!Histogram.active) return;
		int[] keys = new int[map.size()];
		int i = 0;
		for (Iterator<Integer> it = map.keySet().iterator(); it.hasNext();) {
			keys[i++] = it.next();
		}
		Arrays.sort(keys);
		for (i = 0; i < keys.length; i++) {
			out.print(keys[i] + "\t" + map.get(keys[i]) + "\n");
		}
	}
		
}
