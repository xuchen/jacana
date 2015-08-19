package iitb.CRF;

public class Entry {
	public Soln solns[];
	public boolean valid=true;
	public Entry() {}
	public Entry(int beamsize, int id, int pos) {
		solns = new Soln[beamsize];
		for (int i = 0; i < solns.length; i++)
			solns[i] = newSoln(id, pos);
	}
	protected Soln newSoln(int label, int pos) {
		return new Soln(label,pos);
	}
	public void clear() {
		valid = false;
		for (int i = 0; i < solns.length; i++)
			solns[i].clear();
	}
	public int size() {return solns.length;}
	public Soln get(int i) {return solns[i];}
	protected void insert(int i, float score, Soln prev) {
		Soln saved = solns[size()-1];
		for (int k = size()-1; k > i; k--) {
			//solns[k].copy(solns[k-1]);
			solns[k] = solns[k-1];
		}
		solns[i] = saved;
		solns[i].setPrevSoln(prev,score);
	}
	public void add(Entry e, float thisScore) {
		assert(valid);
		if (e == null) {
			add(thisScore);
			return;
		}
		// the soln within each entry are sorted.
		int insertPos = 0;
		for (int i = 0; (i < e.size()) && (insertPos < size()); i++) {
			float score = e.get(i).score + thisScore;
			insertPos = findInsert(insertPos, score, e.get(i));
		}

		//print()
	}
	protected int findInsert(int insertPos, float score, Soln prev) {
		for (; insertPos < size(); insertPos++) {
			if (score >= get(insertPos).score) {
				insert(insertPos, score, prev);
				insertPos++;
				break;
			}
		}
		return insertPos;
	}
	public void add(float thisScore) {
		findInsert(0, thisScore, null);
	}
	public int numSolns() {
		for (int i = 0; i < solns.length; i++)
			if (solns[i].isClear())
				return i;
		return size();
	}
	public void setValid() {valid=true;}
	void print() {
		String str = "";
		for (int i = 0; i < size(); i++)
			str += ("["+i + " " + solns[i].score + " i:" + solns[i].pos + " y:" + solns[i].label+"]");
		System.out.println(str);
	}

	public String toString(){
		assert(solns != null && solns[0] != null);
		String toString = "";
		toString += "[" + solns[0].pos + " " + solns[0].label + " " + solns[0].score;
		if(solns[0].prevSoln != null)
			toString += " : " + solns[0].prevSoln.pos + " " + solns[0].prevSoln.label + " " + solns[0].prevSoln.score;
		toString += "]";
		return toString;
	}
	
	public void sortEntries() {}
}