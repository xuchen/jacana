package approxlib.random;
import java.util.Random;

public class RandomVal {
	
	public final static String[] distribNames = 
		new String[]{"GAUSS", "UNIFORM", "CUSTOM"};
	
	public final static int GAUSS = 0;
	public final static int UNIFORM = 1;
	public final static int CUSTOM = 2;    
	
	private Random random;
	private long seed;
	private int distribution = UNIFORM;
	
	private int k = 2;
	
	public RandomVal() {
		this(UNIFORM, System.currentTimeMillis());
	}
	
	public RandomVal(long seed) {
		this(UNIFORM, seed);
	}
	
	public RandomVal(int distribution, long seed) {
		this.seed = seed;
		random = new Random(seed);
		this.distribution = distribution;
	}
	
	/**
	 * @param k for custom distribution (1-p)^k
	 */
	public void setK(int k) {
		this.k = k;
	}
	
	/**
	 * (1 - p)^k distibution, symmetric around 0.
	 */
	public double customDistrib() {
		double ranVal = random.nextDouble();
		if (random.nextBoolean()) {
			return Math.pow(1 - ranVal, k);
		} else {
			return - Math.pow(1 - ranVal, k);
		}
	}
	
	/**
	 * @return int value in [min,max]
	 */
	public int getInt(int min, int max) {
		if (distribution == UNIFORM) {
			return random.nextInt(max - min + 1) + min;
		} else if (distribution == GAUSS) {
			double mean = (max + min) / 2.0;
			double stdDev = (max - min + 1) / 2.0;
			return (int)Math.round(mean + random.nextGaussian() * stdDev);
		} else {
			double mean = (max + min) / 2.0;
			double stdDev = (max - min + 1) / 2.0;
			return (int)Math.round(mean + customDistrib() * stdDev);
		}
	}
	
	/**
	 * @return double value in [min,max]
	 */
	public double getDouble(double min, double max) {
		if (distribution == UNIFORM) {
			return min + (max - min) * random.nextDouble();
		} else if (distribution == GAUSS) {
			double mean = (max + min) / 2.0;
			double stdDev = (max - min) / 2.0;
			return mean + random.nextGaussian() * stdDev;
		} else {
			double mean = (max + min) / 2.0;
			double stdDev = (max - min) / 2.0;
			return mean + customDistrib() * stdDev;
		}
	}
	
	/**
	 * @return uniform choise of numberOfStrins different 
	 *         strings (with repetition)
	 */
	public String getString(int numberOfStrings) {
		return Integer.toString(random.nextInt(numberOfStrings), 
				Character.MAX_RADIX);    
	}
	
	public boolean getBoolean(double trueProbability) {
		return random.nextDouble() < trueProbability;
	}
	
	public void reset() {
		random = new Random(seed);
	}
	
	public int getDistribution() {
		return distribution;
	}
	
	@Override
	public String toString() {
		return distribNames[distribution];
	}
	
}
