/*
 * Created on Feb 9, 2005
 */
package approxlib.distance;

import approxlib.tree.LblTree;

import java.util.Enumeration;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Vector;

/**
 * Implementation of the distance measure proposed by
 * Minos Garofalakis, Amit Kumar, "Correlating XML Data Streams Using
 * Tree-Edit Distance Embeddings", PODS 2002. 
 *
 *  * @author augsten
 */
public class TreeEmbedDist extends ProfileDist {
	
	public static boolean DEBUG = false;
	
	private static final int HASH_CONST = 1000;		/* To be used in hash function */
	private static final int HASH_A = 7901;
	private static final int HASH_B = 7877;
	private static final int HASH_C = 6007;
	private static final int HASH_D = 6073;
	private static final int HL = 31;
	private static final int MOD = Integer.MAX_VALUE;
	
	public TreeEmbedDist(boolean normalized) {
		super(normalized);
	}	
	
	
			
	/* (non-Javadoc)
	 * @see distance.ProfileDist#createProfile(tree.LblTree)
	 */
	@Override
	public Profile createProfile(LblTree t) {
		Profile p = getParseVector(t);
		return p;
	}
	
	private static Profile getParseVector(LblTree t) {
		HashTree ht = new HashTree(t);
		VectorProfile parseVec = new VectorProfile(ht.getNodeCount() * 2, ht.getNodeCount()); 
		int phase = 0;
		do {
			if (DEBUG) {
				ht.prettyPrint();
				System.out.println(ht.toLatex());
			}
			for (Enumeration e = ht.breadthFirstEnumeration(); e.hasMoreElements();) {
				HashTree n = (HashTree)e.nextElement();
				parseVec.add(new SubtreePhaseCombi(n.getHashVal(), phase));
			}
			LinkedList[] l1 = getValidSubtrees(ht);
			handleLoneLeafs(l1[2], ht); 
			handleChains(l1[0], ht);
			handleLeafSeq(l1[1], ht);
			phase++; // next phase in parsing hierarchy
		} while (ht.getNodeCount() != 1);
		if (DEBUG) {
			ht.prettyPrint();
		}
		parseVec.add(new SubtreePhaseCombi(ht.getHashVal(), phase));		
		if (DEBUG) {
			System.out.println("parse array (" + parseVec.size() + " elements):"); 
			for (int i = 0; i < parseVec.size(); i++) {
				System.out.print(parseVec.elementAt(i) + " ");
			}
			System.out.println();
		}
		return parseVec;
	}
	
	private static void handleChains(LinkedList l, HashTree t) {
		for (ListIterator it = l.listIterator(); it.hasNext();) {
			// one of the chains in the tree...	
			LinkedList list = (LinkedList)it.next();
			HashTree[] chain = (HashTree[])list.toArray(new HashTree[list.size()]);  
			
			// split the chain
			int[] split = split(chain);
			
			HashTree p = null;	// new node replacing previous block in chain

			// backup the subtree following the chain
			HashTree subtree = null;
			if (!chain[chain.length - 1].isLeaf()) {
				subtree = (HashTree)chain[chain.length - 1].getFirstChild(); // not leaf => first child exits
			}
			
			for (int s = 0; s < split.length; s++) {
				// length of block (2 or 3)
				int blocklength = chain.length - split[s];
				if (s < split.length - 1) {
					blocklength = split[s + 1] - split[s];
				}
				// store hash codes of the nodes in this block into contract
				int[] contract = new int[blocklength];
				for (int c = 0; c < contract.length; c++) {
					contract[c] = chain[split[s] + c].getHashVal();
					if (c == 0) {
						// we are starting a new block
						if (p != null) {
							// this is not the first block
							chain[split[s]].removeFromParent();
							p.add(chain[split[s]]);
						}				
						// remember the first node in the block (will get it's hashcode later...)
						p = chain[split[s]];
					} else {
						// just forget about the second and third node in a block
						chain[split[s] + c].removeFromParent();
					}
				}
				//chain[split[s]].setHashVal(hash(contract));
				// give p its new hash value
				p.setHashVal(hashChain(contract));
				// form new label
				if (DEBUG) {
					String label = "[";
					for (int c = 0; c < contract.length; c++) {
						label += chain[split[s] + c].getLabel();
						if (c < contract.length - 1) {
							label += ",";
						}
					}
					label += "]";
					chain[split[s]].setLabel(label);
				}
			}
			// link subtree after chain to last of new chain nodes
			if (subtree != null) {
				p.add(subtree);
			}
		}
	}
	
