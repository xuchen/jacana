/*
 * Created on Mar 10, 2005
 */
package approxlib.distance;

/**
 * @author augsten
 */
public class PQGramProfile extends VectorProfile {
	
	public PQGramProfile(int initialCapacity, int capacityIncrement) {
		super(initialCapacity, capacityIncrement);
	}

	/**
	 * @return p-value of the first pq-gram in the vector, or -1 if
	 * no pq-gram is there.
	 */
	public int getP() {
		if (size() > 0) {
			return ((PQGram)elementAt(0)).getP();
		} else {
			return -1;
		}
	}
	
	/**
	 * @return q-value of the first pq-gram in the vector, or -1 if
	 * no pq-gram is there.
	 */
	public int getQ() {
		if (size() > 0) {
			return ((PQGram)elementAt(0)).getQ();
		} else {
			return -1;
		}	
	}

	
}
