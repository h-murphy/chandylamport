/* BankClient.java
 * March 17, 2017
 * Hannah Murphy and Carly Battaile
 */

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.io.*;

public class BankClient implements BankClientInterface{

  //FOR TESTING WITHOUT TRANSFERS: set to true to enable transfers
  boolean TRANSFER = true;


  Timer time; //timer used to delay transfers
  Scanner scan; //scanner used for command line input
  
  String self; // holds IP/public DNS of the computer this code is running on
  BankClientInterface selfStub; //holds stub reference from registry to the computer this code is running on
  
  static HashMap<String, BankClientInterface> clientMap; //holds stubs of all the other clients in the network, referenced by their public DNS
  
  int numClients; //number of clients in the network
  String nextNode; //holds reference to the next node in the ring topology
  
  String proposedLeader; //public DNS of the proposed leader
  String confirmedLeader; //public DNS of the confirmed leader
  boolean selfPotentialLeader; //true if this computer is the potential leader
  boolean selfConfirmedLeader; //true if this computer is the confirmed leader
  
  int localState; //holds initial state of process when snapshot is triggered
  HashMap<String, Boolean> recordChannel; //false = channel is open/record, true = channel is closed/don't record
  boolean takingSnapshot; //snapshot occurring?
  boolean hasReceivedMarker; //checks if first marker has been received by given machine
  HashMap<String, ArrayList<Integer>> channels; //holds transfers that have come in to channels since original state was recorded  
  
  int amount; //local balance 

  public BankClient(String ip){
    self = ip;
    clientMap = new HashMap<String, BankClientInterface>();
    amount = 200;
    proposedLeader = self; //at first, every computer thinks it is the proposed leader
    hasReceivedMarker = false;
    takingSnapshot = false;
    time = new Timer();
    scan = new Scanner(System.in);
  }

  //////////// SUBCLASSES /////////////////////////////////

  /* 
   * SendTransfer
   * Extends TimerTask to be called by timer during random transfers
   */
  public class SendTransfer extends TimerTask {

    String i;

    public SendTransfer(String ip) {
      i = ip;
    }

    public void run() {
      sendTransfer(i); //calls sendTransfer from current machine to machine with public DNS i
    }

  }
  //////////////////////////////////////////////////////////


  /////////////// SETTERS //////////////////////////////////
  
  /* setSelfStub(BankClientInterface selfSt)
   * 
   * Set the self stub (called from the main method)
   */ 
  public void setSelfStub(BankClientInterface selfSt){
    selfStub = selfSt;
  }
  
  /* setSelf(BankClientInterface selfSt)
   * 
   * Set the self string (called from the main method)
   */ 
  public void setSelf(String s){
    self = s;
  }
  
  /* setProposedLeader(String s)
   * 
   * Set the proposedLeader string
   */ 
  public void setProposedLeader(String s){
    proposedLeader = s;
  }
  
  /* setNextNode(String nNode) 
   * 
   * Sets the nextNode instance variable to set up the ring topology. This computer wil have the nextNode stub in its client map
   */
  public void setNextNode(String nNode){
    nextNode = nNode;
    
    System.out.println("NEXT IS: " + nextNode);
  }
  //////////////////////////////////////////////////////////////

  ///////////SNAPSHOT METHODS/////////////////////
  
  /*takeSnapshot()
   * 
   * Called by the leader to start the Chandy-Lamport algoritm. Begins by saves its local amount value and sends markers to all other processes. 
   * Begins recording all incoming channels.
   * 
   */ 
  public void takeSnapshot() throws RemoteException{
    System.out.println("Initiated Snapshot");
    localState = amount; //record state of machine - ie. balance - at time of snapshot
    
    takingSnapshot = true; 
    hasReceivedMarker = true;
    
    Iterator<String> keys = clientMap.keySet().iterator();
    
    // sends marker to all clients
    while(keys.hasNext()){
      String key = keys.next();
      
      try {
        System.out.println("Sending marker from " + self + " to " + clientMap.get(key));
        clientMap.get(key).receiveMarker(self);
        
      } catch (Exception e) {
        System.err.println("Failed to send Marker to:  " + key);
        System.err.println("Client exception: " + e.toString());
        e.printStackTrace();
      }
    }
    
    
  }
  
