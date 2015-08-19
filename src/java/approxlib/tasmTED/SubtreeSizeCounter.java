/**
 * 
 */
package approxlib.tasmTED;

import approxlib.util.Histogram;

/**
 * @author Nikolaus Augsten
 *
 */
public class SubtreeSizeCounter implements PostorderQueue {

	/* (non-Javadoc)
	 * @see tasmTED.PostorderQueue#append(java.lang.String, int)
	 */
	public void append(String label, int subtreeSize) {
		Histogram.put(subtreeSize);		
	}
	
	

}
