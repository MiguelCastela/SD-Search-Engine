package search ;

import java.util.HashSet;
import java.util.Set;
import java.io.Serializable;

/**
 * @authors
 * Miguel Castela 2022212972 👍
 * Miguel Martins 2022213951 👍
 */

/**
 * This class represents a URL information object.
 * It contains the URL, title, description, and a set of citations.
*/
public class UrlInfo implements Serializable {
    /**
     * Serial version UID
     */
    private static final long serialVersionUID = 1L;
    /**
     * URL
     */
    private String url;
    /**
     * Title
     */
    private String title;
    /**
     * Description
     */
    private String description;
    /**
     * List of citations
     */
    private Set<String> citations;
    /**
     * Count of citations
     */
    private int count_of_citations;

    public UrlInfo(String url, String title, String description) {
        this.url = url;
        this.title = title;
        this.description = description;
        this.citations = new HashSet<>();
        this.count_of_citations = 0;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<String> getCitations() {
        return citations;
    }

    public void setCitations(Set<String> citations) {
        this.citations = citations;
    }

    public int getCountOfCitations() {
        return count_of_citations;
    }

    public void setCountOfCitations(int count_of_citations) {
        this.count_of_citations = count_of_citations;
    }

    public void addCitation(String citation) {
        this.citations.add(citation);
        this.count_of_citations = this.citations.size();
    }
}