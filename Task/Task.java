package Task;

public abstract class Task {

    private String appId;

    public Task(String appId) {
        this.appId = appId;
    }

    public String getAppId() {
        return appId;
    }
}
