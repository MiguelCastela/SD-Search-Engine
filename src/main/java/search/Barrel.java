package search ;

import java.io.*;
import java.rmi.*;
import java.rmi.server.*;
import java.util.concurrent.*; 
import java.util.*;

/**
 * @authors
 * Miguel Castela 2022212972 👍
 * Miguel Martins 2022213951 👍
 */


/**
 * This class represents a barrel that stores indexed items and URLs.
 * It implements the BarrelInterface and extends UnicastRemoteObject.
 * It is used to communicate with the gateway and the client.
 */
public class Barrel extends UnicastRemoteObject implements BarrelInterface {
    /**
     * Indexed items
     */
    private ConcurrentHashMap<String, List<Integer>> indexedItems;
    /**
     * List of URLs
     */
    private List<UrlInfo> urlList; 
    /**
     * Gateway reference
     */ 
    private GatewayInterface gateway;
    /**
     * Barrel ID
     */
    private int id;
    /**
     * Maximum number of retries
     */
    private static int MAX_RETRIES;
    /**
     * Gateway IP address, directory, index file, URL file
     */
    private static String GATEWAY_IP_ADDRESS = "", DIR = "", INDEX_FILE = "", URL_FILE = "";
    /**
     * Maximum size of the barrel
     */
    private static long MAX_SIZE;
    /**
     * List of response times
     */
    private List<Double> responseTimes;

    public Barrel() throws RemoteException {
        super();
        this.indexedItems = new ConcurrentHashMap<>();
        this.urlList = new ArrayList<>();
        this.responseTimes = new ArrayList<>();
       

        if (!loadConfigFile()) {
            System.out.println("Error loading config file");
            return;
        }

        try {
            this.gateway = (GatewayInterface) Naming.lookup("rmi://" + GATEWAY_IP_ADDRESS + ":1099/Gateway");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Schedule the task to save data every 10 seconds
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::saveDataToFile, 10, 10, TimeUnit.SECONDS);

    }

