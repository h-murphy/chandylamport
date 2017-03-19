/* BankClientInterface.java
 * March 17, 2017
 * 
 */
import java.util.*;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BankClientInterface extends Remote {

 /* receiveProposedLeader(String proposedLeader)
  *
  */
 //public void receiveProposedLeader(String proposedLeader) throws RemoteException;

 /* receiveConfirmedLeader(String confirmedLeader)
  *
  */
 //public void receiveConfirmedLeader(String confirmedLeader) throws RemoteException;
 public String receiveMessage(String message) throws RemoteException;
 
 public void receiveTransfer(int transferAmount) throws RemoteException;

 public void setNextNode(String nNode) throws RemoteException;
 
 public void receiveAllIps(HashMap<String, BankClientInterface> clientMap) throws RemoteException;//, String sender) throws RemoteException;
 
 public void receiveProposedLeader(String proposedLeader) throws RemoteException;
 
 public void receiveConfirmedLeader(String confirmedLeader) throws RemoteException;

}