	private static int[] split(HashTree[] l) {
		// copy all hashvalues in an array and split them
		int[] intChain = new int[l.length];
		for (int i = 0; i < l.length; i++) {
			intChain[i] = l[i].getHashVal();
		}
		return Splitter.split(intChain);		
	}
	
	private static void handleLeafSeq(LinkedList l, HashTree t) {
		for (ListIterator it = l.listIterator(); it.hasNext();) {
			// one of the chains in the tree...	
			LinkedList list = (LinkedList)it.next();
			HashTree[] chain = (HashTree[])list.toArray(new HashTree[list.size()]);  
			
			// split the chain
			int[] split = split(chain);
			
			for (int s = 0; s < split.length; s++) {
				// length of block (2 or 3)
				int blocklength = chain.length - split[s];
				if (s < split.length - 1) {
					blocklength = split[s + 1] - split[s];
				}
				// store hash codes of the nodes in this block into contract
				int[] contract = new int[blocklength];
				for (int c = 0; c < contract.length; c++) {
					contract[c] = chain[split[s] + c].getHashVal();
					if (c != 0) {
						// get child-ID "by hand"
						HashTree child = chain[split[s] + c];
						HashTree par = (HashTree)(child.getParent());
						int index = 0;
						for (int chCnt = 0; chCnt < par.getChildCount(); chCnt++) {
							if (child == par.getChildAt(chCnt)) {
								index = chCnt;
								break;
							}
						}						
						par.remove(index);
					}
				}				
				chain[split[s]].setHashVal(hashLeaf(contract));
				// form new label
				if (DEBUG) {
					String label = "(";
					for (int c = 0; c < contract.length; c++) {
						label += chain[split[s] + c].getLabel();
						if (c < contract.length - 1) {
							label += ",";
						}
					}
					label += ")";
					chain[split[s]].setLabel(label);
				}
			}
		}
	}
	
	private static void handleLoneLeafs(LinkedList l, HashTree t) {
		for (ListIterator it = l.listIterator(); it.hasNext();) {
			HashTree n = (HashTree)it.next();
			HashTree p = (HashTree)n.getParent();
			n.removeFromParent();
			p.setHashVal(hashChain(new int[] {n.getHashVal(), p.getHashVal()}));
			if (DEBUG) {
				p.setLabel("{" + p.getLabel() + "-" + n.getLabel() + "}");
			}
		}
	}
	
	private static int hashLeaf(int[] block) {
		int result;
		int x = (block[0] * HASH_CONST * HASH_CONST);
		x += block[1] * HASH_CONST;
		if (block.length == 3) {
			x += block[2];
		}
		
		result = (HASH_A * x) + HASH_B;
		result = ((result >> HL) + result) & MOD;
		return (result);
	}
	
