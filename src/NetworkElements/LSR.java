package NetworkElements;

import java.util.*;
import DataTypes.*;

public class LSR{
	private int address; // The AS address of this router
	private boolean psc;
	private boolean lsc;
	private ArrayList<LSRNIC> nics = new ArrayList<LSRNIC>(); // all of the nics in this router
	private TreeMap<Integer, LSRNIC> nextHopIP = new TreeMap<Integer, LSRNIC>(); // a map of which interface to use to get to a given router on the network
	private TreeMap<Integer, LSRNIC> nextHopOpt = new TreeMap<Integer, LSRNIC>(); // a map of which interface to use to get to a given router on the network

	private TreeMap<Integer, NICLabelPair> LabeltoLabelIP = new TreeMap<Integer, NICLabelPair>(); // a map of input VC to output nic and new VC number
	private HashMap<OpticalLabel, NICOptPair> LabeltoLabelOpt = new HashMap<OpticalLabel, NICOptPair>(); // a map of input VC to output nic and new VC number
	private TreeMap<Integer, destLabelPair> LabeltoDestLabel = new TreeMap<Integer, destLabelPair>();
	private HashMap<OpticalLabel, destOptPair> LabeltoDestOpt = new HashMap<OpticalLabel, destOptPair>();

	//private TreeMap<Integer, NICLabelPair> LabeltoLabelOpt = new TreeMap<Integer, NICLabelPair>(); // a map of input VC to output nic and new VC number
	private HashMap<Integer, Integer> destToLabel = new HashMap<Integer, Integer>();
	private HashMap<Integer, OpticalLabel> destToOpt = new HashMap<Integer, OpticalLabel>();
	private HashMap<sourceDestPair, Integer> sourceDestToLabel = new HashMap<sourceDestPair, Integer>();	// For ingress PSC/LSC
	private ArrayList<Packet> waitList = new ArrayList<Packet>();	// packets waiting to be send due to path setting up

	private boolean isStart = true;	// used for deciding if setting up nexthop table
	boolean trace = true;
	private int traceID = (int) (Math.random() * 100000); // create a random trace id for cells
	private boolean displayCommands = true;


	/**
	 * The default constructor for an ATM router
	 * @param address the address of the router
	 * @since 1.0
	 */
	public LSR(int address){
		this.address = address;
	}
	
	/**
	 * The default constructor for an ATM router
	 * @param address the address of the router
	 * @since 1.0
	 */
	public LSR(int address, boolean psc, boolean lsc ){

		this.address = address;
		this.psc = psc;
		this.lsc = lsc;
		GraphInfo.graphIP.put(this.address, new ArrayList<Integer>());
		GraphInfo.nicsIP.put(this.address, new ArrayList<LSRNIC>());
		if (lsc) {
			GraphInfo.graphOpt.put(this.address, new ArrayList<Integer>());
			GraphInfo.nicsOpt.put(this.address, new ArrayList<LSRNIC>());
		}


	}

	/**
	 * The return the router's address
	 * @since 1.0
	 */
	public int getAddress(){
		return this.address;
	}
	
	/**
	 * Adds a nic to this router
	 * @param nic the nic to be added
	 * @since 1.0
	 */
	public void addNIC(LSRNIC nic){
		this.nics.add(nic);
	}
	
