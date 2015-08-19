/**
 * 
 */
package edu.jhu.jacana.experiment;

import java.util.List;
import java.util.AbstractMap.SimpleImmutableEntry;

import weka.classifiers.Classifier;
import weka.classifiers.functions.Logistic;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

import approxlib.distance.EditDist;
import approxlib.tree.LblTree;
import edu.jhu.jacana.dependency.DependencyTree;
import edu.jhu.jacana.feature.CountingFeature;
import edu.jhu.jacana.feature.DeleteFeature;
import edu.jhu.jacana.feature.DistFeature;
import edu.jhu.jacana.feature.FeatureExtractor;
import edu.jhu.jacana.feature.InsertFeature;
import edu.jhu.jacana.feature.RenamePosFeature;
import edu.jhu.jacana.feature.RenameRelFeature;
import edu.jhu.jacana.feature.UneditedFeature;
import edu.jhu.jacana.reader.MstReader;
import edu.jhu.jacana.util.KBest;

/**
 * Given a file in MST format, this class computes the pair-wise
 * likelihood of similarity given a (logistic) model and outputs 
 * top 10 most similar sentences for each sentence.
 * @author Xuchen Yao
 *
 */
public class SimRetrievalWithModel {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		List<DependencyTree> trees;
		// model file
		Classifier c = (Classifier) weka.core.SerializationHelper.read(args[0]);
		// test data in MST format
		trees = MstReader.read(args[1]);
		
		int total = trees.size();
		double[][] matrix = new double[total][total];
		
		boolean normalized = false;
		FeatureExtractor[] extractors = {new CountingFeature(normalized), 
				new DistFeature(),
				new RenamePosFeature(normalized),
				new InsertFeature(normalized),
				new DeleteFeature(normalized),
				new RenameRelFeature(normalized),
				new UneditedFeature(normalized),
		};

		// creating attributes in memory, equivalent to specifying ARFF header
		FastVector attributes = new FastVector();
		for (FeatureExtractor ex:extractors)
			for (String attr:ex.getAttributes())
				attributes.addElement(new Attribute(attr));
		FastVector labels = new FastVector();
		labels.addElement("positive");
		labels.addElement("negative");
		Attribute cls = new Attribute("class", labels);
		attributes.addElement(cls);
		Instances dataset = new Instances("Pairwise Similarity", attributes, 0);
		int size = dataset.numAttributes();
		
		// Make the last attribute be the class
		dataset.setClassIndex(size-1); 
		
		for (int i=0; i<total-1; i++) {
			matrix[i][i] = 0;
			for (int j=i+1; j<total; j++) {
				//System.err.println(String.format("%d\t%d", i+1, j+1));
				DependencyTree questionTree = trees.get(i);
				DependencyTree answerTree = trees.get(j);

				String qString = questionTree.toCompactString();
				String aString = answerTree.toCompactString();
				LblTree qTree = LblTree.fromString(qString);
				LblTree aTree = LblTree.fromString(aString);

				EditDist dis2 = new EditDist(true);
				dis2.treeDist(aTree, qTree);
				dis2.printHumaneEditScript();
				int pointer = 0;
				double[] values = new double[size];
				for (FeatureExtractor ex:extractors) {
					for(double d:ex.getFeatureValues(dis2)) {
						values[pointer++] = d;
					}
				}
				
				Instance inst = new Instance(1.0, values);
				dataset.add(inst);
				
				double[] dist = c.distributionForInstance(inst);
				// probability for positive class
				matrix[i][j] = matrix[j][i] = dist[0];
			}
		}
		matrix[total-1][total-1] = 0;

		int top = Math.min(10, total);
		for (int i=0; i<total; i++) {
			KBest<Integer> kbest = new KBest<Integer>(top, true, true);
			for (int j=0; j<total; j++) {
				if (i == j) continue;
				kbest.insert(j, matrix[i][j]);
			}
			SimpleImmutableEntry<Integer,Double>[] results = kbest.toArray();
			System.out.println("========================");
			System.out.println("["+i+"]\t"+trees.get(i).getSentence());
			System.out.println("========================");
			for (int j = 0; j < results.length; j++)
				System.out.println(String.format("[%d]\t%.4f\t%s", results[j].getKey(),
						results[j].getValue(), trees.get(results[j].getKey()).getSentence()));
		}
	}

}
