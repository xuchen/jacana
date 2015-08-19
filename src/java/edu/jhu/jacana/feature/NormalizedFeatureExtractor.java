/**
 * 
 */
package edu.jhu.jacana.feature;

import java.util.HashMap;

import edu.jhu.jacana.util.StringUtils;

import approxlib.distance.EditDist;

/**
 * @author Xuchen Yao
 *
 */
public abstract class NormalizedFeatureExtractor implements FeatureExtractor {
	
	boolean normalized;
	
	protected String[] features;
	
	protected HashMap<String, Boolean> switches;
	
	public NormalizedFeatureExtractor() {this.normalized = true;}
	
	// should've not had this constructor so subclasses would HAVE TO
	// call the next constructor to MAKE SURE that <code>features</code>
	// and <code>switches</code> are properly initialized. BUT, since a
	// subclass constructor has to call super() first, initializing
	// these (usually large) fields in super() INLINE is a pain to read.
	// so we keep the following constructor and "reply" on the coder to
	// initialize <code>features</code> and <code>switches</code>.
	public NormalizedFeatureExtractor(boolean normalized) {this.normalized = normalized;}
	
	public NormalizedFeatureExtractor(boolean normalized, String[] features, HashMap<String, Boolean> switches) {
		this(normalized);
		this.features = features;
		this.switches = switches;
	}

	/* (non-Javadoc)
	 * @see edu.jhu.jacana.feature.FeatureExtractor#arffAttributeHeader()
	 */
	@Override
	public String arffAttributeHeader() {
		StringBuilder sb = new StringBuilder();
		for (String f:features) {
			if (switches!=null && !switches.get(f)) continue;
			sb.append("@ATTRIBUTE ");
			sb.append(f);
			sb.append(" NUMERIC\n");
		}
		return sb.toString();
	}
	
	/* (non-Javadoc)
	 * @see edu.jhu.jacana.feature.FeatureExtractor#getAttributes()
	 */
	public String[] getAttributes() {
		return features;
	}
	
	/* (non-Javadoc)
	 * @see edu.jhu.jacana.feature.FeatureExtractor#getFeatureValues(approxlib.distance.EditDist)
	 */
	public abstract Double[] getFeatureValues(EditDist dist);

	/* (non-Javadoc)
	 * @see edu.jhu.jacana.feature.FeatureExtractor#arffData(approxlib.distance.EditDist)
	 */
	@Override
	public String arffData(EditDist dist) {
		return StringUtils.<Double>join(getFeatureValues(dist), ",")+",";
	}

}
