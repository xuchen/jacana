/**
 * 
 */
package edu.jhu.jacana.aura;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import edu.jhu.jacana.util.FileManager;

/**
 * manages all concepts and a mapping from phrases to concept.
 * @author Xuchen Yao
 *
 */
public class AuraConceptManager {
	
    private static final Logger logger = Logger.getLogger(AuraConceptManager.class.getName());
	
	// a mapping from phrase to concept
	static HashMap<String, AuraConcept> phrase2concept = new HashMap<String, AuraConcept>(); 


	// a mapping from the name of a concept to itself
	static HashMap<String, AuraConcept> name2concept = new HashMap<String, AuraConcept>();
	
	// the concept graph, multiple edges and loops between two vertices are not permitted
	static DirectedGraph<AuraConcept, DefaultEdge> graph = new SimpleDirectedGraph<AuraConcept, DefaultEdge>(DefaultEdge.class);
	
	static String rootName = "thing";
	
	static AuraConcept root;
		
	public static void create() {
		//String userHome = System.getProperty( "user.home" );
		//AuraConceptManager.create(userHome+"/Vulcan/aura-vocab-oct24-srt.tsv");
		AuraConceptManager.create(FileManager.getResource("resources/aura/aura-vocab-oct24-srt.tsv"));
	}
	
	public static void create(String file) {
		if (root != null) return; // in case this is called multiple times.
		BufferedReader in;
		String line;
		String[] splits;
		try {
			in = FileManager.getReader(file);
			while ((line = in.readLine()) != null) {
				if (line.startsWith("biology-concept")) {
					// biology-concept Zygote  "yes"
					splits = line.split("\t");
					if (splits[2].contains("yes"))
						addConcept(true, splits[1]);
					else
						addConcept(false, splits[1]);
				} else if (line.startsWith("concept2phrase")) { 
					// concept2phrase 3-Prime-Carbon  "3 prime carbon"
					splits = line.split("\t");
					addPhrase2Concept(splits[1], splits[2].replaceAll("\"", ""));
				} else if (line.startsWith("superclass")) { 
					// superclass  3-Prime-Carbon  Carbon
					splits = line.split("\t");
					addEdge(splits[2], splits[1]);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		cleanup();
	}
	
	public static void addConcept(boolean isBio, String name) {
		AuraConcept concept = new AuraConcept(isBio, name);
		name2concept.put(name, concept);
		graph.addVertex(concept);
	}
	
	public static void addPhrase2Concept(String concept, String phrase) {
		if (!name2concept.containsKey(concept))
			addConcept(false, concept);
		name2concept.get(concept).addPhrase(phrase);
		phrase2concept.put(phrase, name2concept.get(concept));
	}
	
	public static void addEdge(String governor, String dependent) {
		if (!name2concept.containsKey(governor)) {
			logger.warning("Error: concept not found: "+governor);
			return;
		}
		if (!name2concept.containsKey(dependent)) {
			logger.warning("Error: concept not found: "+dependent);
			return;
		}
		
		graph.addEdge(name2concept.get(governor), name2concept.get(dependent));
		name2concept.get(governor).addDependent(name2concept.get(dependent));
		name2concept.get(dependent).addGovernor(name2concept.get(governor));		
	}
	
	
	/**
	 * There are quite some island nodes with no parents or children, remove them.
	 * There will still be some small cluster of nodes that are isolated from the 
	 * main cluster left (mostly 2-tuples and triples).
	 */
	public static void cleanup() {
		List<AuraConcept> concept2remove = new ArrayList<AuraConcept>();
		
		for (AuraConcept c:graph.vertexSet()) {
			if (c.getNumOfDependents() == 0 && c.getNumOfGovernors() == 0)
				concept2remove.add(c);
		}
		name2concept.values().removeAll(concept2remove);
		phrase2concept.values().removeAll(concept2remove);
		graph.removeAllVertices(concept2remove);
		if (name2concept.containsKey(rootName)) {
			logger.severe("Root node (Thing) not found! Check your KB!");
		} else
			root = name2concept.get(rootName);
	}
	
	/**
	 * Assume top root is depth 1, then get the governor of node <code>concept</code> at <code>depth</code>.
	 * if the depth of <code>concept</code> is less than <code>depth</code>, then return <code>concept</code> itself.
	 * Note: a good place for memoization.
	 */
	public static AuraConcept getGovernorByLevel(AuraConcept concept, int depth) {
		if (root == null)
			cleanup();
		List<DefaultEdge> path = DijkstraShortestPath.findPathBetween(graph, root, concept);
		if (path == null)
			return null;
		if (path.size() > depth)
			return graph.getEdgeTarget(path.get(depth-1));
		else
			return concept;
	}
	
	public static int getDepthOfNode(AuraConcept concept) {
		if (root == null)
			cleanup();
		List<DefaultEdge> path = DijkstraShortestPath.findPathBetween(graph, root, concept);
		if (path == null)
			return -1;
		else
			return path.size() + 1;
	}
	
	
	public String toString() {
		return graph.toString();
	}
	
	public static HashMap<String, AuraConcept> getPhrase2concept() { return phrase2concept; }

	public static HashMap<String, AuraConcept> getName2concept() { return name2concept; }

	public static DirectedGraph<AuraConcept, DefaultEdge> getGraph() { return graph; }

	public static String getRootName() { return rootName; }

	public static AuraConcept getRoot() { return root; }

}
