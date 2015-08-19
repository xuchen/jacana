package approxlib.distance;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.NoSuchElementException;

import approxlib.tree.LblTree;

public class InclusionDist extends EditBasedDist {

	public short[][] inc;
	
	public static final short INCLUDED = 1;
	public static final short NOT_INCLUDED = -1;
	public static final short NOT_COMPUTED = 0;
	
	public InclusionDist() {
		super(false);
	}
	
	private int addPostorderNumbers(LblTree t, int id) {
		for (Enumeration<LblTree> e = t.children(); e.hasMoreElements(); ) {
			id = addPostorderNumbers(e.nextElement(), id);
		}
		t.setTmpData(new Integer(id++));
		return id;		
	}
	
	private int getPost(LblTree t) {
		return ((Integer)t.getTmpData()).intValue();
	}
	
	private short allChildrenIncluded(LblTree t1, LblTree t2) {
		Enumeration<LblTree> seq2 = t2.children();
		for (Enumeration<LblTree> seq1 = t1.children(); seq1.hasMoreElements(); ) {
			LblTree c1 = seq1.nextElement();
			LblTree c2;
			do {
				try {
					c2 = seq2.nextElement();
				} catch (NoSuchElementException e) {
					return NOT_INCLUDED;
				}
			} while ((seq2.hasMoreElements()) && (included(c1, c2) == NOT_INCLUDED));
		}
		return INCLUDED;
	}
	
	
	private short included(LblTree t1, LblTree t2) {
		short result = inc[getPost(t1)][getPost(t2)];
		if (result != NOT_COMPUTED) {
			return result;
		}
		if (t1.getLabel().equals(t2.getLabel())) {
			result = allChildrenIncluded(t1, t2);
		} else {
			result = NOT_INCLUDED;
		}
		inc[getPost(t1)][getPost(t2)] = result;
		return result;
	}
	
	@Override
	public double nonNormalizedTreeDist(LblTree t1, LblTree t2) {
		// add postorder numbers to trees (start with 0)
		addPostorderNumbers(t1, 0);
		addPostorderNumbers(t2, 0);
		// matrix for storing results of subproblems
		inc = new short[getPost(t1) + 1][getPost(t2) + 1];
		for (int i = 0; i < inc.length; i++) {
			Arrays.fill(inc[i], NOT_COMPUTED);
		}
		
		
		short isIncluded = included(t1, t2);
		
		// delete consecutive IDs from tree nodes
		t1.clearTmpData();
		t2.clearTmpData();
		return isIncluded;
	}


}