  /*receiveMarker(String sender)
   * 
   * If this is the first marker received by a process, save state locally. Send out Markers to everyone else. Mark channel from sender as empty. Also start recording on all other channels.
   * If this is not the first time, stop recording, save state of channel, mark channel from sender to itself as empty. 
   */ 
  public void receiveMarker(String sender) throws RemoteException{
    if(!hasReceivedMarker){ //not received marker before
      System.out.println("Received first marker");
      hasReceivedMarker = true;
      localState = amount;
      takingSnapshot = true;
      
      recordChannel.remove(sender);
      recordChannel.put(sender, true); //mark channel as closed from sender --> this computer, stop recording on this channel
      System.out.println("Closed channel from " + sender + " to " + self);
      
      Iterator<String> keys = clientMap.keySet().iterator();
      
      // sends marker to all clients
      while(keys.hasNext()){
        String key = keys.next();
        
        try {
          System.out.println("Sending marker from " + self + " to " + clientMap.get(key));
          clientMap.get(key).receiveMarker(self);
          
        } catch (Exception e) {
          System.err.println("Failed to send Marker to:  " + key);
          System.err.println("Client exception: " + e.toString());
          e.printStackTrace();
        }
      }
      
    }else{

      recordChannel.remove(sender);
      recordChannel.put(sender, true); //mark incoming channel as closed, stop recording on it
      System.out.println("Closed channel from " + sender + " to " + self);

      try{
      if(selfConfirmedLeader && allChannelsClosed()){ //write results to file
        System.out.println("All channels have been closed: print to file");
        File file = new File("snapshotOutput.txt");
        PrintWriter writeStates = new PrintWriter(file);
        Iterator<String> keys = clientMap.keySet().iterator();
        
        //write local state for each other non-leader machine
        while(keys.hasNext()){
          String key = keys.next();
          
          try {
            String remoteLocalState = clientMap.get(key).getSavedState();
            //String state = key + ", $" + remoteLocalState;
            
            writeStates.println(remoteLocalState);
            
          } catch (Exception e) {
            System.err.println("Failed to retrieve state from:  " + key);
            System.err.println("Client exception: " + e.toString());
            System.err.println("File Writning IO Exception Possible");
            e.printStackTrace();
          }
          
        }

        //write state for leader
        writeStates.println(getSavedState());
        System.out.println("Snapshot printed to file");
        writeStates.close();
      }
    }catch(FileNotFoundException e){
      System.err.println("File not found" + e.toString());
    }
  }
}

  /* allChannelsClosed()
   * 
   * If all channels are closed, returns true 
   */ 
  public boolean allChannelsClosed(){
    Iterator<String> keys = recordChannel.keySet().iterator();
    
    //checks each channel for any open ones
    while(keys.hasNext()){
      String key = keys.next();
      if(!recordChannel.get(key)){
        return false;
        
      }
    }
    return true;
  }

  /* getSavedState()
   * 
   * Once all channels are closed, the leaader will call getSavedState() to retrieve the states of the other processes. 
   * //IP_self: $127;
   * //Sender: IP_other Transfers: $15 $92
   */ 
  public String getSavedState() throws RemoteException{
   String s = self + ": $" + localState + "; ";
   Iterator<String> keys = recordChannel.keySet().iterator();

    // prints transfers recorded during snapshot from each other machine
   while(keys.hasNext()){
    String key = keys.next();
    ArrayList<Integer> temp = channels.get(key);

    s += "\n\t Sender: " + key + "\t Transfers: " ;
    for(int i = 0; i < temp.size(); i ++){
      s += "$" + temp.get(i) + "\t";
    } 
  }
  return s;
}
  //////////////////////////////////////////////////////////////

