/*
 * Created on Jun 23, 2005
 */
package approxlib.hash;

/**
 * @author augsten
 */
public class StringHash extends FixedLengthHash {

	public final static String NULL_NODE = "*";

	public StringHash(int length) {
		super(length);
	}

	/* (non-Javadoc)
	 * @see hash.FixedLengthHash#getNullNode()
	 */
	@Override
	public HashValue getNullNode() {
		return getHashValue(NULL_NODE);
	}

	/**
	 * This produces a string, where each byte of the hash value corresponds to 
	 * a character in the string.
	 * 
	 * With the hash value h, and h[i] the i-th byte in the hash value 
	 * (i = 0..s, s = size of hash value in bytes; i = 0 is the least
	 * significant byte), string s[i] = (char)h[i];
	 * 
	 * @return string that represents hash value
	 */
	
	@Override
	public HashValue getHashValue(String s) {
		int length = this.getLength();
		long max = 1;
		for (int i = 0; i < length; i++) {
			max *= 256;
		}
		if (max == 0) {
			max = Long.MAX_VALUE;
		}
		int bits = length * 8 - 1;
		int n = s.length();
		long h = 0;
		long factor = 1;
		for (int i = n - 1; i >= 0; i--) {
			h = (h + s.charAt(i) * factor) % max;
			factor *= bits;
		}

		return new HashValue(h, length);	
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(length=" + this.getLength() + ")";
	}

}
