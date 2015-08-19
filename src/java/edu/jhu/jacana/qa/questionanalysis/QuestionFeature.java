/**
 * 
 */
package edu.jhu.jacana.qa.questionanalysis;

import java.util.HashSet;
import java.util.logging.Logger;

import edu.jhu.jacana.dependency.DependencyTree;
import edu.stanford.nlp.trees.TreeGraphNode;

/**
 * This class extracts question features that might infer question types, including:
 * 1. question word
 * 2. for specifically what questions:
 * 	a. what-lemma-? feature such that ? is a modifier for what (like "what country" or "what is the name")
 *  b. what-ner-? feature such that ? is the NER of the modifier
 *  c. what-vb-noun if the question is of the type "what is the name"
 *  d. what-noun-vb if the question is of the type "what country"
 * @author Xuchen Yao
 *
 */
public class QuestionFeature {

	private static Logger logger = Logger.getLogger(QuestionFeature.class.getName());

	public static String[] extract(DependencyTree qTree) {

		HashSet<String> features = new HashSet<String>();
		features.add("qword="+QuestionWordExtractor.getQuestionWords(qTree));

		int whatIdx = -1;
		for (int i=0; i<qTree.getSize(); i++) {
			if (qTree.getLabels().get(i).lemma().startsWith("what")) {
				whatIdx = i;
				break;
			}
		}
		if (whatIdx == -1)
			return features.toArray(new String[features.size()]);
		TreeGraphNode what = qTree.getTree().get(whatIdx);

		boolean found_lemma = false;
		if (qTree.getLabels().get(whatIdx).tag().equals("wp") && qTree.getDependencies().get(whatIdx).reln().toString().equals("vmod")) {
			// MXPOST tagger ALWAYS tags "what" with WP and MSTParser ALWAYS tags "what" with VMOD 
			if (what.children() != null && what.children().length != 0) {
				if (qTree.getLabels().get(whatIdx+1).word().equals("kind") &&
						qTree.getLabels().get(whatIdx+2).word().equals("of")) {
					/* What    kind    of  a   community   is  a   Kibbutz ?
					 *
     +---+ 'is/be/vbz/qRoot' 
        +---+ 'what/what/wp/vmod' 
            +---+ 'kind/kind/nn/nmod' 
            +---+ 'of/of/in/nmod' 
                +---+ 'community/community/nn/pmod' 
                    +---+ 'a/a/dt/nmod' 
        +---+ 'kibbutz/kibbutz/nnp/prd' 
            +---+ 'a/a/dt/nmod' 
        +---+ '?/?/./p' 
					 */
					// we need to find the child of "of"
					TreeGraphNode ofNode = qTree.getTree().get(whatIdx+2);
					if (ofNode.children()!=null && ofNode.children().length!=0) {
						for (TreeGraphNode child:ofNode.children()) {
							String childTag = child.label().tag();
							// rule out prepositions and punctuation
							if (childTag.equals("in") || childTag.equals(".")) continue;
							if (childTag.startsWith("nn")) {
								features.add("what-kind="+child.label().lemma());
								features.add("what-kind");
								features.add("what-lemma="+child.label().lemma());
								qTree.setQContentWordIdx(qTree.getNode2idx().get(child));
								// it's a what-noun-verb structure
								features.add("what-noun-verb");
								String ner = qTree.getEntities().get(qTree.getIdxOfNode(child));
								features.add("what-ner="+ner);
								found_lemma = true;
							}
						}
					} 
				}
				if (!found_lemma) {
					// What    country is  the worlds  leading supplier    of  cannabis    ?
					/*
    +---+ 'is/be/vbz/qRoot' 
        +---+ 'what/what/wp/vmod' 
            +---+ 'country/country/nn/nmod' 
        +---+ 'supplier/supplier/nn/prd' 
            +---+ 'worlds/world/nns/nmod' 
                +---+ 'the/the/dt/nmod' 
            +---+ 'leading/lead/vbg/nmod' 
            +---+ 'of/of/in/nmod' 
                +---+ 'cannabis/cannabis/nn/pmod' 
        +---+ '?/?/./p' 
					 */
					for (TreeGraphNode child:what.children()) {
						String childTag = child.label().tag();
						// rule out prepositions and punctuation
						if (childTag.equals("in") || childTag.equals(".")) continue;
						if (childTag.startsWith("nn")) {
							features.add("what-lemma="+child.label().lemma());
							qTree.setQContentWordIdx(qTree.getNode2idx().get(child));
							// it's a what-noun-verb structure
							features.add("what-noun-verb");
							String ner = qTree.getEntities().get(qTree.getIdxOfNode(child));
							features.add("what-ner="+ner);
							found_lemma = true;
						}
					}
				}
			}
			if (!found_lemma) {
				TreeGraphNode parent = qTree.getDependencies().get(whatIdx).gov();
				if (parent != null) {
					String parentPos = parent.label().tag();
					if (parentPos.startsWith("vb")) {
						// What    is  the name    of  the highest mountain    in  Africa  ?
						// we want to extract "what-name" and "what-noun"
						/*
    +---+ 'is/be/vbz/qRoot' 
        +---+ 'what/what/wp/vmod' 
        +---+ 'name/name/nn/prd' 
            +---+ 'the/the/dt/nmod' 
            +---+ 'of/of/in/nmod' 
                +---+ 'mountain/mountain/nn/pmod' 
                    +---+ 'the/the/dt/nmod' 
                    +---+ 'highest/high/jjs/nmod' 
                    +---+ 'in/in/in/nmod' 
                        +---+ 'africa/africa/nnp/pmod' 
        +---+ '?/?/./p' 
						 */
						for (TreeGraphNode child:parent.children()) {
							if (child == what) continue;
							String childTag = child.label().tag();
							// rule out prepositions and punctuation
							if (childTag.equals("in") || childTag.equals(".")) continue;
							if (childTag.startsWith("nn")) {
								features.add("what-lemma="+child.label().lemma());
								//qTree.setQContentWordIdx(qTree.getNode2idx().get(child));
								// it's a what-verb-noun structure
								features.add("what-verb-noun");
								String ner = qTree.getEntities().get(qTree.getIdxOfNode(child));
								features.add("what-ner="+ner);
								found_lemma  = true;
							} else if (childTag.startsWith("vb")) {
								features.add("what-lemma="+child.label().lemma());
								// it's a what-verb-noun structure
								features.add("what-verb-verb");
								found_lemma = true;
							}
						}
					}
				}

				if (!found_lemma) {
					// 
				}
			}
		} else  {

			// Stanford parser tags "what" with either WDT or WP and attaches 
			// "what" to the Lexical Answer Type (such as "country" in "what country")
			if (qTree.getLabels().get(whatIdx+1).word().equals("kind") &&
					qTree.getLabels().get(whatIdx+2).word().equals("of")) {
				/* What    kind    of  a   community   is  a   Kibbutz ?
				 *
            +---+ 'is/be/vbz/qRoot' 
        +---+ 'kind/kind/nn/attr' 
            +---+ 'what/what/wdt/det' 
            +---+ 'of/of/in/prep' 
                +---+ 'community/community/nn/pobj' 
                    +---+ 'a/a/dt/det' 
        +---+ 'kibbutz/kibbutz/nnp/nsubj' 
            +---+ 'a/a/dt/det' 
        +---+ '?/?/./p' 
				 */
				// we need to find the child of "of"
				TreeGraphNode ofNode = qTree.getTree().get(whatIdx+2);
				if (ofNode.children()!=null && ofNode.children().length!=0) {
					for (TreeGraphNode child:ofNode.children()) {
						String childTag = child.label().tag();
						// rule out prepositions and punctuation
						if (childTag.equals("in") || childTag.equals(".")) continue;
						if (childTag.startsWith("nn")) {
							features.add("what-kind="+child.label().lemma());
							features.add("what-kind");
							features.add("what-lemma="+child.label().lemma());
							qTree.setQContentWordIdx(qTree.getNode2idx().get(child));
							// it's a what-noun-verb structure
							features.add("what-noun-verb");
							String ner = qTree.getEntities().get(qTree.getIdxOfNode(child));
							features.add("what-ner="+ner);
							found_lemma = true;
						}
					}
				} 

			}
			if (!found_lemma) {
				TreeGraphNode parent = qTree.getDependencies().get(whatIdx).gov();
				if (parent != null) {
					String parentPos = parent.label().tag();
					if (parentPos.startsWith("nn")) {
										// What    country is  the worlds  leading supplier    of  cannabis    ?
				/*
    +---+ 'is/be/vbz/qRoot' 
        +---+ 'country/country/nn/attr' 
            +---+ 'what/what/wdt/det' 
        +---+ 'producer/producer/nn/nsubj' 
            +---+ 'the/the/dt/det' 
            +---+ 'biggest/big/jjs/amod' 
            +---+ 'of/of/in/prep' 
                +---+ 'tungsten/tungsten/nn/pobj' 
        +---+ '?/?/./p' 
				 */
						features.add("what-lemma="+parent.label().lemma());
						qTree.setQContentWordIdx(qTree.getNode2idx().get(parent));
						// it's a what-noun-verb structure
						features.add("what-noun-verb");
						String ner = qTree.getEntities().get(qTree.getIdxOfNode(parent));
						features.add("what-ner="+ner);
						found_lemma = true;
					} else if (qTree.getDependencies().get(whatIdx).reln().toString().equals("dobj") && parentPos.startsWith("vb")) {
						// What does the company manufacture?
						// dobj    aux det nn  nsubj   root    p

								features.add("what-lemma="+parent.label().lemma());
								// it's a what-verb-noun structure
								features.add("what-verb");
								found_lemma = true;
					} else {
												// What    is  the name    of  the highest mountain    in  Africa  ?
						// we want to extract "what-name" and "what-noun"
						/*
    +---+ 'is/be/vbz/qRoot' 
        +---+ 'what/what/wp/attr' 
        +---+ 'name/name/nn/nsubj' 
            +---+ 'the/the/dt/det' 
            +---+ 'of/of/in/prep' 
                +---+ 'group/group/nn/pobj' 
                    +---+ 'durst/durst/nnp/poss' 
                        +---+ ''s/'s/pos/possessive' 
        +---+ '?/?/./p' 
						 */
						/*
						 * due to parse errors, the parent of what isn't always a verb:
What    is  the brightest   star    visible from    Earth   ?
WP  VBZ DT  JJS NN  JJ  IN  NNP .
attr    cop det amod    nsubj   root    prep    pobj    p
6   6   5   5   6   0   6   7   6
-   -   -   -   PER_DESC-B  -   -   LOCATION-B  -

						 */
						for (TreeGraphNode child:parent.children()) {
							if (child == what) continue;
							String childTag = child.label().tag();
							// rule out prepositions and punctuation
							if (childTag.equals("in") || childTag.equals(".")) continue;
							if (childTag.startsWith("nn")) {
								features.add("what-lemma="+child.label().lemma());
								//qTree.setQContentWordIdx(qTree.getNode2idx().get(child));
								// it's a what-verb-noun structure
								features.add("what-verb-noun");
								String ner = qTree.getEntities().get(qTree.getIdxOfNode(child));
								features.add("what-ner="+ner);
								found_lemma  = true;
							} else if (childTag.startsWith("vb")) {
								features.add("what-lemma="+child.label().lemma());
								// it's a what-verb-noun structure
								features.add("what-verb-verb");
								found_lemma = true;
							}
						}
						
						

					}
				}

			}


		}

		return features.toArray(new String[features.size()]);
	}
}
