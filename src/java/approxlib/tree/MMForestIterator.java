/**
 * 
 */
package approxlib.tree;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author naugsten
 *
 */
public class MMForestIterator implements Iterator<LblValTree> {

	int pos;
	MMForest f;
	
	public MMForestIterator(MMForest f) {
		this.f = f;
		pos = -1;
	}
	
	/* (non-Javadoc)
	 * @see java.util.Iterator#hasNext()
	 */
	public boolean hasNext() {
		return pos < f.size() - 1;
	}

	/* (non-Javadoc)
	 * @see java.util.Iterator#next()
	 */
	public LblValTree next() {
		if (this.hasNext()) {
			return f.elementAt(++pos);
		}
		throw new NoSuchElementException();		
	}

	/* (non-Javadoc)
	 * @see java.util.Iterator#remove()
	 */
	public void remove() {				
		throw new UnsupportedOperationException("The remove() operation is not supported by " + this.getClass());		
	}

}
