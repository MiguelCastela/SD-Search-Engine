package search ;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;


/**
 * @authors
 * Miguel Castela 2022212972 👍
 * Miguel Martins 2022213951 👍
 */


/**
 * This interface defines the methods that the ClientService class must implement.
 * It is used to communicate with the client and update its stats.
 */

public interface ClientServiceInterface extends Remote {
    /**
     * Set the id of the client
     * @param id
     */
    void setId(int id) throws RemoteException;
    /**
     * Get the id of the client
     * @return the id of the client
     */
    int getId() throws RemoteException;
    /**
     * Set the stats of the client
     * @param top10
     * @param averageTimes
     * @param activeBarrels
     * @param barrelSize
     */
    void setStats(List<String> top10, List<Double> averageTimes, List<BarrelInterface> activeBarrels, List<Integer> barrelSize) throws RemoteException;
    
    /**
     * Returns the top10 
     * @return
     */
    public Map<String, Object> getAllStats() throws RemoteException;

}