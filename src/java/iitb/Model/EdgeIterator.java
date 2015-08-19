package iitb.Model;

public interface EdgeIterator {
    void start();
    boolean hasNext();
    Edge next();
    boolean nextIsOuter(); // returns true if the next edge it will return is outer
}