	/**
	 * This method processes data and OAM cells that arrive from any nic with this router as a destination
	 * @param currentPacket the packet that arrived at this router
	 * @param nic the nic that the cell arrived on
	 * @since 1.0
	 */
	public void receivePacket(Packet currentPacket, LSRNIC nic){
		if (this.isStart) {
			if (this.psc) {
				this.calculateNextHop(true);
			}
			if (this.lsc) {
				this.calculateNextHop(false);
			}
			this.isStart = false;
		}
		System.out.println("Router addr: " + this.address);
		//this.printSwitchingTable();

		System.out.println("packet: " + currentPacket.getSource() + ", " + currentPacket.getDest());
		System.out.println("\tOAM: " + currentPacket.isOAM());
		if (currentPacket.isOAM())
		{
			System.out.println("\tOAM: " + currentPacket.getOAMMsg() + ", " + currentPacket.getOpticalLabel().toString() + ", " + currentPacket.getLabel());
			int toAddress = currentPacket.getDest();

			// Receive PATH message
			if (currentPacket.getOAMMsg().equals("PATH")) {
				this.receivedPath(currentPacket);

				// Normal PSC
				if (this.psc && !this.lsc) {
					if (this.address == toAddress) {	// dest address match
						int inLabel = this.getAvailableIPLabel();
						int outLabel = currentPacket.getLabel();
						this.LabeltoLabelIP.put(inLabel, new NICLabelPair(nic, -1));	// ######################
						this.LabeltoLabelIP.put(-1, new NICLabelPair(nic, outLabel));	// ########################
						this.LabeltoDestLabel.put(inLabel, new destLabelPair(this.address, -1));
						this.LabeltoDestLabel.put(-1, new destLabelPair(currentPacket.getSource(), outLabel));

						this.destToLabel.put(currentPacket.getSource(), outLabel);

						// send RESV
						Packet resv = new Packet(currentPacket.getDest(), currentPacket.getSource(), inLabel);
						resv.setOAM(true, "RESV");
						resv.setTraceID(this.getTraceID());
						this.sentResv(resv);
						nic.sendPacket(resv, this);
					}
					else {
						if (this.nextHopIP.containsKey(toAddress)){
							LSRNIC outNIC = this.nextHopIP.get(toAddress);
							int inLabel = this.getAvailableIPLabel();
							int outLabel = currentPacket.getLabel();
							this.LabeltoLabelIP.put(inLabel, new NICLabelPair(outNIC, outLabel));
							this.LabeltoDestLabel.put(inLabel, new destLabelPair(toAddress, outLabel));
							Packet path = new Packet(currentPacket.getSource(), currentPacket.getDest(), inLabel);
							path.setOAM(true, "PATH");
							path.setTraceID(this.getTraceID());
							this.sentPath(path);
							outNIC.sendPacket(path, this);
						}
						else {	// can't find dest addr in nexthop, send PATHERR
							Packet patherr = new Packet(currentPacket.getDest(), currentPacket.getSource());
							patherr.setOAM(true, "PATHERR");
							this.sentPathErr(patherr);
							nic.sendPacket(patherr, this);
						}

					}

				}

				// ingress PSC/LSC
				else if (this.psc && this.lsc && nic.getNextLSR().isPsc()) {
					LSRNIC outIPNIC = this.nextHopIP.get(toAddress);
					int nextPSCAddr = this.getNextPSCAddr(toAddress);
					LSRNIC outOptNIC = this.nextHopOpt.get(nextPSCAddr);
					int inLabelIP = -1;	// ###################################
					int outLabelIP = currentPacket.getLabel();

					//this.LabeltoLabelIP.put(inLabelIP, new NICLabelPair(outIPNIC, outLabelIP));
					//this.destToLabel.put(currentPacket.getDest(), outLabelIP);
					this.sourceDestToLabel.put(new sourceDestPair(currentPacket.getSource(), currentPacket.getDest()), outLabelIP);

					OpticalLabel inLabelOpt = this.getAvailableOptLabel();
					this.LabeltoLabelOpt.put(inLabelOpt, new NICOptPair(outOptNIC, OpticalLabel.NA));	// ##################
					Packet path = new Packet(currentPacket.getSource(), currentPacket.getDest(), inLabelOpt);
					path.setOAM(true, "PATH");
					path.setTraceID(this.getTraceID());
					this.sentPath(path);
					outIPNIC.sendPacket(path, this);

				}

				// egress PSC/LSC
				else if (this.psc && this.lsc && nic.getNextLSR().isLsc()) {
					// should deal with Path (LSC) & Path (PSC)
					// Path (LSC)
					if (currentPacket.getOpticalLabel() != OpticalLabel.NA) {
						// send RESV
						OpticalLabel ulLabel = currentPacket.getOpticalLabel();
						OpticalLabel dlLabel = this.getAvailableOptLabel();
						LSRNIC outIPNIC = this.nextHopIP.get(toAddress);
						this.LabeltoLabelOpt.put(OpticalLabel.NA, new NICOptPair(nic, ulLabel));
						this.LabeltoLabelOpt.put(dlLabel, new NICOptPair(outIPNIC, OpticalLabel.NA));
						int nextPSCAddr = this.getNextPSCAddr(currentPacket.getSource());
						this.destToOpt.put(nextPSCAddr, ulLabel);

						Packet resv = new Packet(currentPacket.getDest(), currentPacket.getSource(), dlLabel);
						resv.setOAM(true, "RESV");
						resv.setTraceID(this.getTraceID());
						this.sentResv(resv);
						nic.sendPacket(resv, this);

					}
					// Path (PSC)
					else {
						// forward PATH
						int outLabel = currentPacket.getLabel();
						int inLabel = this.getAvailableIPLabel();
						LSRNIC outNIC = this.nextHopIP.get(toAddress);
						this.LabeltoLabelIP.put(inLabel, new NICLabelPair(outNIC, outLabel));

						Packet path = new Packet(currentPacket.getSource(), currentPacket.getDest(), inLabel);
						path.setOAM(true, "PATH");
						path.setTraceID(this.getTraceID());
						this.sentPath(path);
						outNIC.sendPacket(path, this);
					}
				}

				// Normal LSC
				else if (!this.psc && this.lsc) {
					// 1st PATH
					if (currentPacket.getOpticalLabel() != OpticalLabel.NA) {
						LSRNIC outIPNIC = this.nextHopIP.get(toAddress);
						int nextPSCAddr = this.getNextPSCAddr(currentPacket.getSource());
						LSRNIC outOptNIC = this.nextHopOpt.get(nextPSCAddr);
						OpticalLabel inLabelOpt = this.getAvailableOptLabel();
						OpticalLabel outLabelOpt = currentPacket.getOpticalLabel();
						this.LabeltoLabelOpt.put(inLabelOpt, new NICOptPair(outOptNIC, outLabelOpt));	// ##################
						Packet path = new Packet(currentPacket.getSource(), currentPacket.getDest(), inLabelOpt);
						path.setOAM(true, "PATH");
						path.setTraceID(this.getTraceID());
						this.sentPath(path);
						outIPNIC.sendPacket(path, this);
					}
					// 2nd PATH
					else {
						LSRNIC outNIC = this.nextHopIP.get(toAddress);
						this.sentPath(currentPacket);
						outNIC.sendPacket(currentPacket, this);
					}
				}
				else {
					System.out.println("Error: Incapable router.");
				}
			}

			// Receive RESV message
			else if (currentPacket.getOAMMsg().equals("RESV")) {
				this.receivedResv(currentPacket);
				// Resv (LSC)
				if (currentPacket.getOpticalLabel() != OpticalLabel.NA) {
					// Ingress PSC/LSC
					if (this.isPsc()) {
						//int outLabel = this.destToLabel.get(currentPacket.getSource());
						int outLabel = this.sourceDestToLabel.get(new sourceDestPair(currentPacket.getDest(), currentPacket.getSource()));
						int inLabel = this.getAvailableIPLabel();
						LSRNIC outIPNIC = this.nextHopIP.get(currentPacket.getSource());
						LSRNIC sourceNIC = this.nextHopIP.get(currentPacket.getDest());
						int nextPSCAddr = this.getNextPSCAddr(currentPacket.getSource());
						LSRNIC outOptNIC = this.nextHopOpt.get(nextPSCAddr);

						//this.LabeltoLabelIP.put(inLabel, new NICLabelPair(outIPNIC, outLabel));
						this.LabeltoLabelIP.put(inLabel, new NICLabelPair(sourceNIC, outLabel));

						this.LabeltoLabelOpt.put(OpticalLabel.NA, new NICOptPair(outOptNIC, currentPacket.getOpticalLabel()));
						//this.destToLabel.put(nextPSCAddr, currentPacket.getOpticalLabel());
						//this.LabeltoDestOpt.put(OpticalLabel.NA, new destOptPair(nextPSCAddr, currentPacket.getOpticalLabel()));
						this.destToOpt.put(nextPSCAddr, currentPacket.getOpticalLabel());

						// send PATH (PSC)
						Packet path = new Packet(currentPacket.getDest(), currentPacket.getSource(), inLabel);
						path.setOAM(true, "PATH");
						path.setTraceID(this.getTraceID());
						this.sentPath(path);
						outIPNIC.sendPacket(path, this);

					}
					// Normal LSC
					else {
						OpticalLabel outLabel = currentPacket.getOpticalLabel();
						OpticalLabel inLabel = this.getAvailableOptLabel();
						LSRNIC outIPNIC = this.nextHopIP.get(toAddress);


						int nextPSCAddr = this.getNextPSCAddr(currentPacket.getSource());
						LSRNIC outOptNIC = this.nextHopOpt.get(nextPSCAddr);
						this.LabeltoLabelOpt.put(inLabel, new NICOptPair(outOptNIC, outLabel));

						Packet resv = new Packet(currentPacket.getSource(), currentPacket.getDest(), inLabel);
						resv.setOAM(true, "RESV");
						resv.setTraceID(this.getTraceID());
						this.sentResv(resv);
						outIPNIC.sendPacket(resv, this);
					}
				}

				// Resv (PSC)
				else {
					// PSC
					if (this.psc) {
						int outLabel = currentPacket.getLabel();
						int inLabel = this.getAvailableIPLabel();

						// RESV reaches the destination
						if (this.address == toAddress) {
							// Should send ResvConf and do other stuffs...
							System.out.println("RESV reaches the end.");

							this.destToLabel.put(currentPacket.getSource(), outLabel);
							this.LabeltoLabelIP.put(inLabel, new NICLabelPair(nic, outLabel));
							//this.LabeltoLabel.put(outLabel, new NICLabelPair(nic, inLabel));

							// send RESVCONF
							Packet conf = new Packet(currentPacket.getDest(), currentPacket.getSource(), currentPacket.getLabel());
							conf.setOAM(true, "RESVCONF");
							conf.setTraceID(this.getTraceID());
							this.sentResvConf(conf);
							nic.sendPacket(conf, this);

							// send packets in the waiting list
							ArrayList<Integer> deleteID = new ArrayList<Integer>();
							for (int i = 0; i < this.waitList.size(); i ++) {
								Packet packet = this.waitList.get(i);
								if (this.destToLabel.containsKey(packet.getDest())
										&& this.destToLabel.get(packet.getDest()) != -1) {
									//packet.addMPLSheader(new MPLS(inLabel, 0, 1));
									packet.setLabel(outLabel);
									nic.sendPacket(packet, this);
									if (trace) {
										System.out.println("Sending packet " + packet.getTraceID() + " from router " + this.getAddress());
									}
									deleteID.add(i);
								}
							}
							for (int i = deleteID.size() - 1; i >= 0; i --) {
								int tmp = deleteID.get(i);
								this.waitList.remove(tmp);
							}
							return;

						}
						else {
							//LSRNIC outNIC = this.nextHopIP.get(currentPacket.getSource());
							LSRNIC sendNIC = this.nextHopIP.get(currentPacket.getDest());
							this.LabeltoLabelIP.put(inLabel, new NICLabelPair(nic, outLabel));
							Packet resv = new Packet(currentPacket.getSource(), currentPacket.getDest(), inLabel);
							resv.setOAM(true, "RESV");
							resv.setTraceID(this.getTraceID());
							this.sentResv(resv);
							sendNIC.sendPacket(resv, this);
						}
					}

					// Normal LSC
					else {
						LSRNIC outNIC = this.nextHopIP.get(currentPacket.getDest());
						this.sentResv(currentPacket);
						outNIC.sendPacket(currentPacket, this);
					}
				}
			}

			// Receive ResvConf
			else if (currentPacket.getOAMMsg().equals("RESVCONF")) {
				this.receivedResvConf(currentPacket);
				if (this.address == currentPacket.getDest()) {
					// Could start sending normal packets now...
					//System.out.println("");


					//this.destToLabel.put(currentPacket.getSource(), currentPacket.getLabel());
				}
				else {
					LSRNIC outNIC = this.nextHopIP.get(currentPacket.getDest());
					this.sentResvConf(currentPacket);
					outNIC.sendPacket(currentPacket, this);
				}
			}

			// Receive PathErr
			else if (currentPacket.getOAMMsg().equals("PATHERR")) {
				this.receivedPathErr(currentPacket);
				if (this.address == currentPacket.getDest()) {
					// PathErr reaches end
				}
				else {
					LSRNIC outNIC = this.nextHopIP.get(currentPacket.getDest());
					this.sentPathErr(currentPacket);
					outNIC.sendPacket(currentPacket, this);
				}
			}

			// Receive ResvErr
			else if (currentPacket.getOAMMsg().equals("RESVERR")) {
				this.receivedResvErr(currentPacket);
				if (this.address == currentPacket.getDest()) {
					// ResvErr reaches end
				}
				else {
					LSRNIC outNIC = this.nextHopIP.get(currentPacket.getDest());
					this.sentResvErr(currentPacket);
					outNIC.sendPacket(currentPacket, this);
				}
			}

			else {
				System.out.println("Error: Message not implemented.");
			}

			//this.printSwitchingTable();
		}

		// Normal packets
		else {
			System.out.println("\t" + currentPacket.getOpticalLabel() + ", " + currentPacket.getLabel());
			LSRNIC outNIC;

			// Normal PSC
			if (this.psc && !this.lsc) {
				int outLabel = this.LabeltoLabelIP.get(currentPacket.getLabel()).getLabel();
				outNIC = this.LabeltoLabelIP.get(currentPacket.getLabel()).getNIC();
				currentPacket.setLabel(outLabel);

			}
			// Normal LSC
			else if (!this.psc && this.lsc) {
				OpticalLabel outLabel = this.LabeltoLabelOpt.get(currentPacket.getOpticalLabel()).getLabel();
				outNIC = this.LabeltoLabelOpt.get(currentPacket.getOpticalLabel()).getNIC();
				currentPacket.setOpticalLabel(outLabel);
				this.printSwitchingTable();
			}
			// Ingress PSC/LSC
			else if (nic.getNextLSR().isPsc()) {
				int nextPSCAddr = this.getNextPSCAddr(currentPacket.getDest());
				OpticalLabel outLabel = this.destToOpt.get(nextPSCAddr);
				//OpticalLabel outLabel = this.LabeltoLabelOpt.get(OpticalLabel.NA).getLabel();
				outNIC = this.LabeltoLabelOpt.get(currentPacket.getOpticalLabel()).getNIC();
				currentPacket.setOpticalLabel(outLabel);
			}
			// Egress PSC/LSC
			else {
				int outLabel = this.LabeltoLabelIP.get(currentPacket.getLabel()).getLabel();
				outNIC = this.LabeltoLabelIP.get(currentPacket.getLabel()).getNIC();
				currentPacket.setOpticalLabel(OpticalLabel.NA);
				currentPacket.setLabel(outLabel);
				this.printSwitchingTable();
			}

			if (outNIC != nic) {
				outNIC.sendPacket(currentPacket, this);
				if (this.trace) {
					System.out.println("Sending packet " + currentPacket.getTraceID() + " from router " + this.getAddress() + " to " + currentPacket.getDest());

				}
			}
			else {
				if (trace) {
					System.out.println("Packet " + currentPacket.getTraceID() + " reaches the end at " + this.getAddress());
				}
			}
			

		}


	}
	
