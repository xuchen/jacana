package approxlib.random;
public class RandomBoolean extends RandomVal {

	double trueProbability;

	public RandomBoolean(double trueProbability, long seed) {
		super(UNIFORM, seed); 
		this.trueProbability = trueProbability;
	}

	public RandomBoolean(double trueProbability) {
		this(trueProbability, System.currentTimeMillis());
	}


	public boolean getBoolean() {
		return super.getBoolean(trueProbability);
	}

	@Override
	public String toString() {
		String res = trueProbability * 100 + "% true";
		return res;
	}

	public double getTrueProbability() {
		return trueProbability;
	}

}
