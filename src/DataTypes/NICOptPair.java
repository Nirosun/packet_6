package DataTypes;

import NetworkElements.LSRNIC;

/**
 * Created by niro on 12/9/14.
 */
public class NICOptPair{
    private LSRNIC nic; // The nic of the pair
    private OpticalLabel label; // the VC of the pair

    /**
     * Constructor for a pair of (nic, vc)
     * @param nic the nic that is in the pair
     * @param vc the vc that is in the pair
     * @since 1.0
     */
    public NICOptPair(LSRNIC nic, OpticalLabel label){
        this.nic = nic;
        this.label = label;
    }

    /**
     * Returns the nic that makes up half of the pair
     * @return the nic that makes up half of the pair
     * @since 1.0
     */
    public LSRNIC getNIC(){
        return this.nic;
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
        if(o instanceof NICLabelPair){
            NICOptPair other = (NICOptPair) o;

            if(other.getNIC()==this.getNIC() && other.getLabel()==this.getLabel())
                return true;
        }

        return false;
    }


}
