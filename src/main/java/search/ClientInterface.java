package search ;

import java.rmi.RemoteException;
import java.util.List;
import java.rmi.Remote;

/**
 * @authors
 * Miguel Castela 2022212972 👍
 * Miguel Martins 2022213951 👍
 */


/**
 * This interface defines the methods that the ClientService class must implement.
 * It is used to communicate with the client and update its stats.
 */
public interface ClientInterface extends Remote {
    /**
     * Set the id of the client
     * @param id
     */
    public void setId(int id) throws RemoteException;
    /**
     * Get the id of the client
     * @return the id of the client
     */
    public int getId() throws RemoteException;
    /**
     * Set the stats of the client
     * @param top10
     * @param averageTimes
     * @param activeBarrels
     */
    public void setStats(List<String> top10, List<Double> averageTimes, List<BarrelInterface> activeBarrels) throws RemoteException;
    
}
