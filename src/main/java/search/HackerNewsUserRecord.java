package search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Represents a Hacker News user record.
 * This class is used to deserialize JSON responses from the Hacker News API.
 * Unknown properties from the JSON response are ignored during deserialization.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HackerNewsUserRecord(
    /**
     * The user's unique ID.
     */
    String id,
    /**
     * True if the user is deleted.
     */
    Long created,
    /**
     * The user's karma score.
     */
    Integer karma,
    /**
     * The user's submitted items.
     */
    String about,
    /**
     * The user's submitted items.
     */
    List<Integer> submitted
    
) {}
