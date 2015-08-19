/*
 * Created on Mar 10, 2005
 */
package approxlib.distance;

import approxlib.tree.LblTree;


/**
 * @author augsten
 */
public abstract class ProfileDist extends TreeDist {


	public static final int BAG_NORM = 0;
	public static final int DICE_NORM = 1;
		
	private int typeOfNormalization = BAG_NORM;
	
	public ProfileDist(boolean normalized) {
		super(normalized);
	}
	
	/**
	 * Return manhattan distance (i.e. L<sub>1</sub> distance) between the
	 * corresponding vectors (v<sub>i</sub> of the profiles p<sub>i</sub>).
	 * The profiles MUST be sorted!
	 * 
	 * L<sub>1</sub>(v<sub>1</sub>, v<sub>2</sub>) = |p<sub>1</sub> &cup; p<sub>2</sub>| &minus; 2 |p<sub>1</sub> &cap; p<sub>2</sub>|
	 * 
	 * @param p2 an other profile
	 * @return L<sub>1</sub> distance of this profile to <tt>p</tt>
	 */
	public double intersect(Profile p1, Profile p2) {
		int i1 = 0, i2 = 0;
		double insec = 0;
		while ((i1 < p1.size()) && (i2 < p2.size())) {
			int cmp = (p1.elementAt(i1)).compareTo(p2.elementAt(i2));
			if (cmp == 0) {
				i1++;
				i2++;
				insec++;
			} else if (cmp < 0) {
				i1++;
			} else {
				i2++;
			}
		}
		return insec;
	}
		
	
	
	@Override
	public double treeDist(LblTree t1, LblTree t2) {
		Profile p1 = getProfile(t1);
		Profile p2 = getProfile(t2);
		p1.sort();
		p2.sort();
		t1.setTmpData(p1);
		t2.setTmpData(p2);
		double intersection = this.intersect(p1, p2);
		double dist;
		if (this.isNormalized()) {
			switch (this.typeOfNormalization) {
			case DICE_NORM:
				dist = 
					((p1.cardinality() + p2.cardinality()) - 2 * intersection) / 
					(p1.cardinality() + p2.cardinality());	
				break;
			case BAG_NORM:
			default:
				dist = 
					((p1.cardinality() + p2.cardinality()) - 2 * intersection) / 
					(p1.cardinality() + p2.cardinality() - intersection);
			}
		} else {
			dist = 
				((p1.cardinality() + p2.cardinality()) - 2 * intersection);			
		}
		return dist;
	}

	/**
	 * Calculates a new profile for a tree.
	 * 
	 * @param t tree
	 * @return profile
	 */
	public abstract Profile createProfile(LblTree t);
	
	/**
	 * Return the profile stored in tree t. If t stores no profile,
	 * or the profile is not of the correct type, null is returned.
	 * 
	 * This implementation gets the profile with getTmpData() and checks, 
	 * whether it is of type Profile. Subclasses of ProfileDist
	 * can do more specialized checks, but should respect the behavior above.
	 *  
	 * @param t tree
	 * @return profile of t, if t stores a valid profile, 
	 *         null otherwise
	 */
	public Profile getStoredProfile(LblTree t) {
		Profile p = null;
//		if (t.getTmpData() == null) {
//			System.out.print(".");
//		}
		if (t.getTmpData() != null) {
			try {
				p = (Profile)t.getTmpData();
			} catch (ClassCastException e) {
				p = null;
			}
		} 
		return p;
	}
	
	/**
	 * Returns the profile of the tree. If no valid profile is stored in the 
	 * tree (i.e. getStoredProfile returns null), it is caluclated using
	 * createProfile().
	 * 
	 * @param t
	 * @return Profile
	 */
	public Profile getProfile(LblTree t) {
		Profile prof = getStoredProfile(t);
		if (prof == null) {
			prof = createProfile(t);
		}
		return prof;
	}

	public int getTypeOfNormalization() {
		return typeOfNormalization;
	}

	public void setTypeOfNormalization(int typeOfNormalization) {
		this.typeOfNormalization = typeOfNormalization;
	}


	@Override
	public String toString() {
		String strNorm = "(not normalized)";
		if (this.isNormalized()) {
			switch (this.typeOfNormalization) {
			case ProfileDist.BAG_NORM:
					strNorm = "(bag normalization)";
			break;
			case ProfileDist.DICE_NORM:
				strNorm = "(dice normalization)";
				break;				
			}
		}
		return this.getClass().getSimpleName() + strNorm;
	}
	
}
