/*
 * Created on Jan 4, 2008
 */
package approxlib.distance;

import java.util.Enumeration;
import java.util.NoSuchElementException;

import approxlib.tree.LblTree;

public class BinaryBranchDist extends ProfileDist {

	
	public BinaryBranchDist(boolean normalized) {
		super(normalized);
	}
	
	@Override
	public Profile createProfile(LblTree t) {
		VectorProfile prof = new VectorProfile(100, 100);
		Enumeration<LblTree> breadthFirst = t.breadthFirstEnumeration();
		while (breadthFirst.hasMoreElements()) {
			LblTree currentNode = breadthFirst.nextElement();
			LblTree child;
			try {
				child = (LblTree)currentNode.getFirstChild();
			} catch (NoSuchElementException e) {
				child = new LblTree(PQGram.NULL_SYMBOL, t.getTreeID());
			}
			LblTree sibling = (LblTree)currentNode.getNextSibling();
			if (sibling == null) { sibling = new LblTree(PQGram.NULL_SYMBOL, t.getTreeID()); }
			PQGram binaryBranch = new PQGram(new String[] {currentNode.getLabel(), child.getLabel()}, 
					new String[] {sibling.getLabel()}, null);
			prof.add(binaryBranch);			
		}
		return prof;
	}
	
}
