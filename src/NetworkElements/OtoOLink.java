package NetworkElements;

import DataTypes.*;

public class OtoOLink {
	private LSRNIC r1NIC=null, r2NIC=null;
	private Boolean trace=false;
	private Boolean optical = false;
	private Boolean lossOfLight = false;
	
	/**
	 * The default constructor for a OtoOLink
	 * @param computerNIC
	 * @param routerNIC
	 * @since 1.0
	 */
	public OtoOLink(LSRNIC r1NIC, LSRNIC r2NIC){
		this.r1NIC = r1NIC;
		this.r1NIC.connectOtoOLink(this);
		this.r2NIC = r2NIC;
		this.r2NIC.connectOtoOLink(this);

		int addr1 = r1NIC.getParent().getAddress();
		int addr2 = r2NIC.getParent().getAddress();
		GraphInfo.graphIP.get(addr1).add(addr2);
		GraphInfo.graphIP.get(addr2).add(addr1);
		GraphInfo.nicsIP.get(addr1).add(r1NIC);
		GraphInfo.nicsIP.get(addr2).add(r2NIC);
		
		if(this.trace){
			if(r1NIC==null)
				System.err.println("Error (OtoOLink): R1 nic is null");
			if(r1NIC==null)
				System.err.println("Error (OtoOLink): R2 nic is null");
		}
	}

	/**
	 * The default constructor for a OtoOLink
	 * @param computerNIC
	 * @param routerNIC
	 * @param optical - determines if the link is optical or not
	 * @since 1.0
	 */
	public OtoOLink(LSRNIC r1NIC, LSRNIC r2NIC, Boolean optical){
		this.optical = optical;
		this.r1NIC = r1NIC;
		this.r1NIC.connectOtoOLink(this);
		this.r2NIC = r2NIC;
		this.r2NIC.connectOtoOLink(this);

		int addr1 = r1NIC.getParent().getAddress();
		int addr2 = r2NIC.getParent().getAddress();

		if (optical) {
			GraphInfo.graphOpt.get(addr1).add(addr2);
			GraphInfo.graphOpt.get(addr2).add(addr1);
			GraphInfo.nicsOpt.get(addr1).add(r1NIC);
			GraphInfo.nicsOpt.get(addr2).add(r2NIC);
		}
		else {
			GraphInfo.graphIP.get(addr1).add(addr2);
			GraphInfo.graphIP.get(addr2).add(addr1);
			GraphInfo.nicsIP.get(addr1).add(r1NIC);
			GraphInfo.nicsIP.get(addr2).add(r2NIC);
		}
		
		if(this.trace){
			if(r1NIC==null)
				System.err.println("Error (OtoOLink): R1 nic is null");
			if(r1NIC==null)
				System.err.println("Error (OtoOLink): R2 nic is null");
		}

	}
	
	/**
	 * Sends a packet from one end of the link to the other
	 * @param currentPacket the packet to be sent
	 * @param nic the nic the packet is being sent from
	 * @since 1.0
	 */
	public void sendPacket(Packet currentPacket, LSRNIC nic){

		if (optical && currentPacket.getOpticalLabel() == OpticalLabel.NA)
		{
			System.err.println("(OtoOLink) Error: You are trying to send a packet without an optical label through an optical link.");
		} 
		else if (optical && this.lossOfLight)
		{
			return;
		}
		else {
			if(this.r1NIC.equals(nic)){
				if(this.trace)
					System.out.println("(OtoOLink) Trace: sending packet from router A to router B");
				this.r2NIC.receivePacket(currentPacket);
			}
			else if(this.r2NIC.equals(nic)){
				if(this.trace)
					System.out.println("(OtoOLink) Trace: sending packet from router B to router A");
				this.r1NIC.receivePacket(currentPacket);
			}
			else
				System.err.println("(OtoOLink) Error: You are trying to send a packet down a link that you are not connected to");
		}
	}

	public Boolean getOptical() {
		return optical;
	}

	public void setLossOfLight(Boolean lossOfLight) {
		this.lossOfLight = lossOfLight;
	}

	public LSRNIC getOtherNIC(LSRNIC current) {
		if(current==r1NIC){
			return r2NIC;
		}else if(current==r2NIC){
			return r1NIC;
		}else{
			System.err.println("(OtoOLink) Error: Current NIC not connected to a OtoOlink!");
			return null;
		}
	}
	
	

}
