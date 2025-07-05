package search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.util.List;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.stereotype.Controller;
 /** 
 * @authors
 * Miguel Castela 2022212972 👍
 * Miguel Martins 2022213951 👍
 */


/**
 * This class handles requests related to indexing and searching Hacker News stories.
 * It provides endpoints to index top stories, user stories, and the top 30 stories.
 * The indexed URLs are stored using a ClientService instance.
 */

@Controller
public class HackerNewsController {
    /**
     * Logger for the HackerNewsController class.
     */
    private static final Logger logger = LoggerFactory.getLogger(HackerNewsController.class);
    /**
     * URL for the top stories endpoint.
     */
    private static final String TOP_STORIES_URL = "https://hacker-news.firebaseio.com/v0/topstories.json";
    /**
     * URL for the item details endpoint.
     */
    private static final String ITEM_DETAILS_URL = "https://hacker-news.firebaseio.com/v0/item/%d.json";
    /**
     * URL for the user details endpoint.
     */
    private final ClientService client;
    /**
     * Constructor for the HackerNewsController class.
     *
     * @param client The ClientService instance to use for indexing URLs.
     */
    public HackerNewsController(ClientService client) {
        this.client = client;
    }
    /**
     * Indexes the top stories from Hacker News based on a search term.
     *
     * @param searchTerm The search term to filter stories.
     * @param page       The page number for pagination.
     * @return A redirect to the results page with the search term and page number.
     */
    @GetMapping("/index-hackernewstopstories")
    public String indexHackerNewsTopStories(@RequestParam String searchTerm,
                                            @RequestParam(defaultValue = "1") int page) {
        RestTemplate restTemplate = new RestTemplate();
        int indexedCount = 0;

        String[] forbiddenChars = {";", "|", "\\n", ",", ".", ":", "!", "?", "{", "}", "(", ")", "[", "]", "\"", "\'"};

        try {
            Integer[] topStoryIds = restTemplate.getForObject(TOP_STORIES_URL, Integer[].class);

            if (topStoryIds != null) {
                for (int i = 0; i < Math.min(50, topStoryIds.length); i++) {
                    String itemUrl = String.format(ITEM_DETAILS_URL, topStoryIds[i]);
                    HackerNewsItemRecord item = restTemplate.getForObject(itemUrl, HackerNewsItemRecord.class);

                    if (item != null) {
                        String title = item.title() != null ? item.title().toLowerCase() : "";
                        String text = item.text() != null ? item.text().toLowerCase() : "";

                        String[] searchTerms = searchTerm.toLowerCase().trim().split("\\s+");
                        String combinedContent = (title + " " + text).toLowerCase();
                        String[] contentTokens = combinedContent.trim().split("\\s+");

                        boolean containsAllTerms = true;
                        for (String term : searchTerms) {
                            boolean found = false;
                            for (String token : contentTokens) {
                                String cleanedToken = token;
                                for (String forbiddenChar : forbiddenChars) {
                                    cleanedToken = cleanedToken.replace(forbiddenChar, "");
                                }
                                if (cleanedToken.equals(term)) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                containsAllTerms = false;
                                break;
                            }
                        }

                        if (containsAllTerms) {
                            String urlToIndex = item.url();
                            if (urlToIndex != null && !urlToIndex.isEmpty() && urlToIndex.startsWith("http")) {
                                client.putNew(List.of(urlToIndex));
                                indexedCount++;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error indexing top stories: ", e);
        }

        logger.info("Indexed {} URLs for search term '{}'", indexedCount, searchTerm);

        return "redirect:/results?searchTerm=" + URLEncoder.encode(searchTerm, StandardCharsets.UTF_8) + "&page=" + page;
    }
    /**
     * Indexes the user stories from Hacker News based on a username.
     *
     * @param username The username to filter stories.
     * @param page     The page number for pagination.
     * @return A redirect to the results page with the search term and page number.
     */
    @GetMapping("/index-user-stories")
    public String indexUserStories(@RequestParam String username,
                                @RequestParam(defaultValue = "1") int page) {
        RestTemplate restTemplate = new RestTemplate();
        int indexedCount = 0;

        try {
            // Buscar os dados do utilizador
            String userUrl = "https://hacker-news.firebaseio.com/v0/user/" + username + ".json";
            HackerNewsUserRecord user = restTemplate.getForObject(userUrl, HackerNewsUserRecord.class);

            if (user != null && user.submitted() != null) {
                for (Integer itemId : user.submitted()) {
                    String itemUrl = String.format(ITEM_DETAILS_URL, itemId);
                    HackerNewsItemRecord item = restTemplate.getForObject(itemUrl, HackerNewsItemRecord.class);

                    if (item != null && "story".equals(item.type())) {
                        String urlToIndex = item.url();
                        if (urlToIndex != null && !urlToIndex.isEmpty() && urlToIndex.startsWith("http")) {
                            client.putNew(List.of(urlToIndex));
                            indexedCount++;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error indexing user stories for '{}': ", username, e);
        }

        logger.info("Indexed {} URLs from user '{}'", indexedCount, username);

        return "redirect:/results?searchTerm=" + URLEncoder.encode(username, StandardCharsets.UTF_8) + "&page=" + page;
    }
    /**
     * Indexes the top 30 stories from Hacker News.
     *
     * @param redirectAttributes The redirect attributes to pass messages.
     * @return A redirect to the home page with a success message.
     */
    @GetMapping("/index-hackernews-top30")
    public String indexTop30HackerNewsStories(RedirectAttributes redirectAttributes) {
        RestTemplate restTemplate = new RestTemplate();
        int indexedCount = 0;

        try {
            Integer[] topStoryIds = restTemplate.getForObject(TOP_STORIES_URL, Integer[].class);

            if (topStoryIds != null) {
                for (int i = 0; i < Math.min(30, topStoryIds.length); i++) {
                    String itemUrl = String.format(ITEM_DETAILS_URL, topStoryIds[i]);
                    HackerNewsItemRecord item = restTemplate.getForObject(itemUrl, HackerNewsItemRecord.class);

                    if (item != null && item.url() != null && item.url().startsWith("http")) {
                        client.putNew(List.of(item.url()));
                        indexedCount++;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error indexing top 30 stories: ", e);
        }

        logger.info("Indexed {} top stories", indexedCount);
        redirectAttributes.addFlashAttribute("message", "Indexed " + indexedCount + " top stories successfully.");
        return "redirect:/";
    }
}