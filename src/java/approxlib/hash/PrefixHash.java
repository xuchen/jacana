/*
 * Created on Jun 22, 2005
 */
package approxlib.hash;

/**
 * Hash function that returns a prefix of the required length of the hashed string.
 * 

 * @author augsten
 */
public class PrefixHash extends FixedLengthHash {

	public final static char PADDING_CHAR = '.';
	
	/**
	 * @param length length of the computed hash values 
	 */
	public PrefixHash(int length) {
		super(length);
	}

	/**
	 * The hashed value of a string <code>s</code> is a string with fixed length. If <code>s</code> is 
	 * longer then the specified length, it is cut to the proper length, otherwise it is patched with zero 
	 * characters.
	 * 
	 * @param s string to be hashed
	 * @return hash value of the correct length 	
	 */ 
	 @Override
	public HashValue getHashValue(String s) {
		if (s.length() > this.getLength()) {
			s = s.substring(0, this.getLength());
		} else {
			while (s.length() < this.getLength()) {
				s += PADDING_CHAR;
			}
		}
		return new HashValue(s);
	}	
	 
	@Override
	public HashValue getNullNode() {
		return getHashValue("*");
	}
	

}
