/*
 * Created on Feb 1, 2005
 */
package approxlib.tree;

import java.util.Comparator;

/**
 * @author augsten
 */
public class TreeIDComparator implements Comparator {
	
	public int compare(Object o1, Object o2) {
		int id1 = ((LblTree)o1).getTreeID();
		int id2 = ((LblTree)o2).getTreeID();
		return id1 - id2;
	}
}
