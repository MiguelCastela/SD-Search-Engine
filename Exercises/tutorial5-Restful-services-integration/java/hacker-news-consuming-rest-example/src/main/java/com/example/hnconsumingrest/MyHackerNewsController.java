package com.example.hnconsumingrest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@RestController
public class MyHackerNewsController {
    private static final Logger logger = LoggerFactory.getLogger(MyHackerNewsController.class);
    private static final String TOP_STORIES_URL = "https://hacker-news.firebaseio.com/v0/topstories.json";
    private static final String ITEM_DETAILS_URL = "https://hacker-news.firebaseio.com/v0/item/%d.json";

    @GetMapping("/hackernewstopstories")
    public List<HackerNewsItemRecord> hackerNewsTopStories(@RequestParam(required = false) String search) {
        RestTemplate restTemplate = new RestTemplate();
        List<HackerNewsItemRecord> filteredStories = new ArrayList<>();

        try {
            // Fetch top story IDs
            Integer[] topStoryIds = restTemplate.getForObject(TOP_STORIES_URL, Integer[].class);

            if (topStoryIds != null) {
                // Fetch details for the top 50 stories
                for (int i = 0; i < Math.min(50, topStoryIds.length); i++) {
                    String itemUrl = String.format(ITEM_DETAILS_URL, topStoryIds[i]);
                    HackerNewsItemRecord item = restTemplate.getForObject(itemUrl, HackerNewsItemRecord.class);

                    // Filter by search term if provided
                    if (item != null && (search == null || item.title().toLowerCase().contains(search.toLowerCase()))) {
                        filteredStories.add(item);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching top stories: ", e);
        }

        return filteredStories;
    }
}