	private static int hashChain(int[] block) {
		int result;
		int x = (block[0] * HASH_CONST * HASH_CONST);
		x += block[1] * HASH_CONST;
		if (block.length == 3) {
			x += block[2];
		}
		
		result = (HASH_C * x) + HASH_D;
		result = ((result >> HL) + result) & MOD;
		return (result);
	}

	
	/**
	 * Get chains, leftmost lone leaf children and contiguous leaf-child subsequences.
	 * 
	 * @param t1
	 * @return {chainList, leafsList, loneLeafs}, where chainList is a list of chains (each chain is a LinkeList of nodes), 
	 * leafsList is a list of maximal conitguous leaf-child subsequenes (each sequence is a LinkedList of nodes), and loneLeafs
	 * is a list of leftmost lone leaf nodes.
	 */
	private static LinkedList[] getValidSubtrees(LblTree t1) {
		LinkedList chain = null;
		LinkedList chainList = new LinkedList();
		LinkedList leafs = null;
		LinkedList leafsList = new LinkedList();
		LinkedList loneLeaf = new LinkedList(); // leafmost lone leafs
		for (Enumeration e = t1.preorderEnumeration(); e.hasMoreElements();) {
			LblTree n = (LblTree)e.nextElement();
			if (n.isLeaf() && !n.isRoot()) {
				// n is a leaf 
				if (chain != null) {
					// if you are within a chain, then also n is in this chain and ends it
					chain.add(n);
					chainList.add(chain);
					chain = null;				
				} else {
					// do nothing 
				}
			} else if ((n.getChildCount() == 1)  && !n.isRoot()) {
				// this is a degree-2 node
				if (chain == null) {
					// you are not within a chain, but maybe at the start
					if ((n.getFirstChild().getChildCount() == 1) || (n.getFirstChild().isLeaf())) {
						// it is the start of a chain
						chain = new LinkedList();
						chain.add(n);
					} else {
						// do nothing
					}
				} else {  
					// you are already within a chain
					chain.add(n);
				}
			} else {
				// n has more than 1 child (or is the root node)

				// is there a chain to end?
				if (chain != null) {
					chainList.add(chain);
					chain = null;
				}				

				// check for leaf children
				boolean leftmost = true;
				for (int i = 0; i < n.getChildCount(); i++) {					
					LblTree c = (LblTree)n.getChildAt(i);
					if (c.isLeaf()) {
						// a leaf starts a new leaf sequence or
						// is added to an existing one...
						if (leafs == null) {
							leafs = new LinkedList();
						}
						leafs.add(c);
					} 
					if ((leafs != null) && (!c.isLeaf() || (i == n.getChildCount() - 1))) {
						// there is a leaf sequence to end
						if (leafs.size() == 1) {
							if (leftmost) {
								// leaf sequence to end is a leftmost lone leaf
								loneLeaf.add(leafs.getFirst());
								leafs = null;
								leftmost = false;
							} else {
								// leaf seq is lone leaf, but not leftmost
								// forget it
								leafs = null;
							}
						} else {
							// leaf sequence of more than 1 leafs
							leafsList.add(leafs);
							leafs = null;
						}
					}
				}
				if (leafs != null) {
					System.out.println("FEHLER!!!");
					System.exit(-1);				
				}
			}
		}
		debugPrint2DList("chain list", chainList);
		debugPrint2DList("leafs list", leafsList);
		debugPrintList("lone leafs", loneLeaf);		
		return new LinkedList[] {chainList, leafsList, loneLeaf};
	}
	
	
	
	private static void debugPrint2DList(String name, LinkedList ll) {
		if (DEBUG) {
		System.out.println(name + ": ");
		int i = 0;
		for (ListIterator it = ll.listIterator(); it.hasNext();) {
			LinkedList l = (LinkedList)it.next();
			debugPrintList(i + ": ", l);
			i++;
		}
		}
	}

	private static void debugPrintList(String name, LinkedList l) {
		if (DEBUG) {
		System.out.print(name + ": ");
		for (ListIterator it = l.listIterator(); it.hasNext();) {
			HashTree n = (HashTree)it.next();
			System.out.print(n.getLabel());
			if (it.hasNext()) {
				System.out.print(",");
			}
		}
		System.out.println();
		}
	}

}

class HashTree extends LblTree {
	
	int hashVal;
	
	public HashTree(LblTree t) {
		super(t.getLabel(),t.getTreeID());
		setHashVal(t.getLabel().hashCode());
		for (int i = 0; i < t.getChildCount(); i++) {
			this.add(new HashTree((LblTree)t.getChildAt(i)));
		}
	}
	
	/**
	 * @return Returns the hashVal.
	 */
	public int getHashVal() {
		return hashVal;
	}
	/**
	 * @param hashVal The hashVal to set.
	 */
	public void setHashVal(int hashVal) {
		this.hashVal = hashVal;
	}
}

class SubtreePhaseCombi implements Comparable {
	
	private int	hashVal;
	private int phase;
	
