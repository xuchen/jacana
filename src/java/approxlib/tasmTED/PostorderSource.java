package approxlib.tasmTED;

public interface PostorderSource {

	/**
	 * Append as sequence of nodes to the given {@link PostorderQueue}.
	 * 
	 * @param postorderQueue append nodes to this {@link PostorderQueue}
	 */
	abstract public void appendTo(PostorderQueue postorderQueue);

}