  ////////////// TRANSFER METHODS //////////////////////////////

 /* startAllTransfers() 
   * 
   * Begins all transfers by sending random transfer to each other machine
   */ 
 public void startAllTransfers() {
  Iterator<String> keys = clientMap.keySet().iterator();

  while(keys.hasNext()){
    String key = keys.next();
    sendTransfer(key);
  }
}

 /* initiateRandomTransfer() 
   * 
   * Begins one transfer sent to a random process after sleeping a random # of milliseconds
   */ 
 public void initiateRandomTransfer() {

    //ifacquired
  Random rand = new Random();
  int r = rand.nextInt(45001) + 5000;
  int p = rand.nextInt(4);
  ArrayList<String> clientArray = new ArrayList<String>(clientMap.keySet());
  TimerTask send = new SendTransfer(clientArray.get(p));
  time.schedule(send,r);
}


  /* receiveTransfer(String sender, int transferAmount) 
   * 
   * Called from another computer, receives a transfer, updates amount of money in account, and reports the transfer
   * Then initiates random transfer to another process
   * If a snapshot is being taken and the transfer is coming from an open channel, record the transfer as part of the snapshot
   */ 
  public void receiveTransfer(String sender, int transferAmount) throws RemoteException{

    //if snapshot is on and channel is recording, record transfer
    if(takingSnapshot && recordChannel.get(sender) == false){
     ArrayList<Integer> temp = channels.get(sender);
     temp.add(transferAmount);
     channels.remove(sender);
     channels.put(sender, temp);
   }

   amount += transferAmount; //update local balance
   System.out.println("Received Transfer of " + transferAmount +". Current Balance is " + amount);
   initiateRandomTransfer(); //start a transfer to another machine
 }


  /* sendTransfer(String receiver) 
   * 
   * Initiates a transfer of random transfer amount to the computer denoted by receiver. Updates current account balance.
   * If there is no money left in the account, it will try to initiate another transfer.
   */ 
  public void sendTransfer(String receiver){
    Random rand = new Random();
    int transferAmount;
    if (amount > 0) { //check to see if account has money
      transferAmount = rand.nextInt(amount) + 1;
    } else {
      System.out.println("No money left in account, transferring zero dollars.");
      transferAmount = 0;
    }
    try{
      amount -= transferAmount; //update local balance
      System.out.println("Sent Transfer of " + transferAmount +". Current Balance is " + amount);
      
      clientMap.get(receiver).receiveTransfer(self, transferAmount); //call receiveTransfer on the stub that is receiving the transfer
      
    }catch(Exception e){
      System.out.println("Failed to send transfer of $" + transferAmount + " to " + receiver);
      System.out.println(e);
      initiateRandomTransfer();
    }
  }
  
  //////////////////////////////////////////////////////////////
  
  /////////////////////// INITILIZATION METHODS////////////////////
  