	public SubtreePhaseCombi(int hashVal, int phase) {
		this.hashVal = hashVal;
		this.phase = phase;
	}
		
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Object arg0) {
		SubtreePhaseCombi spc = (SubtreePhaseCombi)arg0;
		if (spc.hashVal == hashVal) {
			return phase - spc.phase;
		} else {
			return hashVal - spc.hashVal; // negative, if hashVal < spc.hashVal, positive otherwise
		}
	}
	
}

/**
 * <p>
 * Implements a method that partitions a string into blocks of length
 * 2 and 3, according to [1].</p>
 * 
 * <p>
 * Differences with respect to [1]:
 * <ul>
 * <li>Reduction of alphabet size for non-repeating blocks: An imaginary 
 * first character is used to avoid the first <tt>log*|T|</tt> (see subsection 2.1.1)
 * not to be labeled. Thus we do not need to distinguish between metablocks
 * of type 2 (long) and 3 (short). This should not do any harm to the local 
 * dependency of the splitting (see Lemma 2.4).
 * <li>Landmarks used as in [1] to find split points while the position pos of the
 * rightmost split point is <tt>pos < length(s)-4</tt> (s is an array of non-rep
 * integers). The rest of the array is split according to rules of 
 * repeating metablocks. How could you - otherwise - split a string with landmarks
 * X0X0X (for example: "babab"), where X are landmarks...?
 * </ul>
 * </p>
 *  
 * <p>
 * [1] Graham Cormode, S. Muthukrishnan. "The String Edit Distance Matching
 * Problem with Moves". SIAM Symposium on Descrete Algorithms, 2002.</p>
 * 
 * @author augsten@inf.unibz.it
 * 
 */
class Splitter {
	
	/**
	 * Size of alphabet after first reduction.
	 */
	public static final int REDUCED_ALPHABET_SIZE = 6;
	/**
	 * Print debug info, if true.
	 */
	public static boolean DEBUG = false;

	/**
	 * As {@link #split(int[])}, only that a String is given instead 
	 * of an integer array.
	 * 
	 * @param str string to be split
	 * @return starting indices of blocks. First element is always 0 (starting
	 * index of first block. Size of the array is equal to the number of blocks. 
	 * Returns null, if size of array is less then 2.
	 *
	 */
	public static int[] split(String str) {
		int[] s = new int[str.length()];
		for (int i = 0; i < str.length(); i++) {
			s[i] = str.charAt(i);
		}
		return split(s);
	}
	
	/**
	 * Split an array of integers in blocks of length 2 or 3. 
	 * 
	 * @param s array of intergers to be split
	 * @return starting indices of blocks. First element is always 0 (starting
	 * index of first block. Size of the array is equal to the number of blocks. 
	 * Returns null, if size of array is less then 2.
	 *  
	 */
	public static int[] split(int[] s) {
		// I can not split strings smaller than 2 chars
		if (s.length < 2) {
			return null;
		}
		
		// vector will hold starting indices of the blocks as Integer objects
		// The number of blocks N is |s|/3 <= N <= |s|/2, where |s| 
		// is the length of array s.
		Vector splits = new Vector(s.length/2);
		
		// parse string for single characters between repeating char blocks
		boolean[] rep = getRepBlocks(s);
		debugPrintArray("rep", rep);		
		boolean[] sizeOne = getSizeOneBlocks(rep);
		debugPrintArray("size1", sizeOne);
		
		// parse string and treat repeating and non-repeating blocks separately
		int blockstart = 0;   // pointer to start of current block
		for (int i = 0; i < s.length; i++) {			
			if (rep[i]) {
				while ((i < s.length) && rep[i]) {
					i++;
				}
				// if the next character is a size-one-block, include it to the non-repeating block
				if ((i < s.length) && (sizeOne[i])) {
					i++;
				}
				// split block with repeating characters
				splitRep(splits, s, blockstart, i - 1);
				// new block starts after last repeating character
				blockstart = i;
			} else {
				if (!sizeOne[i]) {
					while ((i < s.length) && !rep[i]) {
						i++;
					}
					splitNonRep(splits, s, blockstart, i - 1);
					blockstart = i;
				}
			}
		}
		// if strings ends with a non-repeating block
		if (blockstart < s.length) {
			splitNonRep(splits, s, blockstart, s.length - 1);
		}
		// convert the vector to an array of integers
		int[] res = new int[splits.size()];
		for (int i = 0; i < res.length; i++) {
			res[i] = ((Integer)splits.elementAt(i)).intValue();
		}
		return res; 
	}

