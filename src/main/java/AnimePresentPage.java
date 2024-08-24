import net.sandrohc.jikan.model.common.Images;
import org.telegram.telegrambots.meta.api.objects.InputFile;

public class AnimePresentPage {

    public Images images;

    public String title;
    public String url;

    public int episodes;

    public Images getImages() {
        return images;
    }

    public void setImages(Images images) {
        this.images = images;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getEpisodes() {
        return episodes;
    }

    public void setEpisodes(int episodes) {
        this.episodes = episodes;
    }

    @Override
    public String toString() {
        return
                "title=" + title + '\'' +
                ", url=" + url + '\'' +
                ", episodes=" + episodes;
    }
}
