package search;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpSession;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Properties;



/**
 * The HomeController class handles the main functionality of the application.
 * It manages the home page, search functionality, and admin page.
 */
@Controller
public class HomeController {


    /**
     * The maximum number of retries for API calls.
     */
    private boolean AI_ENABLED = false;


    public HomeController() {}


    /**
     * The ClientService instance
     */
    @Autowired
    private ClientService client;


    @PostConstruct
    public void init() {
        try {
            client.registerClientOnce();
        } catch (Exception e) {
            System.out.println("Error registering client");
            e.printStackTrace();
        }
    }

    /**
     * The home page of the application.
     *
     * @param model   The model to add attributes to.
     * @param session The HTTP session.
     * @return The name of the home page view.
     */
    @GetMapping("/")
    public String home(Model model, HttpSession session) {
        session.removeAttribute("cachedSearchTerms");
        model.addAttribute("AI_ENABLED", AI_ENABLED);
        
        return "home_page";
    }


    /**
     * The API endpoint to check if AI is enabled.
     * @return
     */
    @GetMapping("/api/ai-enabled")
    @ResponseBody
    public ResponseEntity<?> getAiEnabled() {
        return ResponseEntity.ok().body(
            java.util.Collections.singletonMap("enabled", AI_ENABLED)
        );
    }


    /**
     * The API endpoint to toggle the Ollama API call.
     *
     * @param enabled The new state of the Ollama API call.
     * @return A response entity with a message indicating the new state.
     */
    @PostMapping("/toggle-ollama")
    public ResponseEntity<String> toggleOllama(@RequestParam boolean enabled) {
        AI_ENABLED = enabled;
        return ResponseEntity.ok("Ollama API call is now " + (AI_ENABLED ? "enabled" : "disabled"));
    }

    public boolean isOllamaEnabled() {
        return AI_ENABLED;
    }

    /**
     * Handles the search request from the home page.
     *
     * @param searchTerm  The search term entered by the user.
     * @param searchOption The search option selected by the user.
     * @param session     The HTTP session.
     * @return A redirect to the results page with the search term and page number.
     */

