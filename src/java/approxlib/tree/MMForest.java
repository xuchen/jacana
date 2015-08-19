package approxlib.tree;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.LineNumberReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Vector;
import java.util.Collections;
import java.util.Arrays;

public class MMForest extends Vector<LblValTree> implements Forest {

	public MMForest(int initialCapacity, int capacityIncrement) {
		super(initialCapacity, capacityIncrement);
	}

	public MMForest(String filename) throws IOException {
		super();

		LineNumberReader in = new LineNumberReader(new FileReader(filename));

		String inline;
		while ((inline = in.readLine()) != null) {
			if (inline.charAt(0) != '#') { // if line is not a comment
				LblValTree node = new LblValTree(LblTree.fromString(inline));
				add(node);
			}
		}
		in.close();
	}

	// labels can be empty, but not ' ' (blank)
	public void writeToFile(String filename) throws IOException {

		BufferedWriter out = new BufferedWriter(new FileWriter(filename));

		for (int i = 0; i < size(); i++) {
			LblValTree el = elementAt(i);
			String s = el.toString() + '\n';
			out.write(s, 0, s.length());
		}
		out.close();
	}

	public int getNodeCount() {
		int sum = 0;
		for (int i = 0; i < size(); i++) {
			sum += elementAt(i).getNodeCount();
		}
		return sum;
	}

	public void prettyPrint() {
		for (int i = 0; i < size(); i++) {
			elementAt(i).prettyPrint();
		}
	}


	public MMForest getSubforest(int[] treeIDs) {
		Arrays.sort(treeIDs);
		MMForest f = (MMForest) clone();
		Collections.sort(f, new Comparator() {
			public int compare(Object o1, Object o2) {
				int id1 = ((LblTree)o1).getTreeID();
				int id2 = ((LblTree)o2).getTreeID();
				return id1 - id2;
			}
		});
		
		MMForest sf = new MMForest(320, 20);

		int i = 0;
		int j = 0;
		while ((i < treeIDs.length) && (j < f.size())) {
			if (treeIDs[i] > f.getTreeAt(j).getTreeID()) {
				j++;
			} else if (treeIDs[i] < f.getTreeAt(j).getTreeID()) {
				i++;
			} else { // treeIDs[i] == f.getTreeAt(j).getTreeID()
				sf.add(f.getTreeAt(j));
				i++;
				j++;
			}
		}
		return sf;
	}

	/**
	 * O(n) method that gets a tree by treeID.
	 * 
	 * @param treeID
	 * @return first tree in forest with this treeID. If there are other trees
	 *         with the same ID later in the list, they will be ignored. If not
	 *         tree with this ID is found, null is returned.
	 */
	public LblValTree getTree(int treeID) {
		for (int i = 0; i < size(); i++) {
			if (getTreeAt(i).getTreeID() == treeID) {
				return getTreeAt(i);
			}
		}
		return null;
	}

	public LblValTree getTreeAt(int i) {
		return elementAt(i);
	}

	public int[] getTreeIDs() {
		int[] treeIDs = new int[this.size()];
		for (int i = 0; i < this.size(); i++) {
			treeIDs[i] = this.elementAt(i).getTreeID();
		}
		return treeIDs;
	}

	
	/**
	 * @see tree.Forest#loadForest(int)
	 */
	public MMForest loadForest() throws SQLException {
		MMForest f = new MMForest(this.size(), 10);
		for (int i = 0; i < this.size(); i++) {
			f.add(LblValTree.deepCopy(this.getTreeAt(i)));
		}
		return f;
	}

	/**
	 * O(n) implementation!
	 * @see tree.Forest#loadTree(int, int)
	 */
	public LblValTree loadTree(int treeID) throws SQLException {
		return LblValTree.deepCopy(this.getTree(treeID));
	}

	/**
	 * @see tree.Forest#storeForest(tree.MMForest)
	 */
	public void storeForest(MMForest f) throws SQLException {
		for (int i = 0; i < f.size(); i++) {
			this.storeTree(f.getTreeAt(i));
		}
	}

	/**
	 * @see tree.Forest#storeTree(tree.LblValTree)
	 */
	public void storeTree(LblValTree t) throws SQLException {
		this.add(LblValTree.deepCopy(t));
		//this.add(t);
	}

	public Iterator<LblValTree> forestIterator() throws SQLException {
		return new MMForestIterator(this);
	}

	public long getForestSize() throws SQLException {
		return this.size();
	}

	
	
}
