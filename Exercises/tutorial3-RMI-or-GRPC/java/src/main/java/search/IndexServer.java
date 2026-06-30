package search;

import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.util.concurrent.*;
import java.io.*;
import java.util.*;

public class IndexServer extends UnicastRemoteObject implements Index {
    private Queue<String> urlsToIndex;
    private Map<String, List<String>> indexedItems;

    public IndexServer() throws RemoteException {
        super();
        urlsToIndex = new ConcurrentLinkedQueue<String>();
        indexedItems = new ConcurrentHashMap<String, List<String>>();   
    }

    public static void main(String args[]) {
        try {
            IndexServer server = new IndexServer();
            Registry registry = LocateRegistry.createRegistry(8183);
            registry.rebind("index", server);
            System.out.println("Server ready. Waiting for input...");

            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.println("1. Add URL for indexing");
                System.out.println("2. Search indexed URLs");
                System.out.println("3. Exit");
                int choice = scanner.nextInt();
                scanner.nextLine(); // consume newline

                if (choice == 1) {
                    System.out.println("Enter URL:");
                    String url = scanner.nextLine();
                    server.putNew(url);
                } else if (choice == 2) {
                    System.out.println("Enter word to search:");
                    String word = scanner.nextLine();
                    List<String> results = server.searchWord(word);
                    System.out.println("Search results: " + results);
                } else if (choice == 3) {
                    break;
                } else {
                    System.out.println("Invalid choice. Try again.");
                }
            }
            scanner.close();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    private long counter = 0, timestamp = System.currentTimeMillis();;

    public String takeNext() throws RemoteException {
        //TODO: not implemented fully. Prefer structures that return in a push/pop fashion
        return urlsToIndex.poll();
    }

    public void putNew(String url) throws java.rmi.RemoteException {
        //TODO: Example code. Must be changed to use structures that have primitives such as .add(...)
        urlsToIndex.add(url);

    }

    public void addToIndex(String word, String url) throws java.rmi.RemoteException {
        indexedItems.computeIfAbsent(word, k -> new ArrayList<String>()).add(url);
    }

    
    public List<String> searchWord(String word) throws java.rmi.RemoteException {
        return indexedItems.getOrDefault(word, new ArrayList<>());
    }
}
