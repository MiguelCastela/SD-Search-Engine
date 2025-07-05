package search ;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import java.io.*;
import java.rmi.*;
import java.rmi.server.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @authors
 * Miguel Castela 2022212972 👍
 * Miguel Martins 2022213951 👍
 */


/**
 * This class represents the gateway of the system.
 * It is responsible for managing the barrels and clients,
 * as well as the queue of URLs to be indexed.
 * It also provides methods for searching and adding URLs to the index.
 */

@Service
public class Gateway extends UnicastRemoteObject implements GatewayInterface {

    /**
     * List of active barrels
     */
    private List<BarrelInterface> barrels;
    /**
     * List of active clients
     */
    private List<ClientInterface> clients;
    
    
    /**
     * List of active web clients
     */
    private List<ClientServiceInterface> webClients;    
    
    /**
     * Queue of URLs to be indexed
     */

    private LinkedBlockingQueue<String> urlsToIndex;
    /**
     * Map of top searches
     */
    private ConcurrentHashMap<String, Integer> topSearches;
    /**
     * List of average response times for each barrel
     */
    private List<Double>  averageResponseTimes;
    /**
     * List of index size for each barrel
     */
    private List<Integer> barrelSizes;
    /**
     * RMI registry
     */
    private Registry registry;
    /**
     * Next barrel ID
     */
    private int nextBarrelId = 0;
    /**
     * Next client ID
     */
    private int nextClientId = 0;
    /**
     * Maximum number of retries
     */
    private static int MAX_RETRIES;
    /**
     * Maximum size of the data files
     */
    private static long MAX_SIZE;
    /**
     * Directory to save the data files, queue file, searches file, gateway ip address
     */
    private static String DIR = "", QUEUE_FILE = "", SEARCHES_FILE = "", GATEWAY_IP_ADDRESS = "";



    public Gateway() throws RemoteException {
        super();

        urlsToIndex = new LinkedBlockingQueue<>();
        barrels = new ArrayList<>();
        clients = new ArrayList<>();
        webClients = new ArrayList<>();

        averageResponseTimes = new ArrayList<>();
        topSearches = new ConcurrentHashMap<>();
        barrelSizes = new ArrayList<>();
    }

