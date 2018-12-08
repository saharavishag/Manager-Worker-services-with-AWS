package Task;

import com.google.gson.Gson;

public class NewTask extends Task {

    private String imageListKeyName;
    private Integer n;

    public NewTask(String appId, String imageListKeyName, Integer n) {
        super(appId);
        this.imageListKeyName = imageListKeyName;
        this.n = n;
    }

    public static NewTask fromJson(String message) {
        return new Gson().fromJson(message, NewTask.class);
    }

    public String toJson() {
        return new Gson().toJson(this, NewTask.class);
    }

    public String getImageListKeyName() {
        return imageListKeyName;
    }

    public Integer getN() {
        return n;
    }
}