	/**
	 * This method creates a packet with the specified type of service field and sends it to a destination
	 * @param destination the destination router
	 * @since 1.0
	 */
	public void createPacket(int destination){
		if (this.isStart) {
			if (this.psc) {
				this.calculateNextHop(true);
			}
			if (this.lsc) {
				this.calculateNextHop(false);
			}
			this.isStart = false;
		}

		Packet newPacket= new Packet(this.getAddress(), destination);
		this.sendPacket(newPacket);
	}
	
	/**
	 * This method forwards a packet to the correct nic or drops if at destination router
	 * @param newPacket The packet that has just arrived at the router.
	 * @since 1.0
	 */
	public void sendPacket(Packet newPacket) {
		
		//This method should send the packet to the correct NIC (and wavelength if LSC router).
		//This method should send the packet to the correct NIC.]
		int dest = newPacket.getDest();
		//int DSCP = newPacket.getDSCP();
		//DestDSCPPair pair = new DestDSCPPair(dest, DSCP);
		LSRNIC nic = this.nextHopIP.get(dest);

		if (this.address == 6) {
			System.out.println("6");
		}

		if (this.destToLabel.containsKey(dest) && this.destToLabel.get(dest) != -1) {
			int inLabel = this.destToLabel.get(dest);
			//int outLabel = this.LabeltoLabelIP.get(inLabel).getLabel();
			//newPacket.addMPLSheader(new MPLS(outLabel, 0, 1));
			newPacket.setLabel(inLabel);
			nic.sendPacket(newPacket, this);
			if (this.trace) {
				System.out.println("Sending packet " + newPacket.getTraceID() + " from router " + this.getAddress());
			}
		}

		else if (!this.destToLabel.containsKey(dest)) {
			this.destToLabel.put(dest, -1);
			this.waitList.add(newPacket);

			int label = this.getAvailableIPLabel();

			Packet path = new Packet(this.getAddress(), newPacket.getDest(), label);
			path.setOAM(true, "PATH");
			path.setTraceID(this.getTraceID());
			this.sentPath(path);
			nic.sendPacket(path, this);
			this.LabeltoLabelIP.put(label, new NICLabelPair(nic, -1));	// ######################
			this.LabeltoLabelIP.put(this.destToLabel.get(dest), new NICLabelPair(nic, -1));	// ##################
			this.LabeltoDestLabel.put(label, new destLabelPair(this.address, -1));
			this.LabeltoDestLabel.put(-1, new destLabelPair(dest, -1));

		}
		else {
			this.waitList.add(newPacket);
		}
	}

