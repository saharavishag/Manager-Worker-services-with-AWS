package Task;

import com.google.gson.Gson;

public class DoneImgTask extends Task {
    private String url;
    private String text;

    public DoneImgTask(String appId, String url, String text) {
        super(appId);
        this.url = url;
        this.text = text;
    }
    public static DoneImgTask fromJson(String message) {
        return new Gson().fromJson(message, DoneImgTask.class);
    }

    public String toJson() {
        return new Gson().toJson(this, DoneImgTask.class);
    }

    public String getUrl() {
        return url;
    }

    public String getText() {
        return text;
    }
}
