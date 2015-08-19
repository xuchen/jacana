/**
 * from http://www.java2s.com/Code/Java/Collections-Data-Structure/Demonstratesheaps.htm
 */
package approxlib.util;

import java.util.Arrays;

/**
 * Max-Heap.
 * 
 * @author augsten 
 */
public class Heap {

	private Comparable[] heapArray;

	private int maxSize; // size of array

	private int size; // number of nodes in array

	/**
	 * Create new max-heap.
	 * @param maxSize
	 */
	public Heap(int maxSize) {
		this.maxSize = maxSize;
		this.size = 0;
		this.heapArray = new Comparable[maxSize]; // create array
	}

	/**
	 * @return true iff the heap is empty
	 */
	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * @return true iff the heap is full, i.e., getSize() == getMaxSize()
	 */
	public boolean isFull() {
		return this.size == this.maxSize;
	}

	/**
	 * Insert a new element into the heap.
	 * 
	 * @param key
	 * @return true iff insert was successful (i.e., space was left on the heap)
	 */
	public boolean insert(Comparable key) {
		if (size == maxSize) {
			return false;
		}
		heapArray[size] = key;
		trickleUp(size++);
		return true;
	} 

	/**
	 * Remove the max-element from the heap and insert a new element instead.
	 * Assumes non-empty heap.
	 * 
	 * @param key
	 * @return the max-element that was removed
	 */
	public Comparable substitute(Comparable key) { 
		Comparable root = this.heapArray[0]; 
		heapArray[0] = key;
		if (key.compareTo(root) < 0) {
			trickleDown(0);
		}
		return root;
	} 

	/**
	 * Remove maximum element from the heap.
	 * Assumes non-empty heap.
	 * @return maximum element on the heap
	 */
	public Comparable remove() { 
		Comparable root = heapArray[0];
		heapArray[0] = heapArray[--size];
		trickleDown(0);
		return root;
	} 

	/**
	 * Return maximum element without removing it from the heap.
	 * @return maximum element on the heap
	 */
	public Comparable peek() 
	{ // (assumes non-empty list)
		return heapArray[0];
	} // end peek()

	private void trickleDown(int index) {
		int largerChild;
		Comparable top = heapArray[index]; // save root
		while (index < size / 2) // while node has at
		{ //    least one child,
			int leftChild = 2 * index + 1;
			int rightChild = leftChild + 1;
			// find larger child
			if (rightChild < size
					&& // (rightChild exists?)
					heapArray[leftChild].compareTo(heapArray[rightChild]) < 0) {
				largerChild = rightChild;
			} else {
				largerChild = leftChild;
			}
			// top >= largerChild?
			if (top.compareTo(heapArray[largerChild]) >= 0) {
				break;
			}
			// shift child up
			heapArray[index] = heapArray[largerChild];
			index = largerChild; // go down
		}
		heapArray[index] = top; // root to index
	}

	/**
	 * 
	 * @param index
	 */
	private void trickleUp(int index) {
		int parent = (index - 1) / 2;
		Comparable bottom = heapArray[index];

		while (index > 0 && heapArray[parent].compareTo(bottom) < 0) {
			heapArray[index] = heapArray[parent]; // move it down
			index = parent;
			parent = (parent - 1) / 2;
		}
		heapArray[index] = bottom;
	}
	
	public void displayHeap() {
		System.out.print("heapArray: "); // array format
		for (int m = 0; m < size; m++) {
			if (heapArray[m] != null) {
				System.out.print(heapArray[m] + " \n");
			} else {
				System.out.print("-- ");
			}
		}
		int nBlanks = 32;
		int itemsPerRow = 1;
		int column = 0;
		int j = 0; // current item

		while (size > 0) // for each heap item
		{
			if (column == 0) {
				for (int k = 0; k < nBlanks; k++) {
					// preceding blanks
					System.out.print(' ');
				}
			}
			// display item
			System.out.print(heapArray[j]);

			if (++j == size) {
				break;
			}

			if (++column == itemsPerRow) // end of row?
			{
				nBlanks /= 2; // half the blanks
				itemsPerRow *= 2; // twice the items
				column = 0; // start over on
				System.out.println(); //    new row
			} else {
				// next item on row
				for (int k = 0; k < nBlanks * 2 - 2; k++) {
					System.out.print(' '); // interim blanks
				}
			}
		} 
	}
	
	/**
	/*<p>
	 * Copy all heap elements to a given array. The type of the array must
	 * be able to contain all heap elements. 
	 * The input array must be of the same size as the heap.  
	 * The items are sorted decrementally (largest item first).
	 *
	 * <p>
	 * Example: 
	 * <code>(NodeDistPair[])topK.getSortedArray(new NodeDistPair[topK.getSize()])</code> 
	 *  
	 * @param topk array of the same size as the heap, same type as heap elements
	 * @return decrementally sorted array of all heap elements
	 */
	public Comparable[] getSortedArray() {
		Comparable[] topk = Arrays.copyOf(this.heapArray, this.getSize());
		Arrays.sort(topk);
		return topk;		
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		Comparable[] a = this.getSortedArray();
		StringBuffer sb = new StringBuffer("[");
		for (int i = 0; i < a.length; i++) {
			sb.append(a[i]);
			if (i < a.length - 1) sb.append(",");
		}
		sb.append("]");
		return sb.toString();
	}

	/**
	 * @return current number of elements on the heap
	 */
	public int getSize() {
		return size;
	}

	public Comparable[] getHeapArray() {
		return heapArray;
	}

	/**
	 * @return maximum number of elements that this heap can store
	 */
	public int getMaxSize() {
		return maxSize;
	}

	/**
	 * Merges a given heap with this heap and empty it.
	 * If the given heap is null, nothing is done.
	 * @param heap
	 */
	public void merge(Heap heap) {
		if (heap == null) return;
		while (!heap.isEmpty()) {
			Comparable el = heap.remove();
			if (!this.isFull()) {
				this.insert(el);
			} else if (el.compareTo(this.peek()) < 0) {
				this.substitute(el);
			}
		}
	}
			
}
