/* BankClient.java
 * March 17, 2017
 * Hannah Murphy and Carly Battaile
 */

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class BankClient implements BankClientInterface{
  
  
  String self; // holds public DNS of the computer this code is running on
  BankClientInterface selfStub; //holds reference to the computer this code is running on
  
  static HashMap<String, BankClientInterface> clientMap; //holds stubs of all the other clients in the network, referenced by their public DNS
  
  int numClients; //number of clients in the network
  String nextNode; //holds referencde to the next node in the ring topology
  
  String proposedLeader; //public DNS of the proposed leader
  String confirmedLeader; //public DNS of the confirmed leader
  boolean selfPotentialLeader; //true if this computer is the potential leader
  boolean selfConfirmedLeader; //true if this computer is the confirmed leader
  
  int amount; //money stored in account

  public BankClient(String ip){
    self = ip;
    clientMap = new HashMap<String, BankClientInterface>();
    amount = 200;
    proposedLeader = self; //at first, every computer thinks it is the proposed leader
  }


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
   * Set the proposedLeader string (called from the main method)
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
  
  /*receiveMarker(String sender)
   * 
   * If this is the first marker received by a process, save state locally. Send out Markers to everyone else. Mark channel from sender as empty. Also start recording on all other channels.
   * If this is not the first time, stop recording, save state of channel, mark channel from sender to itself as empty. 
   */ 
  public void receiveMarker(String sender) throws RemoteException{
  }
  
  /* getSavedState(String sender, int amount)
   * 
   * Once all channels are closed, the leaader will call getSavedState() to retrieve the states of the other processes. 
   */ 
  public String getSavedState(String sender, int amount) throws RemoteException{
    return "";
  }
  
  ////////////// TRANSFER METHODS //////////////////////////////

 /* initiateRandomTransfer() 
   * 
   * Begins one transfer of a random amount sent to a random process after sleeping a random # of milliseconds
   */ 
  public void initiateRandomTransfer() {
    Random rand = new Random();
    int r = rand.nextInt(45001) + 5000;
    int m = rand.nextInt(amount) + 1;
    int p = rand.nextInt(4);
    ArrayList<String> clientArray = new ArrayList<String>(clientMap.keySet());
    try {
      Thread.sleep(r);
    } catch (InterruptedException e) {
      System.out.println("Random transfers interrupted.");
    }
    if (amount > 0) {
      sendTransfer(m, clientArray.get(p));
    }
  }
  
  
  /* receiveTransfer(int transferAmount) 
   * 
   * Called from another computer, receives a transfer, updates amount of money in account, and reports the transfer
   * Then initiates random transfer to another process
   */ 
  public void receiveTransfer(int transferAmount) throws RemoteException{
    amount += transferAmount;
    System.out.println("Received Transfer of " + transferAmount +". Current Balance is " + amount);
    initiateRandomTransfer();
  }
  
  /* sendTransfer(int transferAmount, String receiver) 
   * 
   * Initiates a transfer of transferAmount to the computer denoted by receiver. Updates current account amount
   */ 
  public void sendTransfer(int transferAmount, String receiver){
    try{
      
      clientMap.get(receiver).receiveTransfer(transferAmount); //call receiveTransfer on the stub that is receiving the transfer
      amount -= transferAmount;
      System.out.println("Sent Transfer of " + transferAmount +". Current Balance is " + amount);
      
    }catch(Exception e){
      System.out.println("Failed to send transfer of $" + transferAmount + " to " + receiver);
      System.out.println(e);
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
        BankClientInterface stub = (BankClientInterface) registry.lookup("Self");
        
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
        clientMap.get(key).receiveAllIps(toSend);

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
        
        last = nextRemoteNode; // update the last node
      }
      
      clientMap.get(nextRemoteNode).setNextNode(self);//set last node next to be self to complete the ring
      System.out.println("Set " + nextRemoteNode + " --> " + self);
      
      clientMap.get(nextNode).receiveProposedLeader(self); //kick off leader election
      
    }catch(Exception e){
      System.out.println("Self: " + self);
      System.out.println("Next: " + nextNode);//instance var
      System.out.println(e);
    }
      
  }
  
  /* receiveAllIps(HashMap<String, BankClientInterface> cMap) 
   * 
   * Called by the initiator to all other clients. Gives a complete HashMap of all the client stubs in the network,including the sender
   */
  public void receiveAllIps(HashMap<String, BankClientInterface> cMap){
    clientMap = cMap; //sets clientMap instance var
    clientMap.remove(self); //removes itself from the complete list of clients
    
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
    
    
    if(proposedLeaderDigits > pDigits){ // don't change the leader from local 
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
  public void receiveConfirmedLeader(String p) throws RemoteException{
    System.out.println("Received Confirmed Leader: " + p);
    
    if(p.equals(self)){ // if this computer is the leader
      
      System.out.println("I am the confirmed leader!: " + p);
      selfConfirmedLeader = true;
      confirmedLeader = p;
   
      System.out.println("Stopped leader election: " + self);

      initiateRandomTransfer();
      
    }else{
      confirmedLeader = p;
      clientMap.get(nextNode).receiveConfirmedLeader(confirmedLeader); 
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
    
    //ping leader to make sure everythng has been done before starting transfers
    
  }
}