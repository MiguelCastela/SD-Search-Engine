package search ;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import javax.annotation.PostConstruct;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * @authors
 * Miguel Castela 2022212972 👍
 * Miguel Martins 2022213951 👍
 */


/**
 * ClientService class that acts as a WebServer, connects to the Gateway and provides methods for searching and updating stats.
 * It also handles WebSocket communication for real-time updates.
 */


@Service
public class ClientService extends UnicastRemoteObject implements ClientServiceInterface {

    @Autowired
    private final SimpMessagingTemplate messagingTemplate;

    private GatewayInterface gateway;
    private static int id;
    private List<String> top10 = new ArrayList<>();
    private List<Double> averageTimes = new ArrayList<>();
    private List<Integer> activeBarrels = new ArrayList<>();
    private List<Integer> barrelSize = new ArrayList<>();
    private static int MAX_RETRIES;
    /**
     * Gateway IP address, directory, index file, URL file
     */
    private static String GATEWAY_IP_ADDRESS = "", DIR = "", INDEX_FILE = "", URL_FILE = "";
    /**
     * Maximum size of the barrel
     */
    private static long MAX_SIZE;


    public ClientService(SimpMessagingTemplate messagingTemplate) throws RemoteException {
        this.messagingTemplate = messagingTemplate;        
        
        if (!loadConfigFile()) {
            System.out.println("Error loading config file");
            return;
        }


        try {
            gateway = (GatewayInterface) Naming.lookup("rmi://" + GATEWAY_IP_ADDRESS + ":1099/Gateway");
            System.out.println("Connected to Gateway successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    /**
     * Registers the client with the gateway
     */
    public void registerClientOnce() {
        try {
            gateway.registerClientService(this);
            System.out.println("Web Client registered successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
    /**
     * Searches for a word in the index of a random barrel
     * @param words
     * @param page
     * @param pageSize
     * @param isCached
     * @return
     * @throws RemoteException
     */
    public List<UrlInfo> searchWord(List<String> words, int page, int pageSize, boolean isCached) throws RemoteException {
        if (page == 1 && !isCached) gateway.topSearches(String.join(" ", words));
        return gateway.searchWord(words, page, pageSize, isCached);
    }

    /**
     * Searches for citations in the index of a random barrel
     * @param words
     * @param page
     * @param pageSize
     * @param isCached
     * @return
     * @throws RemoteException
     */
    public List<String> searchCitations(List<String> words, int page, int pageSize, boolean isCached) throws RemoteException {
        return new ArrayList<>(gateway.searchCitations(words, page, pageSize, isCached));
    }

    /**
     * Registers the client with the gateway
     */
    public void setId(int id) {
        ClientService.id = id;
    }

    /**
     * Returns the id of the client
     * @return
     */
    public int getId() {
        return id;
    }


    /**
     * Sets the stats of the client for the WebSocket
     * @param top10
     * @param averageTimes
     * @param activeBarrels
     * @param barrelSize
     */
    public void setStats(List<String> top10, List<Double> averageTimes, List<BarrelInterface> activeBarrels, List<Integer> barrelSize) {
        this.top10 = top10;
        this.averageTimes = averageTimes;
        this.barrelSize = barrelSize;
        this.activeBarrels.clear();

        for (BarrelInterface barrel : activeBarrels) {
            try {
                this.activeBarrels.add(barrel.getId());
            } catch (RemoteException e) {
                this.activeBarrels.add(0);
                continue;
            }
        }


        
        // Create a stats payload
        Map<String, Object> stats = new HashMap<>();
        stats.put("top10", top10);
        stats.put("averageTimes", averageTimes);
        stats.put("activeBarrels", this.activeBarrels);
        stats.put("barrelSize", barrelSize);

        // Broadcast the stats to WebSocket clients
        messagingTemplate.convertAndSend("/topic/stats", stats);
    }


    /**
     * Returns the top10 
     * @return
     */
    public List<String> getTop10() {
        return new ArrayList<>(top10);
    }

    /**
     * Returns the average times of the barrels
     * @return
     */
    public List<Double> getAverageTimes() {
        return new ArrayList<>(averageTimes);
    }

    /**
     * Returns the active barrels
     * @return
     */
    public List<Integer> getActiveBarrels() {
        return new ArrayList<>(activeBarrels);
    }

    /**
     * Returns the barrel size
     * @return
     */
    public List<Integer> getBarrelSize() {
        return new ArrayList<>(barrelSize);
    }


    /**
     * Returns all the stats of the client
     * @return
     * @throws RemoteException
     */
    public Map<String, Object> getAllStats()  throws RemoteException {
        HashMap<String, Object> stats = new HashMap<>();
        stats.put("top10", top10);
        stats.put("averageTimes", averageTimes);
        stats.put("activeBarrels", activeBarrels);
        stats.put("barrelSize", barrelSize);
        return stats;
    }

    /**
     * puts the new URLs in the gateway
     * @param urls
     * @throws RemoteException
     */
    public void putNew(List<String> urls) throws RemoteException {
        System.out.println("Sending URLs to Gateway: " + urls);
        gateway.putNew(urls);
    }

    /**
     * Updates the stats of the client
     */
    public void updateStats() {
        try {
            gateway.sendStatsToWebClient();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads the config file
     * @return
     */
    public static boolean loadConfigFile() {
        Properties prop = new Properties();
        try (InputStream input = new FileInputStream("config.properties")) {
            prop.load(input);
            GATEWAY_IP_ADDRESS = prop.getProperty("GATEWAY_IP_ADDRESS");
            MAX_SIZE = Long.parseLong(prop.getProperty("MAX_SIZE"));
            DIR = prop.getProperty("FILE_DIR");
            INDEX_FILE = prop.getProperty("INDEX_FILE");
            URL_FILE = prop.getProperty("URL_FILE");
            MAX_RETRIES = Integer.parseInt(prop.getProperty("MAX_RETRIES"));
            return true;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }
}