    @GetMapping("/search")
    public String handleGetSearch(@RequestParam String searchTerm,
                                  @RequestParam String searchOption,
                                  HttpSession session) {
        session.setAttribute("searchTerm", searchTerm);
        session.removeAttribute("content");
        session.setAttribute("searchOption", searchOption);
        session.setAttribute("page", 1);

        List<String> searchTerms = List.of(searchTerm.trim().split("\\s+"));
        String encodedSearchTerm = URLEncoder.encode(searchTerm, StandardCharsets.UTF_8);

        if ("index".equalsIgnoreCase(searchOption)) {
            if (searchTerms.get(0).startsWith("http://") || searchTerms.get(0).startsWith("https://")) {
                try {
                    client.putNew(List.of(searchTerm));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return "redirect:/";
            } else {
                return "redirect:/index-user-stories?username=" + encodedSearchTerm + "&page=1";
            }
        }

        return "redirect:/results?searchTerm=" + encodedSearchTerm + "&page=1";
    }

    /**
     * Displays the search results based on the search term and page number.
     *
     * @param searchTerm The search term entered by the user.
     * @param page       The page number for pagination.
     * @param session    The HTTP session.
     * @param model      The model to add attributes to.
     * @return The name of the results view.
     */
    @GetMapping("/results")
    public String showResults(@RequestParam String searchTerm,
                              @RequestParam(defaultValue = "1") int page,
                              HttpSession session,
                              Model model) {

        String searchOption = (String) session.getAttribute("searchOption");
        String admin = (String) session.getAttribute("admin");
        List<String> searchTerms = List.of(searchTerm.trim().split("\\s+"));

        @SuppressWarnings("unchecked")
        List<String> cachedSearchTerms = (List<String>) session.getAttribute("cachedSearchTerms");

        boolean isCached = cachedSearchTerms != null && cachedSearchTerms.equals(searchTerms);
        if (!isCached) {
            session.setAttribute("cachedSearchTerms", searchTerms);
        }

        try {
            if ("admin".equalsIgnoreCase(admin)) {
                return "redirect:/admin";
            }
            List<?> results = new ArrayList<>();
            if (!(searchTerms.get(0).startsWith("http://") || searchTerms.get(0).startsWith("https://"))) {
                results = client.searchWord(searchTerms, page, 11, isCached);

                // If size < 11, this is the last page
                boolean hasNextPage = results.size() == 11;
                if (!hasNextPage && results.isEmpty() && page > 1) {
                    // Try backing up to a valid page
                    int lastValidPage = page;
                    while (lastValidPage > 1) {
                        List<?> testPage = client.searchWord(searchTerms, lastValidPage - 1, 11, isCached);
                        if (!testPage.isEmpty()) {
                            return "redirect:/results?searchTerm=" + URLEncoder.encode(searchTerm, StandardCharsets.UTF_8) + "&page=" + (lastValidPage - 1);
                        }
                        lastValidPage--;
                    }
                    // No results at all
                    return "redirect:/results?searchTerm=" + URLEncoder.encode(searchTerm, StandardCharsets.UTF_8) + "&page=1";
                }
            }




            if ("search".equalsIgnoreCase(searchOption)) {
                if (searchTerms.get(0).startsWith("http://") || searchTerms.get(0).startsWith("https://")) {
                    List<String> citations = client.searchCitations(searchTerms, page, 11, isCached);
                    model.addAttribute("citations", citations);
                    model.addAttribute("results", null);
                    model.addAttribute("index", null);
                    model.addAttribute("hasNextPage", citations.size() == 11);
                } else if (AI_ENABLED && page == 1) {

                    String content = "";

                    String aiContent = (String) session.getAttribute("content");
                    System.out.println("Results: " + results);
                    
                    if (aiContent != null && !aiContent.isEmpty()) {
                        System.out.println("using cached AI content");
                        model.addAttribute("content", aiContent);
                    } else {
                        System.out.println("AI content is empty!!!!!!!!!!!!");
                        content = "";
                        String endpoint = "http://localhost:11434/api/chat";
                        String prompt = "SEARCH TEARM: " +searchTerms.get(0);
                        

                        StringBuilder context = new StringBuilder();
                        if(results.size() > 0) {  
                            context.append("results: "); 
                            for (int i = 0; i < Math.min(results.size(), 3); i++) {
                                if (results.get(i) instanceof UrlInfo) {
                                    UrlInfo urlInfo = (UrlInfo) results.get(i);
                                    String title = urlInfo.getTitle();
                                    String description = urlInfo.getDescription();
                                    context.append("result").append(i+1).append(": ")
                                    .append("Title: ").append(title)
                                        .append("Description: ").append(description);
                                }
                            }
                        }

                        content = context.toString();
                        prompt += context.toString();

                        
                        // Now add the context to the prompt
                            if (endpoint != null && !endpoint.isEmpty()) {
                                try {

                                    System.out.println("Making API call to Ollama...");
                                    URL url = new URI(endpoint).toURL();
                                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                                    conn.setRequestMethod("POST");
                                    conn.setRequestProperty("Content-Type", "application/json");
                                    conn.setDoOutput(true);
                            
                                    String requestBody = "{"
                                        + "\"model\": \"llama3:8b-instruct-q4_0\","
                                        + "\"messages\": ["
                                        + "{\"role\": \"system\", \"content\": \"You are part of a search engine. Your only job is to give short contextual and true analysis of title and the small part of the websites you are fed (description). Do not introduce your answer. Be clear and direct about the topic you are talking about.\"},"
                                        + "{\"role\": \"user\", \"content\": \"Based on the following search terms and possibly some of the results, provide a short contextual analysis (less than 15 words) and include 3 emojis: " + prompt + "\"}"
                                        + "],"
                                        + "\"stream\": false"
                                        + "}";

                                    conn.getOutputStream().write(requestBody.getBytes());
                                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                                    String line;
                                    StringBuilder response = new StringBuilder();
                                    while ((line = reader.readLine()) != null) {
                                        response.append(line);
                                    }
                                    reader.close();

                                    content = response.toString();



                                    JsonNode messageNode = new ObjectMapper().readTree(content).path("message");
                                    
                                    if (messageNode != null) {
                                        content = messageNode.get("content").asText();

                                    content = content.replaceAll("The search tearm \".*?\":", "").trim();
                                    content = content.replaceAll("\\\\n", "\n");
                                    content = content.replaceAll("\\\\", "");
                                    }

                                    conn.disconnect();
                                } catch (IOException e) {
                                    System.out.println("Exception occurred during API call: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            } else {
                                System.err.println("API key not set or is empty.");
                            }
                        
                        
                        session.setAttribute("content", content);
                        model.addAttribute("content", content);
                        
                        }
                        model.addAttribute("results", results);
                        model.addAttribute("citations", null);
                        model.addAttribute("index", null);
                        model.addAttribute("hasNextPage", results.size() == 11);

                }
                else {
                    // Get message loaded on page 1 if AI is enabled, to the rest of the pages
                    System.out.println("AI_ENABLED: " + AI_ENABLED);
                    if (AI_ENABLED){ 
                        String aiContent = (String) session.getAttribute("content"); 
                        model.addAttribute("content", aiContent);  
                    }


                    model.addAttribute("results", results);
                    model.addAttribute("citations", null);
                    model.addAttribute("index", null);
                    model.addAttribute("hasNextPage", results.size() == 11);
                }
            } else {
                return "redirect:/";
            }

            model.addAttribute("searchTerm", searchTerm);
            model.addAttribute("page", page);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "results";
    }

    /**
     * Displays the admin page with statistics.
     *
     * @param model The model to add attributes to.
     * @return The name of the admin page view.
     */
    @GetMapping("/admin")
    public String showAdminPage(Model model) {
        client.updateStats();

        model.addAttribute("top10", client.getTop10());
        model.addAttribute("averageTimes", client.getAverageTimes());
        model.addAttribute("activeBarrels", client.getActiveBarrels());
        model.addAttribute("barrelSize", client.getBarrelSize());

        return "admin_page";
    }
}