	/**
	 * Find blocks of repeating integers.
	 * 
	 * @param s input array (containing repeating and/or non-repeating blocks) 
	 * @return array of size s.length, true at posistion i, if s[i] == s[i-1] or s[i] == s[i + 1]
	 */
	private static boolean[] getRepBlocks(int[] s) {
		boolean[] rep = new boolean[s.length];
		for (int i = 1; i < s.length; i++) {
			rep[i - 1] = rep[i - 1] || (s[i - 1] == s[i]);
			rep[i] = s[i - 1] == s[i];
		}
		return rep;
	}
	
	/**
	 * Find all blocks at the beginning, at the end, or in the middle of 
	 * the input array s (in the latter case between two repeating sequences 
	 * of characters) that are of length 1. 
	 * @param rep input array (containing repeating and/or non-repeating blocks) 
	 * @return array of size s.length, true if char at position i is a sizeOneBlock, false otherwise
	 */
	private static boolean[] getSizeOneBlocks(boolean[] rep) {
		boolean[] sizeOne = new boolean[rep.length];
		for (int i = 0; i < rep.length; i++) {
			boolean prev = true;
			boolean next = true;
			if (i > 0) {
				prev = rep[i - 1];
			}
			if (i < rep.length - 1) {
				next = rep[i + 1];
			}
			sizeOne[i] = prev && next && !rep[i];
		}
		return sizeOne;
	}
	
	/**
	 * Split part of an array of integers (known to be non-repeating) into blocks of 
	 * length 2 or 3. 
	 * 
	 * @param splits add split points to this vector as Integer objects
	 * @param s array of integers to split
	 * @param blockstart start position of non-repeating block within s
	 * @param blockend end position of non-repeating block within s
	 */	
	private static void splitNonRep(Vector splits, int[] s, int blockstart, int blockend) {
		int blocklength = blockend - blockstart + 1;
		if (blocklength <= 3) {
			splits.add(new Integer(blockstart));
		} else if (blocklength == 4) {
			splits.add(new Integer(blockstart));
			splits.add(new Integer(blockstart + 2));			
		} else {
			debugPrintArray("Original Substring", s, blockstart, blockend);
		
			// reduce the alphabet size by shifting the alphabet, so that it starts with 0
			int[] sub = shiftAlphabet(s, blockstart, blockend);
			debugPrintArray("Alphabet shift", sub);
			
			// reduce alphabet to {0, 1, 2, 3, 4, 5}
			while (getAlphabetSize(sub) > REDUCED_ALPHABET_SIZE) {
				sub = reduceAlphabet(sub);
			}
			debugPrintArray("Pre-reduction", sub);
			
			// reduce alphabet to {0, 1, 2}
			sub = finalReduceAlphabet(sub);
			debugPrintArray("Final reduction", sub);
			
			// get landmarks
			boolean[] landmarks = getLandmarks(sub);
			debugPrintArray("Landmarks", landmarks);
			
			// split based on landmarks
			int pos = splitOnLandmarks(splits, s, blockstart, landmarks);
			
			// the remaining string is at most 4 chars long...
			splitNonRep(splits, s, pos, blockend);
		}

	}
	
	/**
	 * Split string s based on landmarks using patterns. Return position after last 
	 * block that was found. This block is always of length 2, 3 or 4.  
	 * 
	 * @param splits
	 * @param s array of length > 4
	 * @param blockstart
	 * @param landmarks
	 * @return start position of last block of size 2, 3 or 4
	 */
	private static int splitOnLandmarks(Vector splits, int[] s, int blockstart, boolean[] landmarks) {
		int i = 0;
		while (i + 4 < landmarks.length) {
			boolean l0 = landmarks[i];
			boolean l1 = landmarks[i + 1];
			boolean l2 = landmarks[i + 2];
			boolean l3 = landmarks[i + 3];
			if (!l0 && !l1 && l2) {
				splits.add(new Integer(blockstart + i));
				i += 3;
			} else if (!l0 && l1 && !l2 && !l3) {
				splits.add(new Integer(blockstart + i));
				i += 3;
			} else if (!l0 && l1 && !l2 && l3) {
				splits.add(new Integer(blockstart + i));
				i += 2;
			}			
		}
		return i + blockstart;
	}

