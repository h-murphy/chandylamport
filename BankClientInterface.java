/* BankClientInterface.java
 * March 17, 2017
 * Hannah Murphy and Carly Battaile
 */
import java.util.*;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BankClientInterface extends Remote {
  
  /* receiveTransfer(int transferAmount) 
   * 
   * Called from another computer, receives a transfer, updates amount of money in account, and reports the transfer
   */
  public void receiveTransfer(int transferAmount) throws RemoteException;
  
  /* setNextNode(String nNode) 
   * 
   * Sets the nextNode instance variable to set up the ring topology. This computer will have the nextNode stub in its client map
   */
  public void setNextNode(String nNode) throws RemoteException;
  
  /* receiveAllIps(HashMap<String, BankClientInterface> cMap) 
   * 
   * Called by the initiator to all other clients. Gives a complete HashMap of all the client stubs in the network,including the sender
   */
  public void receiveAllIps(HashMap<String, BankClientInterface> clientMap) throws RemoteException;//, String sender) throws RemoteException;
  
  /* receiveProposedLeader(String p)
   * 
   * Called by the node that is pointing to this one. D
   * Decides whether to keep the proposed leader before passing it on or keeping the one it had before.
   * If it receives itself as the proposed leader, send out a leader confirmation to nextNode
   * 
   */ 
  public void receiveProposedLeader(String proposedLeader) throws RemoteException;
  
  /* receiveConfirmedLeader(String p)
   * 
   * Called by the node that is pointing to this one. 
   * Sets the confirmed leader to whatever was received as an argument
   * If it receives itself as the confirmed leader, stops the leader election process
   * 
   */ 
  public void receiveConfirmedLeader(String confirmedLeader) throws RemoteException;
  
  /* receiveMessage(String message) 
   * 
   * Receive a generic message and return a confirmation
   */
  public String receiveMessage(String message) throws RemoteException;
  
  
}