	/**
	 * This method forwards a packet to the correct nic or drops if at destination router
	 * @param newPacket The packet that has just arrived at the router.
	 * @since 1.0
	 */
	public void sendKeepAlivePackets() {
		
		//This method should send the keep alive packets for routes for each the router is an inbound router
		
	}
	
	/**
	 * Makes each nic move its cells from the output buffer across the link to the next router's nic
	 * @since 1.0
	 */
	public void sendPackets(){
		sendKeepAlivePackets();
		for(int i=0; i<this.nics.size(); i++)
			this.nics.get(i).sendPackets();
	}
	
	/**
	 * Makes each nic move all of its cells from the input buffer to the output buffer
	 * @since 1.0
	 */
	public void receivePackets(){
		for(int i=0; i<this.nics.size(); i++)
			this.nics.get(i).receivePackets();
	}
	
	public void sendKeepAlive(int dest, OpticalLabel label){
			Packet p = new Packet(this.getAddress(), dest, label);
			p.setOAM(true, "KeepAlive");
			this.sendPacket(p);
	}


	/**
	 * Using Dijkstra algorithm to calculate this.nexthop
	 * @since 1.0
	 */
	public void calculateNextHop(boolean isIP){

		HashMap<Integer, ArrayList<Integer>> graph;
		HashMap<Integer, ArrayList<LSRNIC>> nics;
		TreeMap<Integer, LSRNIC> nextHop;

		if (isIP) {
			graph = GraphInfo.graphIP;
			nics = GraphInfo.nicsIP;
			nextHop = this.nextHopIP;
		}
		else {
			graph = GraphInfo.graphOpt;
			nics = GraphInfo.nicsOpt;
			nextHop = this.nextHopOpt;
		}

		int origin = this.getAddress();
		int v0 = 0;		// the origin node
		ArrayList<Integer> nodes = new ArrayList<Integer>();

		for (Integer node : graph.keySet()) {
			nodes.add(node);
			if (node == origin) {
				v0 = nodes.size() - 1;
			}
		}

		int n = nodes.size();
		ArrayList<Integer> dist = new ArrayList<Integer>();	// distance to origin node
		ArrayList<Integer> path = new ArrayList<Integer>();	// path to origin node
		ArrayList<Boolean> visited = new ArrayList<Boolean>();
		ArrayList<LSRNIC> niclist = nics.get(origin);
		ArrayList<Integer> neighborlist = graph.get(origin);

		for (int i = 0; i < n; i ++) {
			dist.add(0);
			path.add(-1);
			visited.add(false);
		}
		for (int i = 0; i < n; i ++) {
			if (neighborlist.contains(nodes.get(i))
					&& nodes.get(i) != origin) {
				dist.set(i, 1);
				path.set(i, v0);
			}
			else {
				dist.set(i, Integer.MAX_VALUE);
				path.set(i, -1);
			}
			visited.set(i, false);
			path.set(v0, v0);
			dist.set(v0, 0);
		}
		visited.set(v0, true);
		for (int i = 1; i < n; i ++) {
			int min = Integer.MAX_VALUE;
			int u = 0;
			for (int j = 0; j < n; j ++) {
				if (visited.get(j) == false && dist.get(j) < min) {
					min = dist.get(j);
					u = j;
				}
			}
			visited.set(u, true);
			for (int k = 0; k < n; k ++) {
				if (visited.get(k) == false
						&& graph.get(nodes.get(u)).contains(nodes.get(k))
						&& min + 1 < dist.get(k)) {
					dist.set(k, min + 1);
					path.set(k, u);
				}
			}
		}

		for (int i = 0; i < n; i ++) {
			if (i == v0) {
				continue;
			}
			int tmp = i;
			while (path.get(tmp) != v0) {
				tmp = path.get(tmp);
			}
			int id;
			for (id = 0; id < neighborlist.size(); id ++) {
				if (neighborlist.get(id) == nodes.get(tmp)) {
					break;
				}
			}

			nextHop.put(nodes.get(i), niclist.get(id));
		}

		return;
	}



