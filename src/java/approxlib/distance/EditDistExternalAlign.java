package approxlib.distance;

import java.util.ArrayList;
import java.util.HashSet;

import edu.jhu.jacana.dependency.DependencyTree;

/**
 * A hack class that inherits basic <code>EditDist</code> and computes edit scripts
 * from external alignments. It doesn't fill in most data fields, but those only
 * needed in feature extraction, so that the whole pipeline doesn't fail. This class
 * was only used for quick research experiment.
 * 
 */
public class EditDistExternalAlign extends EditDist {

    public EditDistExternalAlign(boolean normalized) {
      super(normalized);
    }
  
    public EditDistExternalAlign(double ins, double del, double update, boolean normalized) {
      super(ins, del, update, normalized);
    }

	public double treeDist(DependencyTree atree, DependencyTree qtree) {
		if (this.isNormalized()) {
			this.dist =  nonNormalizedTreeDist(atree, qtree) / (atree.getSize() + qtree.getSize());
			return this.dist;
		} else {
			this.dist = nonNormalizedTreeDist(atree, qtree);
			return this.dist;
		}
	}

	public double nonNormalizedTreeDist(DependencyTree atree, DependencyTree qtree) {
	  
	    // fill in pos1 for NearestDistanceToAlign
	    pos1 = new String[atree.getLabels().size()];
	    for (int i=0; i<atree.getLabels().size(); i++) {
	      pos1[i] = atree.getLabels().get(i).tag();
	    }


		editList = new ArrayList<Edit>();
		double cost = 0.0;
		for (int i=0; i<atree.getAlignIndices().size(); i++) {
		  int j = atree.getAlignIndices().get(i);
		  if (j != -1) {
  		    if (! (i == atree.getAlignIndices().size() - 1 && j == qtree.getAlignIndices().size() - 1 &&
  		           atree.getDependencies().get(i).reln().toString().equalsIgnoreCase("p")
  		           && qtree.getDependencies().get(j).reln().toString().equalsIgnoreCase("p"))) {
  		        // ignore punctuation alignment
    		    // alignment found
    		    cost += this.getUpdate();
    		    align1to2.put(i, j);
    		    align2to1.put(j, i);
    		    boolean posMatch = atree.getLabels().get(i).tag().equals(qtree.getLabels().get(j).tag());
    		    boolean relMatch = atree.getDependencies().get(i).reln().toString().equals(qtree.getDependencies().get(j).reln().toString());
    		    if (!posMatch && !relMatch)
    		      editList.add(new Edit(Edit.TYPE.REN_POS_REL, i, j));
    		    else if (!posMatch)
    		      editList.add(new Edit(Edit.TYPE.REN_POS, i, j));
    		    else if (!relMatch)
    		      editList.add(new Edit(Edit.TYPE.REN_REL, i, j));
    		    else
    		      ; // do nothing, this node is aligned
  		    }
		  }
		}
		// tree1 is answer tree
		for (int i=0; i<atree.getSize(); i++) {
		  if (!align1to2.containsKey(i)){
		    // deleted
		    cost += this.getDel();
		    if (atree.getTree().get(i).isLeaf()) {
		      editList.add(new Edit(Edit.TYPE.DEL_LEAF, i));
		    } else {
		      editList.add(new Edit(Edit.TYPE.DEL, i));
		    }
		  }
		}
		// tree2 is question tree
		for (int j=0; j<qtree.getSize(); j++) {
		  if (!align2to1.containsKey(j)){
		    // deleted
		    cost += this.getIns();
		    if (qtree.getTree().get(j).isLeaf()) {
		      editList.add(new Edit(Edit.TYPE.INS_LEAF, j, 0, 0, 0));
		    } else {
		      editList.add(new Edit(Edit.TYPE.INS, j, 0, 0, 0));
		    }
		  }
		}
	    return cost;
	}

	@Override
	public String printEditScript() {
		//this.fixEditScriptTopDown();
	
		//mergeEditScript();
		
		StringBuilder editString = new StringBuilder();
		for (int k=editList.size()-1; k>=0; k--)
			editString.append(editList.get(k)+";");
		editScript = editString.toString().substring(0, editString.length()-1);
		
		/*
		editString = new StringBuilder();
		for (int k=compactEditList.size()-1; k>=0; k--)
			editString.append(compactEditList.get(k)+";");
		compactEditScript = editString.toString().substring(0, editString.length()-1);
		*/
		return editScript;
	}
	
	
	@Override
	public int id2idxInWordOrder2(int id) { return id; }
	
	@Override
	public int id2idxInWordOrder1(int id) { return id; }
}
