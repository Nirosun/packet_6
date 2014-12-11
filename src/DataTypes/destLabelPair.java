package DataTypes;

import NetworkElements.*;

public class destLabelPair implements Comparable<destLabelPair>{
	private int dest; // The nic of the pair
	private int label; // the VC of the pair

	/**
	 * Constructor for a pair of (nic, vc)
	 * @param dest the dest that is in the pair
	 * @param label the label that is in the pair
	 * @since 1.0
	 */
	public destLabelPair(int dest, int label){
		this.dest = dest;
		this.label = label;
	}

	/**
	 * Returns the dest that makes up half of the pair
	 * @return the dest that makes up half of the pair
	 * @since 1.0
	 */
	public int getDest(){
		return this.dest;
	}

	/**
	 * Returns the nic that makes up half of the pair
	 * @return the nic that makes up half of the pair
	 * @since 1.0
	 */
	public int getLabel(){
		return this.label;
	}

	/**
	 * Returns whether or not a given object is the same as this pair. I.e. it is a pair containing the same nic and vc
	 * @return true/false the given object of the same as this object
	 * @since 1.0
	 */
	public boolean equals(Object o){
		if(o instanceof destLabelPair){
			destLabelPair other = (destLabelPair) o;

			if(other.getDest()==this.getDest() && other.getLabel()==this.getLabel())
				return true;
		}

		return false;
	}

	/**
	 * Allows this object to be used in a TreeMap
	 * @returns if this object is less than, equal to, or greater than a given object
	 * @since 1.0
	 */
	public int compareTo(destLabelPair o){
		return this.getLabel()-o.getLabel();
	}
}

