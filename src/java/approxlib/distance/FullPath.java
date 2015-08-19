/*
 * Created on Jul 8, 2008
 */
package approxlib.distance;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;

import approxlib.tree.LblTree;

/**
 * Implements Buttler, 2004, Int. Conf. on Internet Computing, 
 * "A short survey of document structure similarity algorithms".
 * 
 * @author naugsten
 */
public class FullPath extends ProfileDist {

	int windowSize;
	
	/**
	 * 
	 * @param windowSize
	 */
	public FullPath(int windowSize, boolean normalized) {
		super(normalized);
		this.windowSize = windowSize;
	}
	
	@Override
	public Profile createProfile(LblTree t) {
		VectorProfile prof = new VectorProfile(100, 100);
		for (Enumeration<LblTree> e = t.preorderEnumeration(); e.hasMoreElements();) {
			LblTree node = e.nextElement();
			LinkedList<String> path = getPath(node, new LinkedList());
			StringBuffer concat = new StringBuffer();
			for (Iterator<String> it = path.iterator(); it.hasNext();) {
				concat.append(it.next());
			}
			prof.add(concat.toString());
		}
		return prof;
	}
	
	public static LinkedList<String> getPath(LblTree node, LinkedList<String> path) {
		path.addFirst(node.getLabel());
		if (!node.isRoot()) {
			path = getPath((LblTree)node.getParent(), path);
		}
		return path;
	}
}
