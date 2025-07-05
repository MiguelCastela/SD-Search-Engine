package search ;

import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.io.*;
import java.util.*;

import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

/**
 * @authors
 * Miguel Castela 2022212972 👍
 * Miguel Martins 2022213951 👍
 */


/**
 * Downloader class that connects to the Gateway and downloads URLs.
 * It filters out dead barrels and retries indexing URLs if necessary.
 * It also extracts links from the downloaded pages and adds them to the queue for later indexing.
 */
public class Downloader extends UnicastRemoteObject implements DownloaderInterface {
    
    /**
     * Gateway reference
     */
    private GatewayInterface gateway;
    /**
     * Gateway IP address
     */
    private static String GATEWAY_IP_ADDRESS = "";
    /**
     * Maximum number of retries
     */
    private static int MAX_RETRIES;

    public Downloader() throws RemoteException {
        super();

        if (!loadConfigFile()) {
            System.out.println("Error loading config file");
            return;
        }

        try {
            this.gateway = (GatewayInterface) Naming.lookup("rmi://" + GATEWAY_IP_ADDRESS + ":1099/Gateway");
            System.out.println("Connected Gateway class: " + gateway.getClass().getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            Downloader downloader = new Downloader();
            System.out.println("Downloader is running...");

            while (true) {
                try {

                    // Get next URL and alive barrels to download
                    List<Object> urlAndBarrels = downloader.gateway.takeNext();
                    String url = (String) urlAndBarrels.get(0);
                    
                    // Queue is empty
                    if (url == null) {
                        continue;
                    }

                    // Already Indexed URL will be skipped
                    int i = downloader.gateway.urlIsRepeated(url);


                    // No active barrels, skip???
                    if (i == -2){
                        System.out.println("No active barrels found. Skipping this URL.");
                        continue;
                    }

                    if (i == 1) {
                        continue;
                    }

                    System.out.println("Downloading: " + url);

                    // Filter out dead barrels
                    List<?> rawBarrels = (List<?>) urlAndBarrels.get(1);
                    List<BarrelInterface> barrels = filterAliveBarrels(rawBarrels);

                    // No alive barrels, skip the url
                    if (barrels.isEmpty()) {
                        System.out.println("No alive barrels found. Skipping this URL.");
                        continue;
                    }

                    // Download the URL
                    Document doc = Jsoup.connect(url).get();
                    String title = doc.title();
                    String description = doc.body().text().length() > 100
                            ? doc.body().text().substring(0, 200) + "..."
                            : doc.body().text();

                    UrlInfo urlInfo = new UrlInfo(url, title, description);

                    StringTokenizer st = new StringTokenizer(doc.text(), " ,.!?:;");
                    List<String> words = new ArrayList<>();
                    while (st.hasMoreElements()) {
                        words.add(st.nextToken().toLowerCase());
                    }

                    List<BarrelInterface> done = new ArrayList<>();
                    int retries = 0;

                    // Index or update(add title and body info) the URL
                    // Break after MAX_RETRIES or all barrels are done                   
                    while (true) {
                        for (BarrelInterface barrel : barrels) {
                            if (done.contains(barrel)) continue;
                            try {
                                if (i == -1) {
                                    barrel.addToIndex(words, urlInfo);
                                    downloader.gateway.updateBarrelSize();
                                } else {
                                    barrel.updateIndex(words, urlInfo);
                                    downloader.gateway.updateBarrelSize();
                                }
                                done.add(barrel);
                            } catch (RemoteException e) {
                                System.err.println("Failed to index: " + e.getMessage());
                            }
                        }
                        retries++;
                        if (done.size() == barrels.size() || retries == MAX_RETRIES ){
                            barrels = removeBarrelsNotInList(barrels, done); 
                            break;
                        }
                    }

                    // Extract links from the URL
                    Elements links = doc.select("a[href]");
                    for (Element link : links) {
                        done = new ArrayList<>();
                        retries = 0;

                        List<String> foundUrl = List.of(link.attr("abs:href"));

                        // URL is not yed indexed, add incomplete indexation
                        if (downloader.gateway.urlIsRepeated(foundUrl.get(0)) == -1) {
                            UrlInfo incompleteUrl = new UrlInfo(foundUrl.get(0), null, null);
                            incompleteUrl.addCitation(url);
                            while (true) {
                                for (BarrelInterface barrel : barrels) {
                                    try {
                                        barrel.addToIndex(new ArrayList<>(), incompleteUrl);
                                        done.add(barrel);
                                    } catch (RemoteException e) {
                                        System.err.println("Failed to add to index: " + e.getMessage());
                                    }
                                }
                                retries++;
                                if (done.size() == barrels.size() || retries == MAX_RETRIES ){
                                    barrels = removeBarrelsNotInList(barrels, done); 
                                    break;
                                }
                            }

                        // URL is already indexed, update citations    
                        } else {
                            while (true) {
                                for (BarrelInterface barrel : barrels) {
                                    try {
                                        barrel.updateCitation(foundUrl.get(0), url);
                                        done.add(barrel);
                                    } catch (RemoteException e) {
                                        System.err.println("Failed to update citation: " + e.getMessage());
                                    }
                                }
                                retries++;
                                if (done.size() == barrels.size() || retries == MAX_RETRIES ){
                                    barrels = removeBarrelsNotInList(barrels, done); 
                                    break;
                                }
                            }
                        }
                        // Add new URL to the queue for later indexation
                        downloader.gateway.putNew(foundUrl);
                    }

                } catch (Exception e) {
                    System.out.println("Downloader encountered an error");
                    //e.printStackTrace();
                    
                }
            }

        } catch (Exception e) {
            System.out.println("Downloader error");
            //e.printStackTrace();
        }
    }

    /**
     * Filter out dead barrels by pinging them
     * @param rawBarrels
     * @return
     */
    private static List<BarrelInterface> filterAliveBarrels(List<?> rawBarrels) {
        List<BarrelInterface> alive = new ArrayList<>();
        for (Object obj : rawBarrels) {
            if (obj instanceof BarrelInterface barrel) {
                try {
                    barrel.getId(); // Simple ping
                    alive.add(barrel);
                } catch (RemoteException e) {
                    System.err.println("Dead barrel detected. Skipping.");
                }
            }
        }
        return alive;
    }

    /**
     * Load configuration file
     * @return
     */
    public static boolean loadConfigFile() {
        Properties prop = new Properties();
        try (InputStream input = new FileInputStream("config.properties")) {
            prop.load(input);
            GATEWAY_IP_ADDRESS = prop.getProperty("GATEWAY_IP_ADDRESS");
            MAX_RETRIES = Integer.parseInt(prop.getProperty("MAX_RETRIES"));
            return true;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Remove barrels from list A that are not in list B
     * @param listA
     * @param listB
     * @return
     */
    public static List<BarrelInterface> removeBarrelsNotInList(List<BarrelInterface> listA, List<BarrelInterface> listB) {
        List<BarrelInterface> result = new ArrayList<>();
        for (BarrelInterface barrelA : listA) {
            if (listB.contains(barrelA)) {
                result.add(barrelA);
            }
        }
        return result;
    }
}
