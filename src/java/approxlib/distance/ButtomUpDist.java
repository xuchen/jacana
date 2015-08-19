package approxlib.distance;

import java.util.*;

import approxlib.tree.LblTree;

public class ButtomUpDist extends EditBasedDist {

	private CompactGraph G; // compacted directed acyclc graph representation
	private LblTree[] M12; // buttom-up mapping between t1 and t2
	private LblTree[] M21; // buttom-up mapping between t1 and t2

	public ButtomUpDist(boolean normalized) {
		this(1, 1, 1, normalized);	
	}

	public ButtomUpDist(double ins, double del, double update, boolean normalized) {
		super(ins, del, update, normalized);	
	}   

	private void compact(LblTree t1, LblTree t2) {
		LinkedList Q = new LinkedList();
		// add additional info to nodes and enque leafs (lines 8-14)
		int prenum = 0;
		for (Enumeration e = t1.preorderEnumeration(); e.hasMoreElements();) {
			LblTree n = (LblTree)e.nextElement();
			n.setTmpData(new NodeData(n, prenum));
			if (n.isLeaf()) {
				Q.add(n);
			}
			prenum++;
		}
		prenum = 0;
		for (Enumeration e = t2.preorderEnumeration(); e.hasMoreElements();) {
			LblTree n = (LblTree)e.nextElement();
			n.setTmpData(new NodeData(n, prenum));
			if (n.isLeaf()) {
				Q.add(n);
			}
			prenum++;
		}

		// lines 3-7
		G = new CompactGraph(t1.getNodeCount() + t2.getNodeCount());

		// lines 15-49
		do {	    
			LblTree v = (LblTree)Q.removeFirst();	    
			boolean reverseMapping = (v.getRoot() == t2.getRoot()); // nodes of the same tree
			// we skip 17-19, as it is done already above
			boolean found = false;	    

			// 21-40
			for (int i = G.size() - 1; i >= 0; i--) {
				// 21-24
				if (!G.equalsHeight(i, v)) {
					break;
				}
				if (!G.equalsNode(i, v)) {
					continue;
				}
				// 25-40
				if (G.checkChildren(i, v)) {
					((NodeData)v.getTmpData()).setCompactGraph(i);
					if (reverseMapping) {
						G.addToK(i, v);
					}
					found = true;
				}
			}
			// 33-41
			if (!found) {
				G.add(v, reverseMapping);
			}

			// 43-48
			if (!v.isRoot()) {
				NodeData nd = (NodeData)((LblTree)v.getParent()).getTmpData();
				nd.decreaseChildren();
				if (nd.hasZeroChildren()) {
					Q.add(v.getParent());
				}
			}
		} while (!Q.isEmpty());
	}

