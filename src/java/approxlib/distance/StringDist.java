/*
 * Created on Jul 10, 2008
 */
package approxlib.distance;

public interface StringDist {
    /** 
     * This method returns the distance between two strings as a 
     * scalar value. If the distance function is not symmetric 
     * the treeDist returns the distance from 
     * tree s1 to tree s2.
     *
     * @param s1 first tree
     * @param s2 second tree
     * @return dist(s1,s2) for a specific distance function "dist"
     */
    double stringDist(String s1, String s2);
    
    /**
     * Normalized distance between [0..1].
     * @param t1
     * @param t2
     * @return
     */
    double normStringDist(String s1, String s2);

}
