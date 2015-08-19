package approxlib.util;

import java.util.Arrays;

/**
 * Growing array of integers. 
 * 
 * The initial capacity is also the length by which the array grows 
 * when it needs to be increased. The first element is at index position 0. 
 * The last element in the array is
 * the element at the largest index position that was explicitily stored 
 * with {@link #setElementAt(int, int)}. The size is the index position of
 * the last element plus 1. The default value of an element is zero.
 */
public class DynIntArray {
	private int data[];
	private int size, step;

	/**
	 * Initialize array with the default initial capacity 
	 * of 1024 integers.
	 *
	 */
	public DynIntArray(){
		this(1024);
	}

	/**  
	 * Initialize array with a given initial capacity.
	 * 
	 * @param capacity initial capacity of the array and size by which it will grow
	 */
	public DynIntArray(int capacity){
		this.size = 0;
		this.step = capacity;
		this.data = new int[capacity];
	}

	/**
	 * Change the value of the element at a given index in the array. 
	 * Like in a usual array, the first element has index 0. 
	 * 
	 * <p><code>array[index]=value<code>
	 * 
	 * @param index position of the element in the array
	 * @param value value
	 */
	public void setElementAt(int index, int value){
		ensureCapacity(index);  
		try{
			data[index] = value;
		}
		catch(Exception e){
			System.out.println("\nsetElementAt("+index+") -> "+data.length);           
		}
		if (index >= size) {
			size = index + 1;
		}
	}

	/**
	 * Make sure that data[index] exists. If it does not exist, increase
	 * data[] by step until data[index] exists.
	 * 
	 * @param index
	 */
	private void ensureCapacity(int index){
		if (index >= data.length){
			int diff = Math.max(step, index - data.length + 1);
			data = Arrays.copyOf(data, data.length + diff);
		}
	}

	/**
	 * Return a string that contains all elements in the array.
	 * 
	 * @return string containing all elements of the array
	 */
	@Override
	public String toString(){
		String result = "[";
		for (int i = 0; i < size - 1; i++) {
			result += data[i] +",";			
		}
		if (size > 0) {
			result += data[size-1];
		}
		result += "]";
		return result;
	}	

	/**
	 * Returns the value at the given index position in the array.  
	 * Like in a usual array, the first element has index 0. 
	 * 
	 * <p><code>array[index]=value<code>
	 * 
	 * @param index 
	 * @return
	 * @throws ArrayIndexOutOfBoundsException if the index points outside the array 
	 */
	public int get(int index) {
		if ((index < 0) || (index > size - 1)) {
			throw new ArrayIndexOutOfBoundsException("Index out of bounds: " 
					+ index + " in array of size: " + size);
		}
		return data[index];	
	}

	/**
	 * Get a copy of this array with {@link #size()} elements.
	 * 
	 * @return copy of this array
	 */
	public int[] intArray(){
		return Arrays.copyOf(data, size);
	}

	/**
	 * Get the size of the array. 
	 * This is equal to the next free array position, i.e., the position of the 
	 * last element in the array that was explicitly set plus 1.
	 * 
	 * @return size of the array
	 */
	public int size(){
		return size;
	}

	/**
	 * Length by which the array is increased if it needs to grow.
	 * 
	 * @return the step
	 */
	public int getStep() {
		return step;
	}

	/**
	 * Ste the length by which the array is increased if it needs to grow.
	 * The default is the initial capacity.
	 *	
	 * @param step the step to set
	 */
	public void setStep(int step) {
		this.step = step;
	}

	/**
	 * Reference to the array that is internally used. 
	 * Only for test purpose!
	 * 
	 * @return the data
	 */
	public int[] getData() {
		return data;
	}	
}

