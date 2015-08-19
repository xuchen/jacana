/**
 * 
 */
package edu.jhu.jacana.feature.stanford;

import java.util.HashMap;

/**
 * The "stanford-parsed" folder contains data re-parsed by the Stanford parser.
 * The dependency labels are not fine-grained (12 labels from the MSTParser) anymore.
 * This class clusters the Stanford dependency label into some manually specified clusters
 * according to the Stanford Dependency Manual (Section 3).
 * 
 * @author Xuchen Yao
 *
 */
public class StanfordDepCluster {
	
	public static enum CLUSTER {
		OTHER {public String toString() {return "other";}},
		AUX {public String toString() {return "aux";}},
		AGENT {public String toString() {return "agent";}},
		COMP {public String toString() {return "comp";}},
		OBJ {public String toString() {return "obj";}},
		SUBJ {public String toString() {return "subj";}},
		CC {public String toString() {return "cc";}},
		CONJ {public String toString() {return "conj";}},
		EXPL {public String toString() {return "expl";}},
		MOD {public String toString() {return "mod";}},
		PARATAXIS {public String toString() {return "parataxis";}},
		PUNCT {public String toString() {return "punct";}},
		// ref, sdep/xsubj never appeared in training
	};
	
	public static HashMap<String, CLUSTER> label2cluster =
			new HashMap<String, CLUSTER> () {{
				put("auxpass", CLUSTER.AUX);
				put("cop", CLUSTER.AUX);
				put("acomp", CLUSTER.COMP);
				put("attr", CLUSTER.COMP);
				put("ccomp", CLUSTER.COMP);
				put("xcomp", CLUSTER.COMP);
				put("complm", CLUSTER.COMP);
				put("mark", CLUSTER.COMP);
				put("rel", CLUSTER.COMP);
				put("dobj", CLUSTER.OBJ);
				put("iobj", CLUSTER.OBJ);
				put("pobj", CLUSTER.OBJ);
				put("nsubj", CLUSTER.SUBJ);
				put("csubj", CLUSTER.SUBJ);
				put("cc", CLUSTER.CC);
				put("conj", CLUSTER.CONJ);
				put("expl", CLUSTER.EXPL);
				put("abbrev", CLUSTER.MOD);
				put("amod", CLUSTER.MOD);
				put("appos", CLUSTER.MOD);
				put("advcl", CLUSTER.MOD);
				put("purpcl", CLUSTER.MOD);
				put("det", CLUSTER.MOD);
				put("predet", CLUSTER.MOD);
				put("preconj", CLUSTER.MOD);
				put("infmod", CLUSTER.MOD);
				put("mwe", CLUSTER.MOD);
				put("partmod", CLUSTER.MOD);
				put("advmod", CLUSTER.MOD);
				put("neg", CLUSTER.MOD);
				put("rcmod", CLUSTER.MOD);
				put("quantmod", CLUSTER.MOD);
				put("nn", CLUSTER.MOD);
				put("npadvmod", CLUSTER.MOD);
				put("tmod", CLUSTER.MOD);
				put("num", CLUSTER.MOD);
				put("number", CLUSTER.MOD);
				put("prep", CLUSTER.MOD);
				put("poss", CLUSTER.MOD);
				put("possessive", CLUSTER.MOD);
				put("prt", CLUSTER.MOD);
				put("parataxis", CLUSTER.PARATAXIS);
				put("punct", CLUSTER.PUNCT);
			}};
			
		public static String getCluster(String label) {
			if (label2cluster.containsKey(label)) {
				return label2cluster.get(label).toString();
			} else {
				return CLUSTER.OTHER.toString();
			}
		}
		
		public static String[] getClusterArray(String prefix) {
			CLUSTER[] clusters = CLUSTER.values();
			String[] names = new String[clusters.length];
			for (int i=0; i<names.length; i++) {
				names[i] = prefix+clusters[i].toString();
			}
			return names;
		}

}