  /* initializeWithArguments(String[] ips) 
   * 
   * Takes in a string array of public DNS's and creates references to all of them. 
   * Stores references in a Hashmap and sends the hashmap to all of its new connections. 
   * Sets up the ring topology by assigning each stub a "next" node, so when it sends a message it send it to the next in line. 
   * After this method is executed, all computers iin the ips array and this computer are connected to each other. 
   * Initiates leader election
   * 
   * NOTE: this method is only called by the INITIATOR, the computer that receives all the ips by command line argument
   */
  public void initializeWithArguments(String[] ips){

    // creates stubs to all received clients and places them in a hashmap
    for(int i = 1; i < ips.length; i++){  //starts at 1 so it doesn't add itself to the client map
    try {

      Registry registry = LocateRegistry.getRegistry(ips[i]);
      BankClientInterface stub = (BankClientInterface) registry.lookup("Self"); //search for stub of target machine in its local registry

        String response = stub.receiveMessage("Connected to: " + self); //confirm connection by sending a message
        System.out.println("response: " + response);
        
        clientMap.put(ips[i], stub);
        numClients++;
        
      } catch (Exception e) {
        System.err.println("Failed to connect to stub:" + ips[i]);
        System.err.println("Client exception: " + e.toString());
        e.printStackTrace();
      }
      
    }
    
    //make a temp HashMap and put self in it to send to everyone ese in the network
    HashMap<String, BankClientInterface> toSend = clientMap;
    toSend.put(self, selfStub);
    
    
    Iterator<String> keys = clientMap.keySet().iterator();
    System.out.println("Iterating through keys: ");
    
    // send the HashMap of all clients to every stub in network
    while(keys.hasNext()){
      String key = keys.next();
      
      try {
        clientMap.get(key).receiveAllIps(toSend); //remotely sends hashMap to all other machines

      } catch (Exception e) {
        System.err.println("Failed to send IPs to " + key);
        System.err.println("Client exception: " + e.toString());
        e.printStackTrace();
      }
    }
    
    
    // create the ring topology for leader election
    String nextRemoteNode = "";
    try{

      Iterator<String> ringkeys = clientMap.keySet().iterator();
      System.out.println("Iterating through keys: ");
      String last = self;
      
      while(ringkeys.hasNext()){
        nextRemoteNode = ringkeys.next();
        
        if(!last.equals(self)){
          clientMap.get(last).setNextNode(nextRemoteNode);
          System.out.println("Set " + last + " --> " + nextRemoteNode);
          
        }else{ // the first time, set the instance var nextNode to be the next of self
          nextNode = nextRemoteNode;
          System.out.println("Set " + last + " --> " + nextRemoteNode);
          
        }
        
        last = nextRemoteNode; //update the last node
      }
      
      clientMap.get(nextRemoteNode).setNextNode(self);//set last node next to be self to complete the ring
      System.out.println("Set " + nextRemoteNode + " --> " + self);
      
      clientMap.get(nextNode).receiveProposedLeader(self); //kick off leader election
      
    }catch(Exception e){
      System.out.println("Self: " + self);
      System.out.println("Next: " + nextNode); //instance var
      System.out.println(e);
    }

  }
  
  /* receiveAllIps(HashMap<String, BankClientInterface> cMap) 
   * 
   * Called by the initiator to all other clients. Gives a complete HashMap of all the client stubs in the network, including the sender
   */
  public void receiveAllIps(HashMap<String, BankClientInterface> cMap){
    clientMap = cMap; //sets clientMap instance var
    clientMap.remove(self); //removes itself from the complete list of clients
    
    recordChannel = new HashMap<String, Boolean>();
    channels = new HashMap<String, ArrayList<Integer>>();

    Iterator<String> keys = clientMap.keySet().iterator();
    
    // sends marker to all clients
    while(keys.hasNext()){
      String key = keys.next();
      recordChannel.put(key, false); //marking all channels as false (aka open) because we have not received markers from them
      channels.put(key, new ArrayList<Integer>());
    }


    printConnections(); // print connections locally
    printMessageToAllConnections(); //confirm connections by sending a message to all connections
  }
  
  ////////////////////////////////////////////////////////////////////
  
  
  //////////////////// LEADER ELECTION METHODS ///////////////////////
  
  /* receiveProposedLeader(String p)
   * 
   * Called by the node that is pointing to this one. D
   * Decides whether to keep the proposed leader before passing it on or keeping the one it had before.
   * If it receives itself as the proposed leader, send out a leader confirmation to nextNode
   * 
   */ 
  public void receiveProposedLeader(String p) throws RemoteException{

    System.out.println("Received Proposed Leader: " + p);
    double proposedLeaderDigits = Double.parseDouble(proposedLeader.replaceAll("[^\\d]", ""));
    double pDigits = Double.parseDouble(p.replaceAll("[^\\d]", ""));
    
    
    if(proposedLeaderDigits < pDigits){ // don't change the leader from local 
    System.out.println("Proposed Leader is less than argument. Proposed Leader: " + p);
    proposedLeader = p;
    clientMap.get(nextNode).receiveProposedLeader(proposedLeader);
    System.out.println("New Proposed Leader: " + proposedLeader);

    }else if(p.equals(self)){ //if this computer is the leader
      System.out.println("I am the leader!: " + p);
      selfPotentialLeader = true;
      clientMap.get(nextNode).receiveConfirmedLeader(self);
      System.out.println("Sent confirmed leader message: " + proposedLeader);
      
    }else{ //change the local leader
      clientMap.get(nextNode).receiveProposedLeader(proposedLeader);
    }
  }
  