	/**
	 * This method returns a sequentially increasing random trace ID, so that we can
	 * differentiate cells in the network
	 * @return the trace id for the next cell
	 * @since 1.0
	 */
	public int getTraceID(){
		int ret = this.traceID;
		this.traceID++;
		return ret;
	}

	/**
	 * Outputs to the console that a PATH message has been sent
	 * @since 1.0
	 */
	private void sentPath(Packet packet){
		if(this.displayCommands)
			System.out.println("Router " +this.address+ " sent a PATH to Router " + packet.getDest());
	}

	/**
	 * Outputs to the console that a PATH message has been received
	 * @since 1.0
	 */
	private void receivedPath(Packet packet){
		if(this.displayCommands)
			System.out.println("Router " +this.address+ " received a PATH from Router " + packet.getSource());
	}

	/**
	 * Outputs to the console that a RESV message has been sent
	 * @since 1.0
	 */
	private void sentResv(Packet packet){
		if(this.displayCommands)
			System.out.println("Router " +this.address+ " sent a RESV to Router " + packet.getDest());
	}

	/**
	 * Outputs to the console that a RESV message has been received
	 * @since 1.0
	 */
	private void receivedResv(Packet packet){
		if(this.displayCommands)
			System.out.println("Router " +this.address+ " received a RESV from Router " + packet.getSource());
	}

