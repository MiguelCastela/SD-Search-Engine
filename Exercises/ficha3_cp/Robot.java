package search;

import java.rmi.registry.*;
import java.util.*;
import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

public class Robot {
    public static void main(String[] args) {
        try {
            Index index = (Index) LocateRegistry.getRegistry("194.210.38.242", 8183).lookup("index");
            while (true) {
                String url = index.takeNext();
                System.out.println(url);
                Document doc = Jsoup.connect(url).get();
                StringTokenizer st = new StringTokenizer(doc.text(), " ,.!?:;");
                while (st.hasMoreElements())
                    index.addToIndex(st.nextToken().toLowerCase(), url);

                Elements links = doc.select("a[href]");
                for (Element link : links) {
                    String foundUrl = link.attr("abs:href");
                    index.putNew(foundUrl);
                }
                System.out.println(doc);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
