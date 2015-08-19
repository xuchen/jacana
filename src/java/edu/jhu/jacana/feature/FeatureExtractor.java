/**
 * 
 */
package edu.jhu.jacana.feature;

import approxlib.distance.EditDist;

/**
 * @author Xuchen Yao
 *
 */
public interface FeatureExtractor {
	
	/**
	 * return the header portion of the ARFF file for this feature, e.g.,

@ATTRIBUTE renRel NUMERIC
@ATTRIBUTE renPos NUMERIC
@ATTRIBUTE renPosRel NUMERIC
	 * @return the header string
	 */
	public String arffAttributeHeader();
	
	/**
	 * return a list of attributes this feature extractor generates
	 * @return
	 */
	public String[] getAttributes();
	
	/**
	 * return the body data portion of the ARFF file for this feature, e.g,
	 * "5.0,3.0,2.0,". This function should call <code>getFeatureValues</code>.
	 * @param dist an edit distance object
	 * @return a data string consisting of feature values separated with ,
	 */
	public String arffData(EditDist dist);
	
	/**
	 * return the feature values in the order of <code>arffAttributeHeader</code>
	 * @param dist an edit distance object
	 * @return
	 */
	public Double[] getFeatureValues(EditDist dist);

}
