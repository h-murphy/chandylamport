/* BankClient.java
 * March 17, 2017
 * 
 */

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class BankClient implements BankClientInterface{
 String self;
 BankClientInterface selfStub;
 static HashMap<String, BankClientInterface> clientMap;
  //static LinkedList<String> ring;
  int numClients;
  String nextNode; 
  int amount;
  String proposedLeader;
  String confirmedLeader;
  
  boolean selfPotentialLeader;
  boolean selfConfirmedLeader;
  
 public BankClient(String ip){
  self = ip;
  clientMap = new HashMap<String, BankClientInterface>();
  amount = 200;
  proposedLeader = self;
  //ring = new LinkedList<String>();
 }
 
 public void setSelfStub(BankClientInterface selfSt){
   selfStub = selfSt;
 }
 
 public void receiveTransfer(int transferAmount) throws RemoteException{
   amount += transferAmount;
   System.out.println("Received Transfer of " + transferAmount +". Current Balance is " + amount);
 }
 
 public void sendTransfer(int transferAmount, String receiver){
   try{
   amount -= transferAmount;
   clientMap.get(receiver).receiveTransfer(transferAmount);
   System.out.println("Sent Transfer of " + transferAmount +". Current Balance is " + amount);
   }catch(Exception e){
     System.out.println(e);
   }
 }
 
 public void setSelf(String s){
   self = s;
 }
 
 public void receiveAllIps(HashMap<String, BankClientInterface> cMap){//, String sender){
   clientMap = cMap;
   clientMap.remove(self);
   
//   try {
//           
//     Registry registry = LocateRegistry.getRegistry(sender);
//     BankClientInterface stub = (BankClientInterface) registry.lookup("Self");
//     String response = stub.receiveMessage("Connected to: " + self);
//     System.out.println("response: " + response);
//             
//     clientMap.put(sender, stub);
//     numClients++;
//         } catch (Exception e) {
//             System.err.println("Client exception: " + e.toString());
//             e.printStackTrace();
//         }
   
   printConnections();
   printMessageToAllConnections();
 }
 
 public void printMessageToAllConnections(){
   Iterator<String> keys = clientMap.keySet().iterator();
   System.out.println("Iterating through keys: ");
   
   while(keys.hasNext()){
        //System.out.println(keys.next());
     try{
        clientMap.get(keys.next()).receiveMessage("YOU ARE CONNECTED TO " + self);
     }catch(Exception e){
       System.out.println(e);
     }
     }
 }

 public void printConnections(){
   Iterator<String> keys = clientMap.keySet().iterator();
   System.out.println("Iterating through keys: ");
   
   while(keys.hasNext()){
        System.out.println(keys.next());
         
     }
   
 }
 public void initialize(HashMap<String, BankClientInterface> cMap){
  clientMap = cMap;
 }
 
 public void setNextNode(String nNode){
   nextNode = nNode;
   System.out.println("NEXT IS: " + nextNode);
 }

 public void initializeWithArguments(String[] ips){
   
   //working on its own hashmap, does not add itself
   for(int i = 1; i < ips.length; i++){ //does it add itself?
         try {
           
             Registry registry = LocateRegistry.getRegistry(ips[i]);
             BankClientInterface stub = (BankClientInterface) registry.lookup("Self");
             String response = stub.receiveMessage("Connected to: " + self);
             System.out.println("response: " + response);
             
             clientMap.put(ips[i], stub);
             numClients++;
         } catch (Exception e) {
             System.err.println("Client exception: " + e.toString());
             e.printStackTrace();
         }
         
     }
   HashMap<String, BankClientInterface> toSend = clientMap;
   toSend.put(self, selfStub);
   
   Iterator<String> keys = clientMap.keySet().iterator();
   System.out.println("Iterating through keys: ");
   
   while(keys.hasNext()){
         try {
           
             //Registry registry = LocateRegistry.getRegistry(ips[i]);
             //BankClientInterface stub = (BankClientInterface) registry.lookup("Self");
             //String response = stub.receiveMessage("Connected to: " + self);
           
           clientMap.get(keys.next()).receiveAllIps(toSend);//, self);
             //System.out.println("response: " + response);
             
             //lientMap.put(ips[i], stub);
             //numClients++;
         } catch (Exception e) {
             System.err.println("Client exception: " + e.toString());
             e.printStackTrace();
         }
         
         
     }
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
     }else{
       //clientMap.get(last).setNextNode(nextNode);
       nextNode = nextRemoteNode;
       System.out.println("Set " + last + " --> " + nextRemoteNode);
     }
     last = nextRemoteNode;
   }
   clientMap.get(nextRemoteNode).setNextNode(self);//set last node next to be self
   System.out.println("Set " + nextRemoteNode + " --> " + self);
   
   clientMap.get(nextNode).receiveProposedLeader(self);
   
   }catch(Exception e){
     System.out.println("Self: " + self);
     System.out.println("Next: " + nextNode);//instance var
     System.out.println(e);
   }
   
   //when we know all have their next assigned

   
   
   
   
 }
 
 public void receiveProposedLeader(String p) throws RemoteException{
   System.out.println("Received Proposed Leader: " + p);
   double proposedLeaderDigits = Double.parseDouble(proposedLeader.replaceAll("[^\\d]", ""));
   double pDigits = Double.parseDouble(p.replaceAll("[^\\d]", ""));

   
   if(proposedLeaderDigits > pDigits){
    System.out.println("Proposed Leader is less than argument. Proposed Leader: " + p);
    proposedLeader = p;
    clientMap.get(nextNode).receiveProposedLeader(proposedLeader);
    System.out.println("New Proposed Leader: " + proposedLeader);
   }else if(p.equals(self)){
     System.out.println("I am the leader!: " + p);
     selfPotentialLeader = true;
     clientMap.get(nextNode).receiveConfirmedLeader(self);
     System.out.println("Sent confirmed leader message: " + proposedLeader);
   }else{
   clientMap.get(nextNode).receiveProposedLeader(proposedLeader);
   }
 }
 
 public void receiveConfirmedLeader(String p) throws RemoteException{
   System.out.println("Received Confirmed Leader: " + p);
   
   if(p.equals(self)){
     System.out.println("I am the confirmed leader!: " + p);
     selfConfirmedLeader = true;
     confirmedLeader = p;
     //clientMap.get(nextNode).receiveConfirmedLeader(self);
     System.out.println("Stopped leader election: " + self);
   }else{
     confirmedLeader = p;
     clientMap.get(nextNode).receiveConfirmedLeader(confirmedLeader);
   }
 }

 public void setProposedLeader(String s){
   proposedLeader = s;
 }
 
 
 public String receiveMessage(String message){
     System.out.println(message);
     return "confirm";
 }
 
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
        //obj.setProposedLeader(args[0]);
  
  if(args[1] != null){
  //String host = (args.length < 1) ? null : args[0];
    obj.initializeWithArguments(args);
    //System.out.println("Next is: " + next);

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