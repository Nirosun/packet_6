package DataTypes;

import NetworkElements.*;

public class destOptPair{
    private int dest; // The nic of the pair
    private OpticalLabel label; // the VC of the pair

    /**
     * Constructor for a pair of (nic, vc)
     * @param dest the dest that is in the pair
     * @param label the label that is in the pair
     * @since 1.0
     */
    public destOptPair(int dest, OpticalLabel label){
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
    public OpticalLabel getLabel(){
        return this.label;
    }

    /**
     * Returns whether or not a given object is the same as this pair. I.e. it is a pair containing the same nic and vc
     * @return true/false the given object of the same as this object
     * @since 1.0
     */
    public boolean equals(Object o){
        if(o instanceof destOptPair){
            destOptPair other = (destOptPair) o;

            if(other.getDest()==this.getDest() && other.getLabel()==this.getLabel())
                return true;
        }

        return false;
    }

}

