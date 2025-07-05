package search;

import java.rmi.*;
import java.util.*;

/**
 * @authors
 * Miguel Castela 2022212972 👍
 * Miguel Martins 2022213951 👍
 */


/**
 * This interface defines the methods that the Gateway class must implement.
 */
public interface GatewayInterface extends Remote {
    /**
     * Takes next url from the queue
     * @return
     * @throws RemoteException
     */


    public List<Object> takeNext() throws RemoteException;
    /**
     * Puts new url in the queue
     * @param url
     * @throws RemoteException
     */
    public void putNew(List<String> url) throws RemoteException;
    /**
     * Searches for a word in the index of a random barrel
     * @param url
     * @return
     * @throws RemoteException
     */
    public List<UrlInfo> searchWord(List<String> url, int page, int pageSize, boolean isCached) throws RemoteException;
    /**
     * Searches for citations in the index of a random barrel
     * @param url
     * @return
     * @throws RemoteException
     */
    public Set<String> searchCitations(List<String> url, int page, int pageSize, boolean isCached) throws RemoteException;
    /**
     * Registers a new barrel into the gateway
     * @param barrel
     * @throws RemoteException
     */
    public void registerBarrel(BarrelInterface barrel) throws RemoteException;
    /**
     * Registers a new client into the gateway
     * @param client
     * @throws RemoteException
     */
    public void registerClient(ClientInterface client) throws RemoteException;
    /**
     * Adds the website info to the index of all barrels
     * @param words
     * @param url
     * @throws RemoteException
     */

    public void registerClientService(ClientServiceInterface client) throws RemoteException;
    /**
     * Adds the website info to the index of all barrels
     * @param words
     * @param url
     * @throws RemoteException
     */
    public void addToIndex(List<String> words, UrlInfo url) throws RemoteException;
    /**
     * Checks if the url is already in the index of any barrel
     * @param url
     * @return
     * @throws RemoteException
     */
    public int urlIsRepeated(String url) throws RemoteException;
    /**
     * Returns the barrels registered and active in the gateway
     * @return
     * @throws RemoteException
     */
    public List<BarrelInterface> getBarrels() throws RemoteException;
    /**
     * Updates the topSearches list
     * @param input
     * @throws RemoteException
     */
    public void topSearches(String input) throws RemoteException;

    
    /**
     * sends the stats to the client
     * @throws RemoteException
     */
    public void sendStatsToClient() throws RemoteException;

    /**
     * sends the stats to the web client
     * @throws RemoteException
     */
    public void sendStatsToWebClient() throws RemoteException;


    /**
     * Returns the stats of the gateway
     * @return
     * @throws RemoteException
     */
    public Map<String, Object> getCurrentStats() throws RemoteException;

    /**
     * Updates the barrel size
     * @throws RemoteException
     */
    public void updateBarrelSize() throws RemoteException;


    /**
     * get the top10 searches
     * @return
     * @throws RemoteException
     */
    public List<String> getTop10() throws RemoteException;

    /**
     * get the average times of the barrels
     * @return
     * @throws RemoteException
     */
    public List<Double> getAverageTimes() throws RemoteException;

    /**
     * get the active barrels
     * @return
     * @throws RemoteException
     */
    public List<Integer> getActiveBarrels() throws RemoteException;

    /**
     * get the barrel size
     * @return
     * @throws RemoteException
     */
    public List<Integer> getBarrelSize() throws RemoteException;
    
} 
    
