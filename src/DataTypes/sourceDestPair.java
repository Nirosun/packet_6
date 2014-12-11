package DataTypes;

/**
 * Created by niro on 12/10/14.
 */
public class sourceDestPair implements Comparable<sourceDestPair> {

    private int dest; // The dest of the pair
    private int source; // the source of the pair

    /**
     * Constructor for a pair of (nic, vc)
     * @param nic the nic that is in the pair
     * @param vc the vc that is in the pair
     * @since 1.0
     */
    public sourceDestPair(int source, int dest){
        this.dest = dest;
        this.source = source;
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
     * Returns the source that makes up half of the pair
     * @return the source that makes up half of the pair
     * @since 1.0
     */
    public int getSource(){
        return this.source;
    }

    /**
     * Returns whether or not a given object is the same as this pair. I.e. it is a pair containing the same nic and vc
     * @return true/false the given object of the same as this object
     * @since 1.0
     */
    public boolean equals(Object o){
        if(o instanceof sourceDestPair){
            sourceDestPair other = (sourceDestPair) o;

            if(other.getDest() == this.getDest() && other.getSource() == this.getSource())
                return true;
        }

        return false;
    }

    /**
     * Returns hash code.
     */
    public int hashCode() {
        int hash = this.dest * 10000 + this.source;
        return hash;
    }

    /**
     * Allows this object to be used in a TreeMap
     * @returns if this object is less than, equal to, or greater than a given object
     * @since 1.0
     */
    public int compareTo(sourceDestPair o){
        return this.getSource()-o.getSource();
    }
}
