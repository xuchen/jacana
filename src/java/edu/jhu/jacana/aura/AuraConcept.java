/**
 * 
 */
package edu.jhu.jacana.aura;

import java.util.HashSet;

import edu.stanford.nlp.util.StringUtils;

/**
 * A simple concept object that only has three properties:
 * isBioConcept, name (of this concept) and phrase (instances of this concept).
 * @author Xuchen Yao
 *
 */
public class AuraConcept {
	
	boolean isBio;
	String name;
	HashSet<String> phrases;
	HashSet<AuraConcept> governors;
	HashSet<AuraConcept> dependents;
	
	public boolean isBio() { return isBio; }

	public void setBio(boolean isBio) { this.isBio = isBio; }

	public String getName() { return name; }

	public void setName(String name) { this.name = name; }

	public HashSet<String> getPhrases() { return phrases; }

	public void setPhrases(HashSet<String> phrases) { this.phrases = phrases; }

	public AuraConcept(boolean isBio, String name) {
		this.isBio = isBio;
		this.name = name;
		phrases = new HashSet<String>();
		governors = new HashSet<AuraConcept>();
		dependents = new HashSet<AuraConcept>();
	}
	
	public void addDependent(AuraConcept dep) { dependents.add(dep); }
	public void addGovernor(AuraConcept gov) { governors.add(gov); }
	public int getNumOfGovernors() { return governors.size(); }
	public int getNumOfDependents() { return dependents.size(); }
	
	public void addPhrase(String phrase) {
		phrases.add(phrase);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(name);
		//sb.append(" ("); sb.append(phrases); sb.append(")");
		return sb.toString();
	}
	
}