	private int mapping(LblTree t1, LblTree t2) {
		int mapCardinality = 0;
		M12 = new LblTree[t1.getNodeCount()];  // each node of t1 is mapped to 'null' or a node of t2
		M21 = new LblTree[t2.getNodeCount()];  // each node of t2 is mapped to 'null' or a node of t1

		// reset
		for (int i = 0; i < M12.length; i++) {
			M12[i] = null;
		}
		for (int i = 0; i < M21.length; i++) {
			M21[i] = null;
		}

		// go through nodes of t1 in a breadth first traversal
		for (Enumeration e = t1.breadthFirstEnumeration(); e.hasMoreElements();) {

			// current node of t1
			LblTree v = (LblTree)e.nextElement();
			int vPre = ((NodeData)(v.getTmpData())).getPreorder();
			//System.out.println("******* current node pre(v)= " + vPre + ", v=" + v + " **********");

			// line 4
			if (M12[((NodeData)(v.getTmpData())).getPreorder()] == null) {

				// line 5
				LblTree w = (LblTree)t2.getLastLeaf();
				int wPre = ((NodeData)w.getTmpData()).getPreorder(); // preorder-number of w in t2

				// I use this variable to correct a mistake in algorithm of paper
				boolean found = false;

				// uList contains all nodes u of t2 with K[u] = K[v]
				LinkedList uList = G.getK(((NodeData)v.getTmpData()).getCompactGraph());

				// get the left-most node u (i.e. the one with the lowest preorder number)
				for (int i = 0; i < uList.size(); i++) {

					LblTree u = (LblTree)uList.get(i); 		 // i-th node of uList
					int uPre = ((NodeData)u.getTmpData()).getPreorder(); // preorder-number of u in t2
					//System.out.println("i=" + i + " pre(u): " + uPre + " u: " + u);

					// set w to u, if u is not already matched
					if (M21[uPre] == null) {
						// MUST be <=, otherwise, if w=u loop is not entered and found remains false
						if (uPre <= wPre) {  
							w = u;
							wPre = uPre;
							found = true;
						}
					}
					//System.out.println("i=" + i + " pre(w): " + wPre + " w: " + w);
				}

				// I use this instead of K[v]=K[w] to correct algorithm in paper
				if (found) {		    
					int pre1 = vPre;
					int pre2 = wPre;
					Enumeration e1 = v.preorderEnumeration();
					Enumeration e2 = w.preorderEnumeration();
					while ((e1.hasMoreElements()) && (e2.hasMoreElements())) {
						if ((M12[pre1] == null) && (M21[pre2] == null)) {
							M12[pre1] = (LblTree)e2.nextElement();
							M21[pre2] = (LblTree)e1.nextElement();
							mapCardinality++;
						} else {
							//System.out.println("pre1 = " + pre1);
							//System.out.println("pre2 = " + pre2);
							//System.out.println("------------------------------------* Error!");			    
							e1.nextElement();
							e2.nextElement();
						}
						//System.out.println("....................");
						//System.out.println("M12[" + pre1 + "] = " + M12[pre1]);
						//System.out.println("M21[" + pre2 + "] = " + M21[pre2]);
						//System.out.println("....................");
						pre1++;
						pre2++;
					}		  
				}
			}
		}
		//System.out.println("Map: 1->2");
		for (int i = 0; i < M12.length; i++) {
			if (M12[i] != null) {
				//System.out.println(i + ": (M12) :" + ((NodeData)M12[i].getTmpData()).getPreorder());
			}
		}
		//System.out.println();
		//System.out.println("Map 2->1: ");
		for (int i = 0; i < M21.length; i++) {
			if (M21[i] != null) {
				//System.out.println(i + ": (M21) :" + ((NodeData)M21[i].getTmpData()).getPreorder());
			}
		}
		//System.out.println();
		//System.out.println("Map cardinality: " + mapCardinality);
		return mapCardinality;

	}

	
	@Override
	public double treeDist(LblTree t1, LblTree t2) {
		if (this.isNormalized()) {
			return nonNormalizedTreeDist(t1, t2) / Math.max(t1.getNodeCount(), t2.getNodeCount());			
		} else {
			return nonNormalizedTreeDist(t1, t2);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see distance.TreeDist#dist(tree.LblTree, tree.LblTree)
	 */
	@Override
	public double nonNormalizedTreeDist(LblTree t1, LblTree t2) {
		compact(t1, t2);
		//G.prettyPrint();
		int mapCardinality = mapping(t1, t2);
		int max = Math.max(t1.getNodeCount(), t2.getNodeCount());

		// clean additional info from nodes
		t1.clearTmpData();
		t2.clearTmpData();
		return max - mapCardinality;			      
	}

}

class CompactGraph {
    NodeData[] nodes;
    int nextNode;
    int[][] arcs;
    LinkedList[] K; // map from G to t1 and t2

    public CompactGraph(int size) {
	nodes = new NodeData[size];
	arcs = new int[size][];
	K = new LinkedList[size];
	nextNode = 0;
    }

    public void add(LblTree n, boolean reverseMapping) {
	NodeData nd = (NodeData)n.getTmpData();
	nodes[nextNode] = nd;
	nd.setCompactGraph(nextNode);
	arcs[nextNode] = new int[n.getChildCount()];
	int i = 0;	
	for (Enumeration e = n.children(); e.hasMoreElements();) {
	    LblTree child = (LblTree)e.nextElement();
	    arcs[nextNode][i]=((NodeData)child.getTmpData()).getCompactGraph();
	    i++;
	}
	if (K[nextNode] == null) {
	    K[nextNode] = new LinkedList();
	}
	if (reverseMapping) {
	    K[nextNode].add(n);
	}
	nextNode++;
    }

    public LinkedList getK(int i) {
	return K[i];
    }

    public void addToK(int i, LblTree n) {
	K[i].add(n);
    }

    public int size() {
	return nextNode;
    }

    public boolean equalsNode(int i, LblTree n) {
	return ((NodeData)n.getTmpData()).equals(nodes[i]);	
    }

    public boolean equalsHeight(int i, LblTree n) {
	return ((NodeData)n.getTmpData()).height==nodes[i].height;	
    }

    public boolean checkChildren(int k, LblTree n) {
	if (n.getChildCount() == arcs[k].length) {
	    for (int i = 0; i < arcs[k].length; i++) {
		if (arcs[k][i] != 
		    ((NodeData)((LblTree)n.getChildAt(i)).getTmpData()).getCompactGraph()) {
		    return false;
		}		
	    }
	    return true;
	} else {
	    return false;
	}
    }

    public void prettyPrint() {
	for (int i = 0; i < size(); i++) {
	    System.out.print((i+1) + ":'" + nodes[i].label + "' (");
	    for (int j = 0; j < arcs[i].length; j++) {		
		System.out.print((arcs[i][j]+1) + ",");
	    }
	    System.out.print(") - K: ");
	    System.out.println(K[i]);
	}
    }
    
}

class NodeData {
	String label;
	int height;
	int outdegree;
	int children;
	int compactGraph;
	int preorder;
	
	NodeData(LblTree n, int preorder) {
		label = n.getLabel();
		height = n.getDepth();
		outdegree = n.getChildCount();
		children = outdegree;
		this.preorder = preorder;
		compactGraph = -1;
	}
	
	public void setPreorder(int preorder) {
		this.preorder = preorder;
	}
	
	public int getPreorder() {
		return preorder;
	}
	
	public int getCompactGraph() {
		return compactGraph;
	}
	
	public void setCompactGraph(int compactGraph) {
		this.compactGraph = compactGraph;
	}
	
	public void decreaseChildren() {
		children--;
	}
	
	public boolean hasZeroChildren() {
		return (children == 0);
	}
	
	public boolean equals(NodeData nd) {
		return (label.equals(nd.label) && 
				(height == nd.height) &&
				(outdegree == nd.outdegree));
	}
	
	@Override
	public String toString() {
		return "{'" + label + "', h: " + height + " out: " + outdegree + 
		" ch: " + children + ", pre: " + preorder + "}";
	}
}


