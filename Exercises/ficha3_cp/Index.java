package search;

import java.rmi.*;
import java.util.*;

public interface Index extends Remote {
    String takeNext() throws RemoteException;
    void putNew(String url) throws RemoteException;
    void addToIndex(String word, String url) throws RemoteException;
    List<String> searchWord(String word) throws RemoteException;
    void handleRequest(String input) throws RemoteException;
}