package approxlib.random;

public class RandomInt extends RandomVal {
	
	private int min;
	private int max;
	
	public RandomInt(int min, int max, 
			int distribution, long seed) {
		super(distribution, seed);
		this.min = min;
		this.max = max;
	}
	
	public RandomInt(int min, int max) {
		this(min, max, UNIFORM, System.currentTimeMillis());
	}    
	
	public int getMin() {
		return min;
	}
	
	public int getMax() {
		return max;
	}
	
	public int possibleValues() {
		if (getDistribution() != RandomVal.GAUSS) {
			return (max - min + 1);
		} else {
			return Integer.MAX_VALUE;
		}
	}
	
	/**
	 * [this.min..this.max]
	 */
	public int getInt() {
		return getInt(min, max);
	}
	
	@Override
	public String toString() {
		return "[" + min + "," + max + "] " + super.toString();
	}
}
