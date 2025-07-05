package search ;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;


/**
 * @authors
 * Miguel Castela 2022212972 👍
 * Miguel Martins 2022213951 👍
 */


/**
 * This class represents the client of the system.
 * It is responsible for interacting with the user and sending requests to the Gateway.
 * It implements the ClientInterface and extends UnicastRemoteObject to allow remote method invocation.
 */
public class Client extends UnicastRemoteObject implements ClientInterface {
    /**
     * The IP address of the Gateway
     */
    private static String GATEWAY_IP_ADDRESS = "";
    /**
     * The Gateway interface
     */
    private GatewayInterface gateway;
    /**
     * The id of the client
     */
    private static int id;
    /**
     * The flag to check if the client is in admin mode
     */
    private volatile boolean inAdminMode = false;
    /**
     * The top 10 most searched words
     */
    private List<String> top10 = new ArrayList<>();
    /**
     * The average search times
     */
    private List<Double> averageTimes = new ArrayList<>();
    /**
     * The active barrels
     */
    private List<Integer> activeBarrels = new ArrayList<>();

    public Client() throws RemoteException {
        super();

        if (!loadConfigFile()) {
            System.out.println("Error loading config file");
            return;
        }

        try {
            this.gateway = (GatewayInterface) Naming.lookup("rmi://" + GATEWAY_IP_ADDRESS + ":1099/Gateway");
            gateway.registerClient(this);
        } catch (Exception e) {
            System.out.println("Error connecting to Gateway");
            e.printStackTrace();
            return;
        }
        
    }