	/**
	 * Outputs to the console that a PATHERR message has been sent
	 * @since 1.0
	 */
	private void sentPathErr(Packet packet){
		if(this.displayCommands)
			System.out.println("Router " +this.address+ " sent a PATHERR to Router " + packet.getDest());
	}

	/**
	 * Outputs to the console that a PATHERR message has been received
	 * @since 1.0
	 */
	private void receivedPathErr(Packet packet){
		if(this.displayCommands)
			System.out.println("Router " +this.address+ " received a PATHERR from Router " + packet.getSource());
	}

	/**
	 * Outputs to the console that a RESVERR message has been sent
	 * @since 1.0
	 */
	private void sentResvErr(Packet packet){
		if(this.displayCommands)
			System.out.println("Router " +this.address+ " sent a RESVERR to Router " + packet.getDest());
	}

	/**
	 * Outputs to the console that a RESVERR message has been received
	 * @since 1.0
	 */
	private void receivedResvErr(Packet packet){
		if(this.displayCommands)
			System.out.println("Router " +this.address+ " received a RESVERR from Router " + packet.getSource());
	}

	/**
	 * Outputs to the console that a RESVCONF message has been sent
	 * @since 1.0
	 */
	private void sentResvConf(Packet packet){
		if(this.displayCommands)
			System.out.println("Router " +this.address+ " sent a RESVCONF to Router " + packet.getDest());
	}

