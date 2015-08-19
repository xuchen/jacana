/**
 * 
 */
package edu.jhu.jacana.aura;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.IntegerEdgeNameProvider;
import org.jgrapht.ext.IntegerNameProvider;
import org.jgrapht.ext.StringNameProvider;
import org.jgrapht.graph.DefaultEdge;

import edu.jhu.jacana.util.FileManager;

/**
 * @author Xuchen Yao
 *
 */
public class AuraReader {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		/*
		total num of nodes (each node is a concept, the top node is Thing): 8123
		num of island (node with no parent, no child): 235
		num of nodes with multiple parents: 1181
		average num of parents for nodes with multiple parents: 2.07112616426757
		Depths, mean: 3.4, median: 3.0, max: 13.0
		 */
		AuraConceptManager.create();
		//System.out.println(manager);
		DOTExporter<AuraConcept, DefaultEdge> exporter = new DOTExporter<AuraConcept, DefaultEdge>(
				new IntegerNameProvider<AuraConcept>(), new StringNameProvider<AuraConcept>(), new IntegerEdgeNameProvider<DefaultEdge>()); 
		exporter.export(FileManager.getWriter("/tmp/aura.dot"), AuraConceptManager.graph);
		
		int numOfIsland = 0, numOfNonSingleParent = 0, numOfParents = 0;
		for (AuraConcept c:AuraConceptManager.graph.vertexSet()) {
			if (c.getNumOfDependents() == 0 && c.getNumOfGovernors() == 0)
				numOfIsland ++;
			if (c.getNumOfGovernors() > 1) {
				numOfParents += c.getNumOfGovernors();
				numOfNonSingleParent ++;
			}
		}
		System.out.println("total num of nodes: " + AuraConceptManager.graph.vertexSet().size());
		System.out.println("num of island: " + numOfIsland);
		System.out.println("num of nodes with multiple parents: " + numOfNonSingleParent);
		System.out.println("average num of parents for nodes with multiple parents: " + numOfParents*1.0/numOfNonSingleParent);
		
		AuraConceptManager.cleanup();
		
		List<Integer> depthList = new ArrayList<Integer>();
		for (AuraConcept c:AuraConceptManager.graph.vertexSet()) {
			int depth = AuraConceptManager.getDepthOfNode(c);
			if (depth != -1) {
				depthList.add(depth);
			}
		}
		Integer[] depths = depthList.toArray(new Integer[depthList.size()]);
		Arrays.sort(depths);
		int sum = 0;
		for (Integer d:depths) sum+=d;
		double mean, median;
		mean = sum*1.0/depths.length;
		median = depths[depths.length/2];
		System.out.println(String.format("Depths, mean: %.1f, median: %.1f, max: %.1f", 
				mean, median, (double)depths[depths.length-1]));
	}

}