    /**
     * Load the configuration file
     * @return true if the file was loaded successfully, false otherwise
     */
    public static boolean loadConfigFile() {
        Properties prop = new Properties();
        try (InputStream input = new FileInputStream("config.properties")) {
            prop.load(input);
            GATEWAY_IP_ADDRESS = prop.getProperty("GATEWAY_IP_ADDRESS"); 
            return true;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static void main(String[] args) {

        try {
            Client client = new Client();
            Scanner scanner = new Scanner(System.in);
            
            while (true) {
                client.printMainMenu();
                String mode = scanner.nextLine();
                String input;
                int result = 0;
                switch (mode) {
                    case "1":
                        System.out.println("Enter URL to insert:");
                        input = scanner.nextLine();
                        result = client.parser(input, client.gateway, 1);
                        break;
                    case "2":
                        System.out.println("Enter word to search or URL to see citations:");
                        input = scanner.nextLine();
                        result = client.parser(input, client.gateway, 2);
                        break;
                    case "3":
                        client.adminMode(scanner);
                        break;
                    case "4":
                        result = client.exit();
                        scanner.close();
                        return;
                    default:
                    System.out.println("Invalid input, try again...");
                        break;
                }
                if (result == 1) {
                    break;
                } else if (result == -1) {
                    System.out.println("Error");
                }
            }
            scanner.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * Exit the client
     * @return
     */
    public int exit() {
        System.out.println("Goodbye!");
        System.exit(0);
        return 1;
    }
    /**
     * Insert a URL into the Gateway
     * @param input
     * @param gate
     * @return
     */
    public int insertURL(List<String> input, GatewayInterface gate) {
        try {
            gate.putNew(input);
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }


    /**
     * Parse client input and call the corresponding method
     * @param input
     * @param gate
     * @param mode
     * @return
     */
    public int parser(String input, GatewayInterface gate, int mode) {

        List<String> inputList = new ArrayList<>(Arrays.asList(input.split(" ")));
        
        if (input.isEmpty()) {
            return 0;
        }

        switch (mode){
            case 1:
                inputList.removeIf(item -> !item.startsWith("http://") && !item.startsWith("https://"));
                if (inputList.isEmpty()) {
                    System.out.println("Invalid URLs");
                    return 0;
                }
                else {
                    return insertURL(inputList, gate);
                }
            case 2:
                if (inputList.get(0).startsWith("http://") || inputList.get(0).startsWith("https://")) {
                    try {
                        return printCitations(inputList, gate, 1);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                        return -1;
                    }
                } else {
                    try {
                        gateway.topSearches(input);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                        return -1;
                    }
                    try {
                        return printResults(inputList, gate, 1);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                        return -1;
                    }
                }
            default:
                System.out.println("Invalid mode");
                return 0;
            }        
    }

    /**
     * Print the results of a search
     * @param results
     */
    public int printResults(List<String> input, GatewayInterface gate, int page) throws RemoteException {
        Scanner scanner = new Scanner(System.in);
        int pageSize = 10;
        while (true) {
            List<UrlInfo> results = gate.searchWord(input, page, pageSize, false);

            if (results == null) {
                System.out.println("Error searching word: no active barrels");
                return -1;
            }
            if (results.isEmpty() && page == 0) {
                System.out.println("No URLs found for the word: " + input);
            }else {
            
            for (int i = 0; i < results.size(); i++) {
                UrlInfo urlInfo = results.get(i);
                System.out.println("Result " + ((page-1)*10 + i + 1) + ":");
                System.out.println("------------------------------");
                System.out.print("Title: ");
                System.out.println(urlInfo.getTitle());
                System.out.print("Url: ");
                System.out.println(urlInfo.getUrl());
                System.out.print("Description: ");
                System.out.println(urlInfo.getDescription());
                System.out.print("Number of citations: ");
                System.out.println(urlInfo.getCountOfCitations());
                System.out.println("------------------------------");
            }
            System.out.println("Enter 'd' for next page, 'a' for previous page, or any other key to exit:");
            String userInput = scanner.nextLine();
            if (userInput.equalsIgnoreCase("d") && page != 0 && results.size() == pageSize) {
                page++;
            } else if (userInput.equalsIgnoreCase("a") && page > 0) {
                page--;
            } else {
                System.out.println("invalid input, exiting...");
                return 0;
            }
        }
    }
    }

    /**
     * Print the citations of a URL
     * @param results
     */
    public int printCitations(List<String> input, GatewayInterface gate, int page)  throws RemoteException {
        Scanner scanner = new Scanner(System.in);
        int pageSize = 10;
        while (true) {
            Set<String> results = gate.searchCitations(input, page, pageSize, false);
            List<String> citationsList = new ArrayList<>(results);

            if (results == null) {
                System.out.println("Error searching citations: no active barrels");
                return -1;
            }
            if (results.isEmpty()) {
                System.out.println("No citations found for URL " + input.get(0));
            }else {

            for (int i = 0; i < results.size(); i++) {
                String url = citationsList.get(i);
                System.out.println("Result " + ((page-1)*10 + i + 1) + ":");
                System.out.print("Url: ");
                System.out.println(url);
                System.out.println("------------------------------");

            }
            System.out.println("Enter 'd' for next page, 'a' for previous page, or any other key to exit:");
            String inp = scanner.nextLine();
            if (inp.equalsIgnoreCase("d") && page!= 0 && results.size() == pageSize) {
                page++;
            } else if (inp.equalsIgnoreCase("a") && page > 0) {
                page--;
            } else {
                System.out.println("invalid input, exiting...");
                return 0;
            }
        }
    }

    }
    /**
     * Print the main menu
     */
    public void printMainMenu() {
        System.out.println("---------------------------------");
        System.out.println("Select an option:");
        System.out.println("1. Insert URL");
        System.out.println("2. Search word or URL citations");
        System.out.println("3. Admin Statistics");
        System.out.println("4. Exit");
        System.out.println("---------------------------------");
    }
    
    @Override
    public void setId(int id) {
        Client.id = id;
    }
    @Override
    public int getId() {
        return id;
    }

    @Override
    public synchronized void setStats(List<String> top10, List<Double> averageTimes, List<BarrelInterface> activeBarrels) {
        this.top10 = top10;
        this.averageTimes = averageTimes;
        this.activeBarrels.clear();

        for (BarrelInterface barrel : activeBarrels) {
            try{
                this.activeBarrels.add(barrel.getId()); 
            } catch (RemoteException e) {
                this.activeBarrels.add(0); //pq
                continue;
            }
            
        }
    
        if (inAdminMode) {
            System.out.println("=== Received stats update ===");
            printStats();
        }
    }
    
    /**
     * Print the statistics
     */
    private void printStats(){
        System.out.println("------------------------------");   
        System.out.println("Top 10 most searched words:");
        for (int i = 0; i < top10.size(); i++) {
            System.out.println((i + 1) + ". " + top10.get(i));
        }
        System.out.println("------------------------------");   
        System.out.println("Average search times:");
        for (int i = 0; i < averageTimes.size(); i++) {
            System.out.println("Barrel " + i + ": " + averageTimes.get(i)+ "ms");
        }
        System.out.println("------------------------------"); 
        System.out.println("Active barrels:");
        for (int i = 0; i < activeBarrels.size(); i++) {
            System.out.println("Barrel " + activeBarrels.get(i) + " is active");
        }
        System.out.println("------------------------------");   

    }
    /**
     * Enter admin mode
     * @param scanner
     */
    public void adminMode(Scanner scanner) {
        inAdminMode = true;
        System.out.println("Admin mode started. Waiting for stat updates...");
        System.out.println("Type 'q' and press Enter to exit admin mode.\n");

        printStats();
    
        while (true) {
            if (scanner.hasNextLine()) {
                String input = scanner.nextLine();
                if (input.equalsIgnoreCase("q")) {
                    inAdminMode = false;
                    System.out.println("Exiting admin mode.");
                    break;
                }
            }
        }
    }
    

}