/*
 * Created on Mar 10, 2005
 */
package approxlib.distance;

import java.util.Vector;
import java.util.Collections;

/**
 * @author augsten
 */
public class VectorProfile extends Profile {

	Vector elements;
	
	public VectorProfile(int initialCapacity, int capacityIncrement) {
		elements = new Vector(initialCapacity, capacityIncrement);
	}
	
	/* (non-Javadoc)
	 * @see profile.Profile#size()
	 */
	@Override
	public int size() {
		return elements.size();
	}

	/* (non-Javadoc)
	 * @see profile.Profile#elementAt(int)
	 */
	@Override
	public Comparable elementAt(int i) {
		return (Comparable)elements.elementAt(i);
	}
	
	@Override
	public void add(Comparable el) {
		elements.add(el);
		setSorted(false);
	}

	/* (non-Javadoc)
	 * @see profile.Profile#sort()
	 */
	@Override
	public void sort() {
		if (!this.getSorted()) {
			Collections.sort(elements);
			this.setSorted(true);
		}
	}
	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return elements.toString();
	}
}
