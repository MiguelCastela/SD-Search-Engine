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
        urlsToIndex = new ConcurrentLinkedQueue<>();
        indexedItems = new ConcurrentHashMap<>();
    }

    public static void main(String args[]) {
        try {
            IndexServer server = new IndexServer();
            Registry registry = LocateRegistry.createRegistry(8183);
            registry.rebind("index", server);
            System.out.println("Server ready. Waiting for client requests...");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private long counter = 0, timestamp = System.currentTimeMillis();

    public String takeNext() throws RemoteException {
        return urlsToIndex.poll();
    }

    public void putNew(String url) throws java.rmi.RemoteException {
        urlsToIndex.add(url);
    }

    public void addToIndex(String word, String url) throws java.rmi.RemoteException {
        indexedItems.computeIfAbsent(word, k -> Collections.synchronizedList(new ArrayList<>())).add(url);
    }

    public List<String> searchWord(String word) throws java.rmi.RemoteException {
        return indexedItems.getOrDefault(word, new ArrayList<>());
    }

    public void handleRequest(String input) throws RemoteException {
        System.out.println("Received request: " + input);
        if (input.startsWith("http:") || input.startsWith("https:")) {
            putNew(input);
        } else {
            for (String foundUrl : searchWord(input)) {
                putNew(foundUrl);
            }
        }
    }
}