	/**
	 * Find the landmarks in an array with alphabet {0, 1, 2}.
	 * @param s array consisting of {0, 1, 2}, where s[i - 1] != s[i] != s[i + 1]
	 * @return array of same length as s, true at position i, if s[i] is a landmark, 
	 * false otherwise
	 */
	private static boolean[] getLandmarks(int[] s) {
		boolean[] lm = new boolean[s.length];
		
		// set a landmark for each maximum
		for (int i = 0; i < s.length; i++) {
			int prev = Integer.MAX_VALUE; // no landmark at first index of string
			if (i != 0) {
				prev = s[i - 1];
			}
			int next = Integer.MIN_VALUE; 
			if (i != s.length - 1) {
				next = s[i + 1];
			}
			lm[i] = ((s[i] > prev) && (s[i] > next));
		}
		// set a landmark for each minimum that is not 
		//   adjacent to an existing landmark
		for (int i = 0; i < s.length; i++) {
			int prev = Integer.MIN_VALUE; // no landmark at first index of string 
			boolean leftLm = false;
			if (i != 0) {
				prev = s[i - 1];
				leftLm = lm[i - 1];
			}
			int next = Integer.MAX_VALUE;
			boolean rightLm = false;
			if (i != s.length - 1) {
				next = s[i + 1];
				rightLm = lm[i + 1];
			}
			if ((s[i] < prev) && (s[i] < next) && !leftLm && !rightLm) {
				lm[i] = true;
			}
		}				
		return lm;
	}
	
	/**
	 * Show values of s, if {@link #DEBUG} is true.
	 * 
	 * @param name description of s 
	 * @param s show values of this array
	 */
	private static void debugPrintArray(String name, int[] s) {
		debugPrintArray(name, s, 0, s.length - 1);
	}
	
	/**
	 * Show values of s between indices blockstart and blockend, 
	 * if {@link #DEBUG} is true.
	 * 
	 * @param name description of s 
	 * @param s show values of this array
	 * @param blockstart index of first field to show
	 * @param blockend index of last field to show
	 */
	private static void debugPrintArray(String name, int[] s, int blockstart, int blockend) {
		if (DEBUG) {
			System.out.print(name + ": ");
			for (int i = blockstart; i <= blockend; i++) {
				System.out.print(s[i] + " ");
			}
			System.out.println();
		}
	}
	
	/**
	 * Show values of s between indices blockstart and blockend, 
	 * if {@link #DEBUG} is true.
	 * 
	 * @param name description of s 
	 * @param s show values of this array
	 */
	private static void debugPrintArray(String name, boolean[] s) {
		if (DEBUG) {
			System.out.print(name + ": ");
			for (int i = 0; i < s.length; i++) {
				if (s[i]) {
					System.out.print("X"); 
				} else {
					System.out.print("0");
				}
			}
			System.out.println();
		}
	}


	/**
	 * Shift the alphabet, such that it is 0...alphabetsize - 1. Array is changed!
	 * 
	 * @param s array of integers
	 * @param blockstart first index of block in s
	 * @param blockend last index of block in s
	 * @return new array of length blockend - blockstart + 1 with the shifted alphabet
	 */	
	private static int[] shiftAlphabet(int[] s, int blockstart, int blockend) {
		int min = Integer.MAX_VALUE; 
		for (int i = blockstart; i <= blockend; i++) {
			if (s[i] < min) {
				min = s[i];
			}
		}
		int[] r = new int[blockend - blockstart + 1];
		for (int i = 0; i < r.length; i++) {
			r[i] = s[i + blockstart] - min;
		}
		return r;		
	}
	
