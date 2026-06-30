package search;

import java.rmi.*;
import java.rmi.registry.*;
import java.util.*;

public class Client {

    public Client() throws RemoteException {}

    public static void main(String args[]) {
        try {
            Index index = (Index) LocateRegistry.getRegistry(8183).lookup("index");

            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.println("Enter an URL or search word:");
                String input = scanner.nextLine();
                index.handleRequest(input);
            }
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }
}

// https://pt.wikipedia.org/wiki/Wikipédia:Página_principal
// https://pt.wikipedia.org/wiki/Wikip%C3%A9dia:P%C3%A1gina_principal