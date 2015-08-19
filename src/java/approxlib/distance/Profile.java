	/*
 * Created on Mar 10, 2005
 */
package approxlib.distance;

/**
 * A profile is a multi-set of elements that implement the 
 * comparable interface.
 * 
 * @author augsten
 */
public abstract class Profile {
	
	private boolean sorted = false;
	
	/**
	 * Get the cardinality of the profile (i.e. number of non-zero dimensions in 
	 * corresponding vector).
	 * 
	 * @return cardinality of profile
	 */
	abstract public int size();

	/**
	 * Get the i-th element of the profile sorted in ascending order.
	 * 
	 * @param i get i-th element, first element has number 0
	 * @return i-th element of profile
	 */
	abstract public Comparable elementAt(int i);
	
	/**
	 * 
	 * After using this, the profile will not necessarily be sorted. If you
	 * implement it, set sorted to false! 
	 * 
	 * @param el
	 */
	abstract public void add(Comparable el);

	/**
	 * Should sort the profile if {@link #sorted} is false, and then set {@link #sorted} to true.
	 *
	 */
	abstract public void sort();
	
	public double cardinality() {
		return this.size();
	}
	
	public boolean getSorted() {
		return sorted;
	}
	
	public void setSorted(boolean sorted) {
		this.sorted = sorted;
	}
	
}
