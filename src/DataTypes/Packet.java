package DataTypes;

import java.util.LinkedList;
import java.util.Queue;

public class Packet {
	private int source, dest, DSCP; // The source and destination addresses
	private boolean OAM = false;
	private String OAMMsg = null; 
	private OpticalLabel opticalLabel = OpticalLabel.NA;
	private int label = -1;
	private Queue<MPLS> MPLSheader = new LinkedList<MPLS>(); // all of the MPLS headers in this router
	private int traceID = 0;	// The trace ID for the packet


	/**
	 * The default constructor for a packet
	 * @param source the source ip address of this packet
	 * @param dest the destination ip address of this packet
	 * @since 1.0
	 */
	public Packet(int source, int dest){
		try{
			this.source = source;
			this.dest = dest;
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * The default constructor for a packet
	 * @param source the source ip address of this packet
	 * @param dest the destination ip address of this packet
	 * @since 1.0
	 */
	public Packet(int source, int dest, OpticalLabel label){
		try{
			this.source = source;
			this.dest = dest;
			this.opticalLabel = label;
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * The default constructor for a packet
	 * @param source the source ip address of this packet
	 * @param dest the destination ip address of this packet
	 * @since 1.0
	 */
	public Packet(int source, int dest, int label){
		try{
			this.source = source;
			this.dest = dest;
			this.label= label;
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * Adds an MPLS header to a packet
	 * @since 1.0
	 */
	public void addMPLSheader(MPLS header){
		MPLSheader.add(header);
	}
	
	/**
	 * Pops an MPLS header from the packet
	 * @since 1.0
	 */
	public MPLS popMPLSheader(){
		return MPLSheader.poll();
	}
	
	/**
	 * Returns the source ip address of this packet
	 * @return the source ip address of this packet
	 * @since 1.0
	 */
	public int getSource(){
		return this.source;
	}
	
	/**
	 * Returns the destination ip address of this packet
	 * @return the destination ip address of this packet
	 * @since 1.0
	 */
	public int getDest(){
		return this.dest;
	}

	/**
	 * Set the DSCP field
	 * @param DSCP the DSCP field value
	 * @since 1.0
	 */
	public void setDSCP(int dSCP) {
		this.DSCP = dSCP;
	}

	/**
	 * Returns the DSCP field
	 * @return the DSCP field
	 * @since 1.0
	 */
	public int getDSCP() {
		return this.DSCP;
	}

	public OpticalLabel getOpticalLabel() {
		return opticalLabel;
	}

	public void setOpticalLabel(OpticalLabel label) {
		this.opticalLabel = label;
	}

	public int getLabel() {
		return this.label;
	}

	public void setLabel(int label) {
		this.label = label;
	}



	public boolean isOAM() {
		return OAM;
	}

	public void setOAM(boolean oAM, String msg) {
		OAM = oAM;
		OAMMsg = msg;
	}

	public String getOAMMsg() {
		return OAMMsg;
	}

	/**
	 * Returns the trace ID for this cell
	 * @return the trace ID for this cell
	 * @since 1.0
	 */
	public int getTraceID(){
		return this.traceID;
	}

	/**
	 * set trace id
	 * @param id
	 */
	public void setTraceID(int id) {
		this.traceID = id;
	}

}
	
