/*
 * Created on Feb 21, 2005
 */
package approxlib.distance;

import approxlib.tree.LblTree;
import java.util.Enumeration;


/**
 * @author augsten
 */
public class HistoDist extends ProfileDist {

	public HistoDist(boolean normalized) {
		super(normalized);
	}
	
	/* (non-Javadoc)
	 * @see distance.ProfileDist#createProfile(tree.LblTree)
	 */
	@Override
	public Profile createProfile(LblTree t) {		
		VectorProfile profile = new VectorProfile(t.getNodeCount(), 100);
		for (Enumeration e = t.preorderEnumeration(); e.hasMoreElements();) {
			profile.add(((LblTree)e.nextElement()).getLabel()); 
		}
		return profile;
	}
}
