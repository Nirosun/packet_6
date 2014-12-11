import NetworkElements.*;

import java.util.*;

public class example {
	// This object will be used to move time forward on all objects
	private int time = 0;
	private ArrayList<LSR> allConsumers = new ArrayList<LSR>();
	/**
	 * Create a network and creates connections
	 * @since 1.0
	 */
	public void go(){
		System.out.println("** SYSTEM SETUP **");
		
		// Create some new ATM Routers
		/*LSR r1 = new LSR(9);
		LSR r2 = new LSR(3);
		LSR r3 = new LSR(11);
		LSR r4 = new LSR(13);
		LSR r5 = new LSR(15);*/

		LSR rA = new LSR(0, true, false);
		LSR rB = new LSR(1, true, true);
		LSR rC = new LSR(2, false, true);
		LSR rD = new LSR(3, false, true);
		LSR rE = new LSR(4, false, true);
		LSR rF = new LSR(5, true, true);
		LSR rG = new LSR(6, true, false);
		
		// give the routers interfaces
		/*LSRNIC r1n1 = new LSRNIC(r1);
		LSRNIC r2n1 = new LSRNIC(r2);
		LSRNIC r2n2 = new LSRNIC(r2);
		LSRNIC r2n3 = new LSRNIC(r2);
		LSRNIC r3n1 = new LSRNIC(r3);
		LSRNIC r3n2 = new LSRNIC(r3);
		LSRNIC r3n3 = new LSRNIC(r3);
		LSRNIC r3n4 = new LSRNIC(r3);
		LSRNIC r4n1 = new LSRNIC(r4);
		LSRNIC r4n2 = new LSRNIC(r4);
		LSRNIC r4n3 = new LSRNIC(r4);
		LSRNIC r5n1 = new LSRNIC(r5);*/

		LSRNIC rAn1 = new LSRNIC(rA);
		LSRNIC rBn1 = new LSRNIC(rB);
		LSRNIC rBn2 = new LSRNIC(rB);
		LSRNIC rBn3 = new LSRNIC(rB);
		LSRNIC rCn1 = new LSRNIC(rC);
		LSRNIC rCn2 = new LSRNIC(rC);
		LSRNIC rCn3 = new LSRNIC(rC);
		LSRNIC rCn4 = new LSRNIC(rC);
		LSRNIC rDn1 = new LSRNIC(rD);
		LSRNIC rDn2 = new LSRNIC(rD);
		LSRNIC rDn3 = new LSRNIC(rD);
		LSRNIC rDn4 = new LSRNIC(rD);
		LSRNIC rEn1 = new LSRNIC(rE);
		LSRNIC rEn2 = new LSRNIC(rE);
		LSRNIC rEn3 = new LSRNIC(rE);
		LSRNIC rEn4 = new LSRNIC(rE);
		LSRNIC rFn1 = new LSRNIC(rF);
		LSRNIC rFn2 = new LSRNIC(rF);
		LSRNIC rFn3 = new LSRNIC(rF);
		LSRNIC rGn1 = new LSRNIC(rG);

		// physically connect the router's nics
		/*OtoOLink l1 = new OtoOLink(r1n1, r2n1);
		OtoOLink l2 = new OtoOLink(r2n2, r3n1);
		OtoOLink l2opt = new OtoOLink(r2n3, r3n2, true); // optical link
		OtoOLink l3 = new OtoOLink(r3n3, r4n1);
		OtoOLink l3opt = new OtoOLink(r3n4, r4n2, true); // optical link
		OtoOLink l4 = new OtoOLink(r4n3, r5n1);*/

		OtoOLink l1 = new OtoOLink(rAn1, rBn1);
		OtoOLink l2 = new OtoOLink(rBn2, rCn1);
		OtoOLink l2opt = new OtoOLink(rBn3, rCn2, true);
		OtoOLink l3 = new OtoOLink(rCn3, rDn1);
		OtoOLink l3opt = new OtoOLink(rCn4, rDn2, true);
		OtoOLink l4 = new OtoOLink(rDn3, rEn1);
		OtoOLink l4opt = new OtoOLink(rDn4, rEn2, true);
		OtoOLink l5 = new OtoOLink(rEn3, rFn1);
		OtoOLink l5opt = new OtoOLink(rEn4, rFn2, true);
		OtoOLink l6 = new OtoOLink(rFn3, rGn1);
		
		// Add the objects that need to move in time to an array
		this.allConsumers.add(rA);
		this.allConsumers.add(rB);
		this.allConsumers.add(rC);
		this.allConsumers.add(rD);
		this.allConsumers.add(rE);
		this.allConsumers.add(rF);
		this.allConsumers.add(rG);


		
		//send packets from router 1 to the other routers...
		rA.createPacket(6);
		rA.createPacket(6);

		for (int i = 0; i < 30; i ++) {
			tock();
		}

		rG.createPacket(0);

		for (int i = 0; i < 30; i ++) {
			tock();
		}

	}
	
	public void tock(){
		System.out.println("** TIME = " + time + " **");
		time++;		
		
		// Send packets between routers
		for(int i=0; i<this.allConsumers.size(); i++)
			allConsumers.get(i).sendPackets();

		// Move packets from input buffers to output buffers
		for(int i=0; i<this.allConsumers.size(); i++)
			allConsumers.get(i).receivePackets();
		
	}
	public static void main(String args[]){
		example go = new example();
		go.go();
	}
}