    public static void main(String[] args) {
        Gateway gateway;
            try {
                gateway = new Gateway();

                        try {
                LocateRegistry.getRegistry(1099).list();
            } catch (RemoteException e) {
                    System.out.println("RMI registry not found, creating new one...");
                try {
                    LocateRegistry.createRegistry(1099);
                } catch (RemoteException ex) {
                    System.err.println("Failed to create RMI registry: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }


            try {
                Naming.rebind("Gateway", gateway);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RemoteException("Error binding Gateway");
            }
            gateway.topSearches = new ConcurrentHashMap<>();

            if (!loadConfigFile()) {
                System.out.println("Error loading config file");
                return;
            }

            System.out.println("Created new gateway");

            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
            scheduler.scheduleAtFixedRate(gateway::saveDataToFile, 10, 10, TimeUnit.SECONDS);
            scheduler.scheduleAtFixedRate(gateway::saveTopSearchesToFile, 10, 10, TimeUnit.SECONDS);
            scheduler.scheduleAtFixedRate(gateway::heartbeatCheck, 1, 1, TimeUnit.SECONDS);
            System.out.println("Gateway is running...");
            System.out.println("Gateway max size: " + MAX_SIZE);
            gateway.retrieveDataFromFile();
            gateway.retrieveTopSearches();

        } catch (Exception e) {
            System.out.println("Gateway error");
            e.printStackTrace();
        }
    }

    @Override
    public List<Object> takeNext() throws RemoteException {
        try{
            List<Object> result = new ArrayList<>();
            result.add(urlsToIndex.take());
            result.add(new ArrayList<>(barrels));
            return result;
        }
        catch (InterruptedException e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void putNew(List<String> url) throws RemoteException {
        for(int i = 0; i < url.size(); i++){
            urlsToIndex.add(url.get(i));
        }
    }

    @Override
    public void addToIndex(List<String> words, UrlInfo url) throws RemoteException {
        for (BarrelInterface barrel : barrels) {
            try {
                barrel.addToIndex(words, url);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public List<UrlInfo> searchWord(List<String> url, int page, int pageSize, boolean isCached) throws RemoteException {
        List<UrlInfo> results = new ArrayList<>();
        boolean done = false;
        while (!done){
            if (barrels.isEmpty()) {
                System.out.println("No barrels available");
                return null;
            }
            int index = (int) (Math.random() * barrels.size());
            BarrelInterface barrel = barrels.get(index);
            try {
                System.out.println("Searching on barrel: " + barrel.getId());  
                long startTime = System.currentTimeMillis();
                results = barrel.searchWords(url, page, pageSize);
                long endTime = System.currentTimeMillis();
                
                if (!isCached){
                    double responseTime = Math.round(barrel.trackResponseTime(endTime - startTime) * 10.0) / 10.0;
                    updateResponseTime(responseTime, index);
                }

                done = true;
            } catch (RemoteException e) {
                System.err.println("Failed to search on barrel: " + barrel.getId());
                e.printStackTrace();
            }
        }

        return results;
    }

    @Override
    public Set<String> searchCitations(List<String> url, int page, int pageSize, boolean isCached) throws RemoteException {
        Set<String> results = new HashSet<>();
        if (barrels.isEmpty()) {
            return null;
        }
        boolean done = false;
        while (!done){
            int index = (int) (Math.random() * barrels.size());
            BarrelInterface barrel = barrels.get(index);
            try {
                long startTime = System.currentTimeMillis();
                results = barrel.searchCitations(url, page, pageSize);
                long endTime = System.currentTimeMillis();
                
                if (!isCached){
                    double responseTime = Math.round(barrel.trackResponseTime(endTime - startTime) * 10.0) / 10.0;
                    updateResponseTime(responseTime, index);
                }

                done = true;
                System.out.println("Search done on barrel: " + barrel.getId());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return results;
    }

    @Override
    public void registerBarrel(BarrelInterface barrel) throws RemoteException {
        int id = nextBarrelId++;
        barrel.setId(id);
        barrels.add(barrel);
        averageResponseTimes.add(null);
        barrelSizes.add(barrel.getSize());
        System.out.println("Barrel registered successfully with ID: " + id);
        sendStatsToClient();
        sendStatsToWebClient();
    }

    

    @Override
    public void registerClient(ClientInterface client) throws RemoteException {
        int id = nextClientId++;
        client.setId(id);
        clients.add(client);
        System.out.println("CLIClient registered successfully with ID: " + id);
        singleClientStats(client);
    }

    public void registerClientService(ClientServiceInterface client) throws RemoteException {
        int id = nextClientId++;
        client.setId(id);
        webClients.add(client);
        System.out.println("Web Client registered successfully with ID: " + id);
        singleWebClientStats(client);
    }


    

    @Override
    public int urlIsRepeated(String url) throws RemoteException {
        if (barrels.isEmpty()) {
            return -2;
        }
        BarrelInterface barrel = barrels.get((int) (Math.random() * barrels.size()));
        return barrel.urlIsRepeated(url);
    }

    
    /**
     * Save the data to a file
     */
    private void saveDataToFile() {
        try (ObjectOutputStream queueOut = new ObjectOutputStream(new FileOutputStream(DIR + QUEUE_FILE))) {
            long max_size = MAX_SIZE;

            long current_queue_size = urlsToIndex.size();
            if(current_queue_size > max_size){
                System.out.println("File size exceeded");
                return;
            }


            synchronized (urlsToIndex){
                queueOut.writeObject(urlsToIndex);
                
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Save the top searches to a file
     */
    private void saveTopSearchesToFile() {
        try (ObjectOutputStream topSearchesOut = new ObjectOutputStream(new FileOutputStream(DIR + SEARCHES_FILE))) {
            long max_size = MAX_SIZE;
            long current_size = topSearches.size();
            if (current_size > max_size) {
                System.out.println("File size exceeded");
                return;
            }
            synchronized (topSearches) {
                topSearchesOut.writeObject(topSearches);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Retrieve the topSearches from the file
     */
    private void retrieveTopSearches() {
        File dir = new File(DIR);
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                System.out.println("Directory created: " + DIR);
            } else {
                System.out.println("Failed to create directory: " + DIR);
                return;
            }
        }

        File topSearchesFile = new File(DIR + SEARCHES_FILE);

        if (!topSearchesFile.exists()) {
            System.out.println("Top searches data file not found, starting with empty data.");
            return;
        } else {
            System.out.println("Top searches data file found, loading data.");
            System.out.println("size of file: " + topSearchesFile.length());
        }

        try (ObjectInputStream topSearchesIn = new ObjectInputStream(new FileInputStream(topSearchesFile))) {
            synchronized (topSearches) {
                topSearches = (ConcurrentHashMap<String, Integer>) topSearchesIn.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }


    }
    /**
     * Retrieve the queue data from the file
     */
    private void retrieveDataFromFile() {
        
        File dir = new File(DIR);
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                System.out.println("Directory created: " + DIR);
            } else {
                System.out.println("Failed to create directory: " + DIR);
                return;
            }
        }

        
        File urlsToIndexFile = new File(DIR + QUEUE_FILE);


        if (!urlsToIndexFile.exists())  {
            System.out.println("Data file not found, starting with empty data.");
            return;
        } else {
            System.out.println("Data file found, loading data.");
            System.out.println("size of file: " + urlsToIndexFile.length());
        }


        
        try (ObjectInputStream queueIn = new ObjectInputStream(new FileInputStream(urlsToIndexFile))){

            synchronized (urlsToIndex){
                urlsToIndex = (LinkedBlockingQueue<String>) queueIn.readObject();
                
            }

        }catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    /**
     * Load the configuration file
     */
    public static boolean loadConfigFile() {
        Properties prop = new Properties();
        try (InputStream input = new FileInputStream("config.properties")) {
            prop.load(input);
            GATEWAY_IP_ADDRESS = prop.getProperty("GATEWAY_IP_ADDRESS"); 
            MAX_SIZE = Long.parseLong(prop.getProperty("MAX_SIZE"));
            DIR = prop.getProperty("FILE_DIR");
            QUEUE_FILE = prop.getProperty("QUEUE_FILE");
            SEARCHES_FILE = prop.getProperty("SEARCHES_FILE");
            MAX_RETRIES = Integer.parseInt(prop.getProperty("MAX_RETRIES"));
        } catch ( IOException ex) {
            ex.printStackTrace();

            return false;
        }
    
        return true;
    }

    @Override
    public List<BarrelInterface> getBarrels() {
        return barrels;
    }
    
    @Override
    public void topSearches(String input) {
        List<String> topTen_first = getTopTen();
        topSearches.put(input, topSearches.getOrDefault(input, 0) + 1);
        List<String> topTen_second = getTopTen();
        if (!topTen_first.equals(topTen_second)) {
            sendStatsToClient();
            try {
                sendStatsToWebClient();
            } catch (RemoteException e) {
                System.err.println("Failed to send stats to web clients: " + e.getMessage());
            }
        }
    }

    

    /**
     * Get the top ten searches
     * @return
     */
    public List<String> getTopTen() {
        List<String> topTen = new ArrayList<>();
        topSearches.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(10)
                .forEach(e -> topTen.add(e.getKey() + " : " + e.getValue()));
        return topTen;
    }

    /**
     * Check if new response time is different from the previous one, if so update client stats
     * @param responseTime
     * @param idx
     */
    public void updateResponseTime(double responseTime, int idx) {
        if (averageResponseTimes.get(idx) == null) {
            averageResponseTimes.set(idx, responseTime);
        } else {
            if (responseTime != averageResponseTimes.get(idx)) {
                averageResponseTimes.set(idx, responseTime);
            }
        }
    
        try {
            sendStatsToWebClient();
        } catch (RemoteException e) {
            System.err.println("Failed to send stats to web clients: " + e.getMessage());
        }
        sendStatsToClient();
    }

    /**
     * Check if barrels and clients are still alive
     */
    private void heartbeatCheck() {
        Iterator<BarrelInterface> barrelIterator = barrels.iterator();
        while (barrelIterator.hasNext()) {
            BarrelInterface barrel = barrelIterator.next();
            boolean alive = false;
    
            for (int i = 0; i < MAX_RETRIES; i++) {
                try {
                    barrel.getId(); // Simple call to check if barrel responds
                    alive = true;
                    break;
                } catch (RemoteException e) {
                    System.err.println("Heartbeat failed for barrel (retry " + (i + 1) + ")");
                }
            }
    
            if (!alive) {
                System.out.println("Heartbeat failed. Removing dead barrel.");
                barrelIterator.remove();
                sendStatsToClient();
                try {
                    sendStatsToWebClient();
                } catch (RemoteException e) {
                    System.err.println("Failed to send stats to web clients: " + e.getMessage());
                }
            }
        }



        Iterator<ClientInterface> clientIterator = clients.iterator();
        while (clientIterator.hasNext()) {
            ClientInterface client = clientIterator.next();
            boolean alive = false;
        
            for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                client.getId(); // Simple call to check if client responds
                alive = true;
                break;
            } catch (RemoteException e) {
                System.err.println("Heartbeat failed for client (retry " + (i + 1) + ")");
            }
            }
        
            if (!alive) {
            System.out.println("Heartbeat failed. Removing dead client.");
            clientIterator.remove();
            }
        }

        Iterator<ClientServiceInterface> webClientIterator = webClients.iterator();
        while (webClientIterator.hasNext()) {
            ClientServiceInterface webClient = webClientIterator.next();
            boolean alive = false;
        
            for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                webClient.getId(); // Simple call to check if client responds
                alive = true;
                break;
            } catch (RemoteException e) {
                System.err.println("Heartbeat failed for web client (retry " + (i + 1) + ")");
            }
            }
        
            if (!alive) {
            System.out.println("Heartbeat failed. Removing dead web client.");
            webClientIterator.remove();
            }
        }
    }
  
    /**
     * Update stats to all clients
     */
    @Override
    public void sendStatsToClient() {
        List<ClientInterface> done = new ArrayList<>();
        int retries = 0;
        while(true){
            for(ClientInterface client : clients){
                try {
                    client.setStats(getTopTen(), averageResponseTimes, barrels);
                    done.add(client);
                } catch (RemoteException e) {
                    System.err.println("Failed to send stats to client: " + e.getMessage());
                }
            }
            retries++;
            if (done.size() == clients.size() || retries == MAX_RETRIES ){
                break;
            }
        }
    }

    /**
     * Update stats to all web clients
     */
    @Override
    public void updateBarrelSize() throws RemoteException {
        for (int i = 0; i < barrels.size(); i++) {
            try {
                barrelSizes.set(i, barrels.get(i).getSize());
            } catch (RemoteException e) {
                System.err.println("Failed to get size for barrel: " + barrels.get(i).getId());
                barrelSizes.set(i, -1); 
            }
        }
        sendStatsToWebClient();
    }

    /**
     * Update stats to all web clients
     */
    @Override
    public void sendStatsToWebClient() throws RemoteException {

        List<ClientServiceInterface> done = new ArrayList<>();
        int retries = 0;


        while(true){
            for(ClientServiceInterface client : webClients){
                try {
                    client.setStats(getTopTen(), averageResponseTimes, barrels, barrelSizes);
                    done.add(client);
                } catch (RemoteException e) {
                    System.err.println("Failed to send stats to web client: " + e.getMessage());
                }
            }
            retries++;
            if (done.size() == webClients.size() || retries == MAX_RETRIES ){
                break;
            }
        }

    }
    /**
     * Send current stats to new single client
     */
    private void singleClientStats(ClientInterface client) {
        try {
            client.setStats(getTopTen(), averageResponseTimes, barrels);
        } catch (RemoteException e) {
            System.err.println("Failed to send stats to client: " + e.getMessage());
        }
    }

    /**
     * Send current stats to new single web client
     */
    private void singleWebClientStats(ClientServiceInterface client) {
        try {
            client.setStats(getTopTen(), averageResponseTimes, barrels, barrelSizes);
        } catch (RemoteException e) {
            System.err.println("Failed to send stats to web client: " + e.getMessage());
        }
    }

    /**
     * Get the current stats of the gateway
     * @return
     */
    @Override
    public Map<String, Object> getCurrentStats() throws RemoteException {
        Map<String, Object> stats = new HashMap<>();
        stats.put("top10", getTopTen());
        stats.put("averageTimes", averageResponseTimes);
        stats.put("activeBarrels", barrels.stream().map(barrel -> {
            try {
                return barrel.getId();
            } catch (RemoteException e) {
                return 0; // Default value if barrel ID cannot be fetched
            }
        }).collect(Collectors.toList()));
        return stats;
    }

    /**
     * Get the top 10 searches
     * @return
     */
    @Override
    public List<String> getTop10() throws RemoteException {
        return getTopTen();
    }

    /**
     * Get the average times of the barrels
     * @return
     */
    @Override
    public List<Double> getAverageTimes() throws RemoteException {
        return averageResponseTimes;
    }

    /**
     * Get the active barrels
     * @return
     */
    @Override
    public List<Integer> getActiveBarrels() throws RemoteException {
        return barrels.stream().map(barrel -> {
            try {
                return barrel.getId();
            } catch (RemoteException e) {
                return -1; // Default value if barrel ID cannot be fetched
            }
        }).collect(Collectors.toList());
    }

    /**
     * Get the barrel size
     * @return
     */
    @Override
    public List<Integer> getBarrelSize() throws RemoteException {
        return barrels.stream().map(barrel -> {
            try {
                return barrel.getSize();
            } catch (RemoteException e) {
                return -1; // Default value if barrel size cannot be fetched
            }
        }).collect(Collectors.toList());
    }
}