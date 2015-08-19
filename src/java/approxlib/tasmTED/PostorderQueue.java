package approxlib.tasmTED;

/**
 * A postorder queue accepts a sequence of tree nodes in postorder. A node is defined by its 
 * label and the size of the subtree that it roots. With this information the tree structure
 * is uniquely define. 
 */
public interface PostorderQueue {

	 /** 
	 * Append the next node (in postorder) to the queue, given the label of the node and the size
	 * of the subtree that it roots. A leaf node has subtree size 1. 
	 * 
	 * @param label label of the node to append
	 * @param subtreeSize size of the subtree that is rooted in the node to append
	 */
	public abstract void append(String label, int subtreeSize);

}