/**
 * 
 */
package edu.jhu.jacana.experiment;

import java.util.List;
import java.util.AbstractMap.SimpleImmutableEntry;

import approxlib.distance.EditDist;
import approxlib.tree.LblTree;
import edu.jhu.jacana.dependency.DependencyTree;
import edu.jhu.jacana.reader.MstReader;
import edu.jhu.jacana.util.KBest;

/**
 * Given a file in MST format, this class computes the pair-wise
 * tree-edit distance and outputs top 10 most similar sentences
 * for each sentence 
 * @author Xuchen Yao
 *
 */
public class SimRetrieval {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		List<DependencyTree> trees;
		if (args.length == 1)
			trees = MstReader.read(args[0]);
		else
			trees = MstReader.read("tree-edit-data/answerSelectionExperiments/training/1-100/test1");
		int total = trees.size();
		double[][] matrix = new double[total][total];

		for (int i=0; i<total-1; i++) {
			matrix[i][i] = 0;
			for (int j=i+1; j<total; j++) {
				DependencyTree questionTree = trees.get(i);
				DependencyTree answerTree = trees.get(j);
				//			String qString = questionTree.stringInRTEDtree();
				//			String aString = answerTree.stringInRTEDtree();
				String qString = questionTree.toCompactString();
				String aString = answerTree.toCompactString();
				//				System.out.println(qString);
				//				System.out.println(aString);
				LblTree qTree = LblTree.fromString(qString);
				LblTree aTree = LblTree.fromString(aString);

				//				qTree.prettyPrint();
				//				aTree.prettyPrint();

				EditDist dis2 = new EditDist(true);
				double dist = dis2.treeDist(aTree, qTree);			
				matrix[i][j] = matrix[j][i] = dist;
				//System.out.println(dist);
				//				dis2.printForestDist();
				//				dis2.printTreeDist();
				//				dis2.printBackPointer();
				//				dis2.printEditMatrix();
				//				System.out.println(dis2.printEditScript());
				//				System.out.println(dis2.printHumaneEditScript());
				//				System.out.println(dis2.printCompactEditScript());
			}
		}
		matrix[total-1][total-1] = 0;

		int top = Math.min(10, total);
		for (int i=0; i<total; i++) {
			KBest<Integer> kbest = new KBest<Integer>(top, false, true);
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
