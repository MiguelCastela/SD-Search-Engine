package search ;

import java.rmi.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @authors
 * Miguel Castela 2022212972 👍
 * Miguel Martins 2022213951 👍
 */


/**
 * This interface defines the methods that the Barrel class must implement.
 * It is used to communicate with the gateway and the client.
 */

public interface BarrelInterface extends Remote {
    /**
     * This method is used to search for a list of words in the indexed items.
     * @param words the list of words to search for
     * @return the list of URLs that contain all the words
     */
    public List<UrlInfo> searchWords(List<String> words, int page, int pageSize) throws RemoteException;
    /**
     * This method is used to search for a list of citations in the indexed items.
     * @param words the list of citations to search for
     * @return the list of URLs that contain all the citations
     */
    public Set<String> searchCitations(List<String> words, int page, int pageSize) throws RemoteException;
    /**
     * This method is used to add a URL to the indexed items.
     * @param word
     * @param url
     * @throws RemoteException
     */
    public void addToIndex(List<String> words, UrlInfo url) throws RemoteException;
    /**
     * If the url was already created but not fully indexed, update it
     * @param words
     * @param urlInfo
     */
    public void updateIndex(List<String> words, UrlInfo urlInfo) throws RemoteException;
    /**
     * Set the id of the barrel
     * @param id
     * @throws RemoteException
     */
    public void setId(int id) throws RemoteException; 
    /**
     * Get the id of the barrel
     * @return the id of the barrel
     * @throws RemoteException
     */
    public int getId() throws RemoteException;
    /**
     * Check if the URL is already indexed, and if so what info do we have about it
     * @param url
     * @return
     */
    public int urlIsRepeated(String url) throws RemoteException;
    /**
     * Update the citation of the existing URL
     * @param url
     * @param citation
     */
    public void updateCitation(String url, String citation) throws RemoteException;
    /**
     * Get the indexed items
     * @return the indexed items
     * @throws RemoteException
     */
    public ConcurrentHashMap<String, List<Integer>> getIndexedItems() throws RemoteException;
    /**
     * Set the indexed items
     * @param indexedItems
     * @throws RemoteException
     */
    public void setIndexedItems(ConcurrentHashMap<String, List<Integer>> indexedItems) throws RemoteException;
    /**
     * Get the list of URLs
     * @return the list of URLs
     * @throws RemoteException
     */
    public List<UrlInfo> getUrlList() throws RemoteException;
    /**
     * Set the list of URLs
     * @param urlList
     * @throws RemoteException
     */
    public void setUrlList(List<UrlInfo> urlList) throws RemoteException;
    /**
     * Add response time to the list, return new average time
     * @param responseTime
     * @return
     * @throws RemoteException
     */
    public double trackResponseTime(double responseTime) throws RemoteException;

    public int getSize() throws RemoteException;

    //DEBUGGING
    public int getUrlListSize() throws RemoteException;

}
