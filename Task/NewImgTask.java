package Task;

import com.google.gson.Gson;

public class NewImgTask extends Task {
    private String imgUrl;

    public NewImgTask(String appId, String imgUrl) {
        super(appId);
        this.imgUrl = imgUrl;
    }

    public static NewImgTask fromJson(String message) {
        return new Gson().fromJson(message, NewImgTask.class);
    }

    public String toJson() {
        return new Gson().toJson(this, NewImgTask.class);
    }

    public String getImgUrl() {
        return imgUrl;
    }
}
