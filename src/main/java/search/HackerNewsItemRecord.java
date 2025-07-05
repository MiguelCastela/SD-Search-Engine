package search;


import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents a Hacker News item, which can be a story, comment, poll, job, or poll option.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HackerNewsItemRecord(

    /**
     * The item's unique ID.
     */
    Integer id,

    /**
     * True if the item is deleted.
     */
    Boolean deleted,

    /**
     * The type of item. One of "job", "story", "comment", "poll", or "pollopt".
     */
    String type,

    /**
     * The username of the item's author.
     */
    String by,

    /**
     * Creation date of the item, in Unix Time.
     */
    Long time,

    /**
     * The comment, story, or poll text. May contain HTML.
     */
    String text,

    /**
     * True if the item is dead.
     */
    Boolean dead,

    /**
     * The comment's parent: either another comment or the relevant story.
     */
    String parent,

    /**
     * The pollopt's associated poll ID.
     */
    Integer poll,

    /**
     * The IDs of the item's comments, in ranked display order.
     */
    List kids,

    /**
     * The URL of the story.
     */
    String url,

    /**
     * The story's score, or the votes for a pollopt.
     */
    Integer score,

    /**
     * The title of the story, poll, or job. May contain HTML.
     */
    String title,

    /**
     * A list of related poll options, in display order.
     */
    List parts,

    /**
     * For stories or polls, the total comment count.
     */
    Integer descendants

) {
}