	/**
	 * Get the maximum number of labels in the alphabet.
	 * @param s get the maximum number of labels in s
	 * @return maximum label in s + 1 (alphabet is assumed to start with 0)
	 */
	private static int getAlphabetSize(int[] s) {
		int max = Integer.MIN_VALUE; 
		for (int i = 0; i < s.length; i++) {
			if (s[i] > max) {
				max = s[i];
			}
		}
		return max + 1;				
	}
	
	/**
	 * Alphabet reduction according to [1], 2.1.1. Only difference: We use 
	 * an imaginary first character in each step, so we get also the first element 
	 * of the array relabeled. 
	 * 
	 * @param s array with no adjacent chars being identical over alphabet A
	 * @return array of same size with alphabet reduced to A' = 2 * log(|A|)
	 */
	private static int[] reduceAlphabet(int[] s) {
		int[] r = new int[s.length];
		// use imaginary label (s[0] + 1) to the left of s[0] to compute new label for s[0]
		r[0] = getLabel(s[0] + 1, s[0]);
		for (int i = 1; i < r.length; i++) {
			r[i] = getLabel(s[i - 1], s[i]);		
		}
		return r;
	}
	
	/**
	 * Create new label from two labels in order to reduce alphabet size.
	 * Precondiction: first != second.
	 * 
	 * @param first first label
	 * @param second second label
	 * @return new label
	 */
	private static int getLabel(int first, int second) {
		int L = 0;
		while ((((first ^ second) >> L) & 1) != 1) {
			L++;
		}		
		int bit = (second & (1 << L)) >> L;
		return 2 * L + bit; 
	}
	
	/**
	 * Reduce alphabet {0,1,2,3,4,5} of array s (no adjacent chars are identical) 
	 * to new array with alphabet {0,1,2} (no adjacent chars are identical).
	 * According to [1], 2.1.1
	 * @param s array over alphabet {0,1,2,3,4,5} with no adjacent chars identical
	 * @return array of same length as s with all chars {3,4,5} changed to a 
	 * char in {0,1,2} with no adjacent chars identical 
	 */
	private static int[] finalReduceAlphabet(int[] s) {
		int[] r = new int[s.length];
		// go through array
		for (int i = 0; i < s.length; i++) {
			r[i] = s[i];
			// change s[i] if it is in {3,4,5}
			for (int j = 3; j <= 5; j++) {    
				if (r[i] == j) {					
					// assign to s[i] the smallest of {0,1,2} that is not a neighbor of s[i]
					for (int k = 0; k <= 2; k++) {  
						int prev = -1;
						int next = -1;
						if (i != 0) {
							prev = r[i - 1]; // r[i - 1] already computed 
						}
						if (i != s.length - 1) {
							next = s[i + 1]; // r[i + 1] undefined!
						}
						if ((prev != k) && (next != k)) {
							r[i] = k;
							break;
						}
					}
				}
			}
		}
		return r;		
	}

	/**
	 * <p>Splits a the substring of s, starting with position blockstart and ending
	 * with position blockend into blocks of length 2 or 3. Blocks of length 2 
	 * or 3 are not splited further. Block of length 4 are splitted into two parts 
	 * of length 2. Larger blocks are splited into a block of length 3 and the rest
	 * is split recursively.</p>
	 * 
	 * <p> This method works for all strings with substrings longer than 2, independently on 
	 * whether they have repeating characters or not.</p>
	 * 	  
	 * <p><it>Note:</it> Implemented as recursive function.</p> 
	 *  
	 * @param splits add split points to this vector as Integer objects
	 * @param s string to split
	 * @param blockstart start position of repeating block within s
	 * @param blockend end position of repeating block within s
	 */
	private static void splitRep(Vector splits, int[] s, int blockstart, int blockend) {
		// in any case: here begins a new block!
		splits.add(new Integer(blockstart));
		
		int blocksize = blockend - blockstart + 1;

		if (blocksize <= 3) { 			// no other blocks
		} else if (blocksize == 4) {    // blocks of 4 are split in the middle
			splits.add(new Integer(blockstart + 2));
		} else {    // longer blocks are processed recursively
			splitRep(splits, s, blockstart + 3, blockend);
		}
	}
	
}