  /* receiveConfirmedLeader(String p)
   * 
   * Called by the node that is pointing to this one. 
   * Sets the confirmed leader to whatever was received as an argument
   * If it receives itself as the confirmed leader, stops the leader election process
   * Initiates random transfer chain once the leader election halts
   * 
   */ 
  public void receiveConfirmedLeader(String p) throws RemoteException {
    System.out.println("Received Confirmed Leader: " + p);
    
    if(p.equals(self)){ // if this computer is the leader

      System.out.println("I am the confirmed leader!: " + p);
      selfConfirmedLeader = true;
      confirmedLeader = p;

      System.out.println("Stopped leader election: " + self);

      //transfer unless boolean is disabled for testing
      if (TRANSFER) {
        startAllTransfers();
      }
      
      //take a snapshot when 'snapshot' entered into leader's terminal via command line
      if (scan.nextLine().equals("snapshot")) {
        takeSnapshot();
      }
      
    }else{
      confirmedLeader = p; //set self as confirmed leader
      clientMap.get(nextNode).receiveConfirmedLeader(confirmedLeader); //inform all other machines that self is the confirmed leader
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  
  ////////// CONNECTION CONFIRMATION METHODS////////////////////
  
  /* printMessageToAllConnections() 
   * 
   * Iterates through HashMap of Clients and sends messages to all of them. 
   */
  public void printMessageToAllConnections(){
    Iterator<String> keys = clientMap.keySet().iterator();
    System.out.println("Iterating through keys: ");
    
    while(keys.hasNext()){
      try{

        clientMap.get(keys.next()).receiveMessage("YOU ARE CONNECTED TO " + self); //print "YOU ARE CONNECTED TO <this computer> on every connection's console
        
      }catch(Exception e){
        System.out.println(e);
      }
    }
  }
  
  /* printConnections() 
   * 
   * Iterates through HashMap of clients and prints all their names locally
   */
  public void printConnections(){
    Iterator<String> keys = clientMap.keySet().iterator();
    
    System.out.println("Iterating through keys: ");
    
    while(keys.hasNext()){

      System.out.println(keys.next());
      
    }
    
  }
  
  /* receiveMessage(String message) 
   * 
   * Receive a generic message and return a confirmation
   */
  public String receiveMessage(String message){
    System.out.println(message);
    return "confirm";
  }

  /////////////////////////////////////////////////////////////////////////////


  /////////////////// main ////////////////////////////////////////////////////

  public static void main(String[] args){
    // ssh -i AmazonKeys/RMI-Tutorial.pem ec2-user@ec2-54-144-209-88.compute-1.amazonaws.com
    // ec2-54-196-27-232.compute-1.amazonaws.com

    String ip = args[0];
    try {
      BankClient obj = new BankClient(ip);
      BankClientInterface mainStub = (BankClientInterface) UnicastRemoteObject.exportObject(obj, 0);
      
      // Bind the remote object's stub in the registry
      Registry registry = LocateRegistry.getRegistry();
      registry.bind("Self", mainStub);
      
      System.err.println("Server ready");
      
      obj.setSelf(args[0]);
      obj.setSelfStub(mainStub);
      
      if(args[1] != null){ //if this computer is the initializer
        obj.initializeWithArguments(args);
        
      }else{
        System.out.println("No arguments");
      }
    } catch (Exception e) {
      System.err.println("Server exception: " + e.toString());
      e.printStackTrace();
    }
    
    //ping leader to make sure everything has been done before starting transfers
    
  }
}