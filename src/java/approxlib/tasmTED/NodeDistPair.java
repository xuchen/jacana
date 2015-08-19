package approxlib.tasmTED;

public class NodeDistPair implements Comparable {
	private int nodeID;
	private double dist;
	
	public NodeDistPair(int nodeID, double dist) {
		this.nodeID = nodeID;
		this.dist = dist;
	}

	public double getDist() {
		return dist;
	}

	public int getNodeID() {
		return nodeID;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Object o) {
		NodeDistPair otherResult = (NodeDistPair)o;
		if (this.getDist() < otherResult.getDist()) {
			return -1;
		} else if (this.getDist() == otherResult.getDist()) {
			if (this.getNodeID() < otherResult.getNodeID()) {
				return -1;
			} else if (this.getNodeID() > otherResult.getNodeID()) {
				return 1;
			} else {
				return 0;
			}
		} else {
			return 1;
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "(" + this.getNodeID() + "," + this.getDist() + ")";
	}

	@Override
	public boolean equals(Object obj) {
		NodeDistPair r = (NodeDistPair)obj;
		return this.getDist() == r.getDist() && this.getNodeID() == r.getNodeID();
	}
	
	
	
	
}
