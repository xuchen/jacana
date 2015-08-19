/**
 * 
 */
package edu.jhu.jacana.feature;

import edu.jhu.jacana.util.StringUtils;
import approxlib.distance.EditDist;

/**
 * @author Xuchen Yao
 *
 */
public class DistFeature implements FeatureExtractor {

	/* (non-Javadoc)
	 * @see edu.jhu.jacana.feature.FeatureExtractor#arffAttributeHeader()
	 */
	@Override
	public String arffAttributeHeader() {

		return "@ATTRIBUTE dist  NUMERIC\n";
	}
	
	/* (non-Javadoc)
	 * @see edu.jhu.jacana.feature.FeatureExtractor#getAttributes()
	 */
	public String[] getAttributes() {
		return new String[]{"dist"};
	}

	/* (non-Javadoc)
	 * @see edu.jhu.jacana.feature.FeatureExtractor#getFeatureValues(approxlib.distance.EditDist)
	 */
	public Double[] getFeatureValues(EditDist dist) {
		return new Double[] {dist.getLastComputedDist()};
	}
	
	/* (non-Javadoc)
	 * @see edu.jhu.jacana.feature.FeatureExtractor#arffData(approxlib.distance.EditDist)
	 */
	@Override
	public String arffData(EditDist dist) {
		return StringUtils.join(getFeatureValues(dist), ",")+",";
	}

}
