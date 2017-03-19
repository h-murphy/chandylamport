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
 static HashMap<String, BankClientInterface> clientMap;
  //static LinkedList<String> ring;
  int numClients;
  String nextNode; 
  
 public BankClient(String ip){
  self = ip;
  clientMap = new HashMap<String, BankClientInterface>();
  //ring = new LinkedList<String>();
 }
 
 public void setSelf(String s){
   self = s;
 }
 
 public void receiveAllIps(HashMap<String, BankClientInterface> cMap){
   clientMap = cMap;
   clientMap.remove(self);
   
   printConnections();
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
   
   
   for(int i = 0; i < ips.length; i++){ //does it add itself?
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
   
   Iterator<String> keys = clientMap.keySet().iterator();
   System.out.println("Iterating through keys: ");
   
   while(keys.hasNext()){
         try {
           
             //Registry registry = LocateRegistry.getRegistry(ips[i]);
             //BankClientInterface stub = (BankClientInterface) registry.lookup("Self");
             //String response = stub.receiveMessage("Connected to: " + self);
           
           clientMap.get(keys.next()).receiveAllIps(clientMap);
             //System.out.println("response: " + response);
             
             //lientMap.put(ips[i], stub);
             //numClients++;
         } catch (Exception e) {
             System.err.println("Client exception: " + e.toString());
             e.printStackTrace();
         }
         
         
     }
   try{
   Iterator<String> ringkeys = clientMap.keySet().iterator();
   System.out.println("Iterating through keys: ");
   String last = self;
   while(ringkeys.hasNext()){
     String nextNode = ringkeys.next();
     clientMap.get(last).setNextNode(nextNode);
     System.out.println("Set " + last + " --> " + nextNode);
     last = nextNode;
   }
   clientMap.get(nextNode).setNextNode(self);//set last node next to be self
   System.out.println("Set " + nextNode + " --> " + last);
   
   }catch(Exception e){
     System.out.println("Self: " + self);
     System.out.println("Next: " + nextNode);
     System.out.println(e);
   }
   
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
        BankClientInterface selfStub = (BankClientInterface) UnicastRemoteObject.exportObject(obj, 0);
      
        // Bind the remote object's stub in the registry
        Registry registry = LocateRegistry.getRegistry();
        registry.bind("Self", selfStub);
      
        System.err.println("Server ready");
      
     
        obj.setSelf(args[0]);
  
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

}
}