	/**
	 * Outputs to the console that a RESVCONF message has been received
	 * @since 1.0
	 */
	private void receivedResvConf(Packet packet){
		if(this.displayCommands)
			System.out.println("Router " +this.address+ " received a RESVCONF from Router " + packet.getSource());
	}

	/**
	 * get first availabel label
	 * @return
	 */
	private int getAvailableIPLabel() {
		int label = 1;

		if (!this.LabeltoLabelIP.isEmpty()) {
			label = this.LabeltoLabelIP.lastKey() + 1;
			for (int i = 1; i < this.LabeltoLabelIP.lastKey(); i ++) {
				if (!this.LabeltoLabelIP.containsKey(i)) {
					label = i;
					break;
				}
			}
		}
		return label;
	}

	/**
	 * get available optical label
	 * @return
	 */
	private OpticalLabel getAvailableOptLabel() {
		if (!this.LabeltoLabelOpt.containsKey(OpticalLabel.red)) return OpticalLabel.red;
		if (!this.LabeltoLabelOpt.containsKey(OpticalLabel.green)) return OpticalLabel.green;
		if (this.LabeltoLabelOpt.containsKey(OpticalLabel.blue)) return OpticalLabel.blue;
		if (this.LabeltoLabelOpt.containsKey(OpticalLabel.yellow)) return OpticalLabel.yellow;
		return OpticalLabel.NA;
	}