    public static void main(String[] args) {
        try {
            Barrel barrel = new Barrel();
            System.out.println("Barrel is running...");
            System.out.println("max size: " + MAX_SIZE);    
            barrel.initializeBarrelInfo();
            try {
                barrel.gateway.registerBarrel(barrel);
            } catch (RemoteException e) {
                System.out.println("Failed to register barrel with gateway");
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down barrel...");
                try {
                    barrel.saveDataToFile();
                } catch (Exception e) {
                    System.out.println("Error saving data to file");
                    e.printStackTrace();
                }
            }));
            
        } catch (Exception e) {
            System.out.println("Barrel error");
            e.printStackTrace();
        }
    }

    @Override
    public List<UrlInfo> searchWords(List<String> words, int page, int pageSize) throws RemoteException { 
        List<UrlInfo> results = new ArrayList<>();
        if (words == null || words.isEmpty()) {
            return results;
        }

        // Get the intersection of all matching indexes
        Set<Integer> resultSet = new HashSet<>(indexedItems.getOrDefault(words.get(0), Collections.emptyList()));
        for (int i = 1; i < words.size(); i++) {
            resultSet.retainAll(indexedItems.getOrDefault(words.get(i), Collections.emptyList()));
        }

        // Convert the resultSet to a list of UrlInfo objects
        List<UrlInfo> allResults = new ArrayList<>();
        for (Integer index : resultSet) {
            if (index >= 0 && index < urlList.size()) { // Ensure index is within bounds
                allResults.add(urlList.get(index));
            }
        }

        // Sort the entire pool of results by citations
        allResults = sortResultsByCitations(allResults);

        // Apply pagination to the sorted results
        int start = (page - 1) * (pageSize-1);
        int end = Math.min(start + pageSize, allResults.size());
        if (start < allResults.size()) {
            results = new ArrayList<>(allResults.subList(start, end)); // Create a new ArrayList
        }

    return results;
}

    @Override
    public Set<String> searchCitations(List<String> words, int page, int pageSize) throws RemoteException {
        Set<String> results = new HashSet<>();
        if (words == null || words.isEmpty()) {
            return results;
        }
        String word = words.get(0);

        for (UrlInfo urlInfo : urlList) {
            if (urlInfo.getUrl().equals(word)) {
                List<String> citations = new ArrayList<>(urlInfo.getCitations());
                int start = (page - 1) * (pageSize-1);
                int end = Math.min(start + pageSize, citations.size());
                if (start < citations.size()) {
                    results.addAll(citations.subList(start, end));
                }
                break;
            }
        }
        return results;
    }  
    @Override
    public void addToIndex(List<String> words, UrlInfo urlInfo) throws RemoteException {
        int index;
        synchronized (urlList) {
            urlList.add(urlInfo);
            index = urlList.size() - 1;
        }
        for (String word : words) {
            indexedItems.computeIfAbsent(word, k -> new ArrayList<>()).add(index);
        }
    }
    /**
     * This method is used to save the data to the files
     */
    public void saveDataToFile() {
        try (ObjectOutputStream urlListOut = new ObjectOutputStream(new FileOutputStream(DIR + id + URL_FILE));
             ObjectOutputStream indexedItemsOut = new ObjectOutputStream(new FileOutputStream(DIR + id + INDEX_FILE))) {
            
            long max_size = MAX_SIZE;

            int current_url_list_size = urlList.size();
            int current_indexed_items_size = indexedItems.size();
            if (current_url_list_size > max_size || current_indexed_items_size > max_size) {
                System.out.println("File size exceeded");
                return;
            }

            synchronized (urlList) {
                urlListOut.writeObject(urlList);
            }

            synchronized (indexedItems) {
                indexedItemsOut.writeObject(indexedItems);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method is used to retrieve data from the file, if no file is found, it will start with empty data
     * @param barrel
     */

    public void retreiveDataFromFile() {
        File dir = new File(DIR);
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                System.out.println("Directory created: " + DIR);
            } else {
                System.out.println("Failed to create directory: " + DIR);
                return;
            }
        }
    
        // Carregar URL List
        File urlListFile = new File(DIR + id + URL_FILE);
        // Carregar IndexedItems
        File indexedItemsFile = new File(DIR + id + INDEX_FILE);
    
        boolean urlListFileExists = urlListFile.exists() && urlListFile.length() > 0;
        boolean indexedItemsFileExists = indexedItemsFile.exists() && indexedItemsFile.length() > 0;
    
        if (!urlListFileExists || !indexedItemsFileExists) {
            System.out.println("One or both data files are missing or empty. Creating new files with empty data.");
            saveDataToFile();
            return;
        }
    
        if (urlListFileExists) {
            try (ObjectInputStream urlListIn = new ObjectInputStream(
                    new BufferedInputStream(new FileInputStream(urlListFile), 8192))) { // 8KB Buffer
    
                synchronized (urlList) {
                    urlList = (List<UrlInfo>) urlListIn.readObject();
                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Error loading URL List");
                //
                
                
                e.printStackTrace();
            }
        } else {
            System.out.println("URL List file empty or missing.");
        }
    
        if (indexedItemsFileExists) {
            try (ObjectInputStream indexedItemsIn = new ObjectInputStream(
                    new BufferedInputStream(new FileInputStream(indexedItemsFile), 8192))) { // 8KB Buffer
    
                synchronized (indexedItems) {
                    indexedItems = (ConcurrentHashMap<String, List<Integer>>) indexedItemsIn.readObject();
                }
            } catch (EOFException e) {
                System.out.println("Error loading Indexed Items.");
            } catch (IOException | ClassNotFoundException e) {
                // System.out.println("Error loading Indexed Items.");
                //e.printStackTrace();
            }
        } else {
            System.out.println("Indexed Items file empty or missing.");
        }
    }
    
    @Override
    public void updateIndex(List<String> words, UrlInfo urlInfo) throws RemoteException {
        int index = -1;
        for (int i = 0; i < urlList.size(); i++) {
            if (urlList.get(i).getUrl().equals(urlInfo.getUrl())) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            System.out.println("URL not found in index");
            return;
        }
        synchronized (urlList) {
            UrlInfo existingUrlInfo = urlList.get(index);
            existingUrlInfo.setTitle(urlInfo.getTitle());
            existingUrlInfo.setDescription(urlInfo.getDescription());
        }
        synchronized (indexedItems) {
            for (String word : words) {
                indexedItems.computeIfAbsent(word, k -> new ArrayList<>()).add(index);
            }
        }
    }

    @Override
    public void setId(int id) throws RemoteException {
        this.id = id;
    }

    @Override
    public int getId() throws RemoteException {
        return id;
    }

    @Override
    public ConcurrentHashMap<String, List<Integer>> getIndexedItems() throws RemoteException {
        return indexedItems;
    }

    @Override
    public void setIndexedItems(ConcurrentHashMap<String, List<Integer>> indexedItems) throws RemoteException {
        this.indexedItems = indexedItems;
    }

    @Override
    public List<UrlInfo> getUrlList() throws RemoteException {
        return urlList;
    }

    @Override
    public void setUrlList(List<UrlInfo> urlList) throws RemoteException {
        this.urlList = urlList;
    }

    public List<UrlInfo> sortResultsByCitations(List<UrlInfo> results) {
        results.sort(Comparator.comparingInt(UrlInfo::getCountOfCitations).reversed());
        return results;
    }

    /**
     * Load the configuration file
     * @return true if the config file was loaded successfully, false otherwise
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

    @Override
    public int urlIsRepeated(String url) throws RemoteException {
        for (UrlInfo urlInfo : urlList) {
            if (urlInfo.getUrl().equals(url)) {
                if (urlInfo.getTitle() != null) {
                    return 1; // Already fully indexed
                }
                return 0; // Not fully indexed
            }
        }
        return -1; // Not indexed
    }

    @Override
    public void updateCitation(String url, String citation) throws RemoteException {
        for (UrlInfo urlInfo : urlList) {
            if (urlInfo.getUrl().equals(url)) {
                urlInfo.addCitation(citation);
                return;
            }
        }
    }

    @Override
    public int getUrlListSize() throws RemoteException {
        return urlList.size();
    }

    /**
     * Initialize the barrel with data from another barrel, or start anew
     */
    public void initializeBarrelInfo(){
        try{
            List<BarrelInterface>barrelList = gateway.getBarrels();
            if (barrelList.size() > 0){
                boolean done = false;
                int retries = 0;
                while (!done){
                    for (BarrelInterface barrel : barrelList){
                        try {
                            ConcurrentHashMap<String, List<Integer>> remoteIndexedItems = barrel.getIndexedItems();
                            if (remoteIndexedItems != null) {
                                setIndexedItems(remoteIndexedItems);
                            }
                            
                            List<UrlInfo> remoteUrlList = barrel.getUrlList();
                            if (remoteUrlList != null) {
                                setUrlList(remoteUrlList);
                            }
                            
                            done = true;
                            System.out.println("Successfully initialized barrel with data from Barrel: " + barrel.getId());
                            break;
                        } catch (RemoteException e) {
                        }                        
                    }
                    retries++;
                    if (retries > MAX_RETRIES){
                        done = true;
                        System.out.println("Failed to initialize barrel with data from other barrels. Starting with empty data.");
                    }
                }
            }
            else {
                retreiveDataFromFile();
            }
            
        }catch (RemoteException e){
            e.printStackTrace();
        }
    }

    @Override
    public double trackResponseTime(double responseTime) {
        synchronized (responseTimes) {
            responseTimes.add(responseTime);
            return calculateAverageResponseTime();
        }
    }

    /**
     * Calculate the average response time
     * @return the average response time
     */
    public double calculateAverageResponseTime() {
        synchronized (responseTimes) { 
            if (responseTimes.isEmpty()) {
                return 0;
            }
            long total = 0;
            for (double time : responseTimes) {
                total += time;
            }
            //print the current response times 
            System.out.println("Current response times: " + responseTimes);
            System.out.println("Average response time: " + (total / (double) responseTimes.size()));
            return total / (double) responseTimes.size();
        }
    }

    @Override
    public int getSize() throws RemoteException {
        int count = 0;
        for (UrlInfo urlInfo : urlList) {
            if (urlInfo.getDescription() != null) {
            count++;
            }
        }
        return count;

    }
}


