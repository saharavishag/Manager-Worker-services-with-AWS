package Task;

import com.google.gson.Gson;

public class DoneTask extends Task {
    private String outputKeyName;

    public DoneTask(String appId, String outputKeyName) {
        super(appId);
        this.outputKeyName = outputKeyName;
    }

    public static DoneTask fromJson(String message) {
        return new Gson().fromJson(message, DoneTask.class);
    }

    public String toJson() {
        return new Gson().toJson(this, DoneTask.class);
    }

    public String getOutputKeyName() {
        return outputKeyName;
    }
}