	/**
	 *
	 * @param destAddr
	 * @return
	 */
	/*private int getNextPSCAddr(int destAddr) {
		LSRNIC outIPNIC = this.nextHopIP.get(destAddr);
		int nextAddr = this.address;
		int currentAddr = this.address;

		while (GraphInfo.graphOpt.containsKey(nextAddr)) {
			currentAddr = nextAddr;
			ArrayList<Integer> neighbors = GraphInfo.graphIP.get(currentAddr);
			ArrayList<LSRNIC> nics = GraphInfo.nicsIP.get(currentAddr);

			for (int i = 0; i < neighbors.size(); i ++) {
				if (nics.get(i).equals(outIPNIC)) {
					nextAddr = neighbors.get(i);
					break;
				}
			}
		}

		return currentAddr;
	}*/
	private int getNextPSCAddr(int destAddr) {
		LSRNIC outIPNIC = this.nextHopIP.get(destAddr);
		LSR nextLSR = outIPNIC.getNextLSR();
		//LSR currLSR = this;

		while (!nextLSR.isPsc()) {
			if (nextLSR.getNextHopIP().isEmpty()) {
				nextLSR.calculateNextHop(true);
			}
			outIPNIC = nextLSR.getNextHopIP().get(destAddr);
			nextLSR = outIPNIC.getNextLSR();
		}

		return nextLSR.getAddress();
	}

	public boolean isPsc() {
		return psc;
	}

	public boolean isLsc() {
		return lsc;
	}

	public TreeMap<Integer, LSRNIC> getNextHopIP() {
		return this.nextHopIP;
	}

	public TreeMap<Integer, LSRNIC> getNextHopOpt() {
		return this.nextHopOpt;
	}

	private void printSwitchingTable() {
		System.out.println("label to label:");
		for (Integer inLabel : this.LabeltoLabelIP.keySet()) {
			System.out.println(inLabel + "\t" + this.LabeltoLabelIP.get(inLabel).getNIC().getNextLSR().getAddress() + "\t" + this.LabeltoLabelIP.get(inLabel).getLabel());
		}
		System.out.println("opt to opt:");
		for (OpticalLabel inLabel : this.LabeltoLabelOpt.keySet()) {
			System.out.println(inLabel.toString() + "\t" + this.LabeltoLabelOpt.get(inLabel).getNIC().getNextLSR().getAddress() + "\t" + this.LabeltoLabelOpt.get(inLabel).getLabel().toString());
		}
	}

}
