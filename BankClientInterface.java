/* BankClientInterface.java
 * March 17, 2017
 * Hannah Murphy and Carly Battaile
 */
import java.util.*;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BankClientInterface extends Remote {
  
  /*receiveMarker(String sender)
   * 
   * If this is the first marker received by a process, save state locally. Send out Markers to everyone else. Mark channel from sender as empty. Also start recording on all other channels.
   * If this is not the first time, stop recording, save state of channel, mark channel from sender to itself as empty. 
   */ 
  public void receiveMarker(String sender) throws RemoteException;
  
  /* getSavedState()
   * 
   * Once all channels are closed, the leaader will call getSavedState() to retrieve the states and transfers of the other processes. 
   */ 
  public String getSavedState() throws RemoteException;
  
  
  /* receiveTransfer(String sender, int transferAmount) 
   * 
   * Called from another computer, receives a transfer, updates amount of money in account, and reports the transfer
   */
  public void receiveTransfer(String sender, int transferAmount) throws RemoteException;
  
  /* setNextNode(String nNode) 
   * 
   * Sets the nextNode instance variable to set up the ring topology. This computer will have the nextNode stub in its client map
   */
  public void setNextNode(String nNode) throws RemoteException;
  
  /* receiveAllIps(HashMap<String, BankClientInterface> clientMap) 
   * 
   * Called by the initiator to all other clients. Gives a complete HashMap of all the client stubs in the network,including the sender
   */
  public void receiveAllIps(HashMap<String, BankClientInterface> clientMap) throws RemoteException;
  
  /* receiveProposedLeader(String proposedLeader)
   * 
   * Called by the node that is pointing to this one. D
   * Decides whether to keep the proposed leader before passing it on or keeping the one it had before.
   * If it receives itself as the proposed leader, send out a leader confirmation to nextNode
   * 
   */ 
  public void receiveProposedLeader(String proposedLeader) throws RemoteException;
  
  /* receiveConfirmedLeader(String confirmedLeader)
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