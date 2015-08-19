/**
 * 
 */
package approxlib.tasmTED;

/**
 * Postorder queue that trashed whatever is appended.
 * 
 * @author Nikolaus Augsten
 *
 */
public class DummyPostorderQueue implements PostorderQueue {

	/* (non-Javadoc)
	 * @see tasmTED.PostorderQueue#append(java.lang.String, int)
	 */
	public void append(String label, int subtreeSize) {
		System.out.print("(" + label + "," + subtreeSize + ") ");
	}

}
