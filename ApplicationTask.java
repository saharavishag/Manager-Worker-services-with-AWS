import Task.DoneImgTask;
import com.amazonaws.services.ec2.model.Instance;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ApplicationTask {

    private Integer taskCount;
    private Integer workersNumOfTasks;
    private Integer ratioActiveWorkers;
    private List<Instance> instances;

    private FileWriter fileWriter;

    public ApplicationTask(Integer taskCount, Integer workersNumOfTasks, List<Instance> instances,String filename) {
        this.instances = instances;
        try {
            this.fileWriter = new FileWriter(filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.taskCount = taskCount;
        this.workersNumOfTasks = workersNumOfTasks;
        this.ratioActiveWorkers = taskCount / workersNumOfTasks;
    }

    public Integer getTaskCount() {
        return taskCount;
    }

    public void decreaseTaskCount(){
        --this.taskCount;
    }

    public Integer getRatioActiveWorkers() {
        return ratioActiveWorkers;
    }

    public List<Instance> getInstances() {
        return instances;
    }

    public FileWriter getFileWriter() {
        return fileWriter;
    }
}
