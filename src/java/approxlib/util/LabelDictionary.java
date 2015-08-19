package approxlib.util;

import java.util.Map;
import java.util.Hashtable;

/**
 * This provides a way of using small int values to represent String labels, 
 * as opposed to storing the labels directly.
 * 
 * @author Denilson Barbosa, Nikolaus Augsten
 */
public class LabelDictionary {
	public static final int KEY_DUMMY_LABEL = -1;
	private int count;
	private Map<String,Integer> StrInt;
	private Map<Integer,String> IntStr;
	private boolean newLabelsAllowed = true;

	/**
	 * Creates a new blank dictionary.
	 *  
	 * @throws Exception
	 */
	public LabelDictionary() {
		count = 0;
		StrInt = new Hashtable<String,Integer>();
		IntStr = new Hashtable<Integer,String>();
	}
	
	
	/**
	 * Adds a new label to the dictionary if it has not been added yet. 
	 * Returns the ID of the new label in the dictionary.
	 * 
	 * @param label add this label to the dictionary if it does not exist yet
	 * @return ID of label in the dictionary
	 */	
	public int store(String label){
		if (StrInt.containsKey(label)) {
			return (StrInt.get(label).intValue());
		} else if (!newLabelsAllowed) { 
			return KEY_DUMMY_LABEL;
		} else { // store label
		Integer intKey = new Integer(count++);
		StrInt.put(label, intKey);
		IntStr.put(intKey, label);
		
		return intKey.intValue();		
	}
	}
	
	/**
	 * Returns the label with a given ID in the dictionary.
	 *	
	 * @param labelID 
	 * @return the label with the specified labelID, or null if this dictionary contains no label for labelID
	 */
	public String read(int labelID){
		return IntStr.get(new Integer(labelID));		
	}
	
	/**
	 * @return true iff new labels can be stored into this label dictinoary
	 */
	public boolean isNewLabelsAllowed() {
		return newLabelsAllowed;
}

	/**
	 * @param newLabelsAllowed the newLabelsAllowed to set
	 */
	public void setNewLabelsAllowed(boolean newLabelsAllowed) {
		this.newLabelsAllowed = newLabelsAllowed;
	}
	
}
