import Task.DoneImgTask;
import Task.DoneTask;
import Task.NewImgTask;
import Task.NewTask;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.sqs.model.Message;
import org.apache.commons.codec.binary.Base64;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Manager {

    public static final String NEW_TASKS_QUEUE_NAME = "newtasksqueue50664856-3e0a-4e76-8e16-3cca613345af";
    public static final String DONE_TASKS_QUEUE_NAME = "donetasksqueuea08aad17-26b1-413a-9909-826ba3ca44ba";
    public static final String NEW_IMG_TASKS_QUEUE_NAME = "newimgtasksqueueb103541c-5678-4455-a5e1-a9f2247c8ff1";
    public static final String DONE_IMG_TASKS_QUEUE_NAME = "doneimgtasksqueue1f74c625-45d6-4e84-8f38-d9f109e38d82";
    public static final String BUCKET_NAME = "s3bucket41cc7a81-9f41-414f-93ae-fb1120b378db";
    public static final String ARN_NAME = "arn:aws:iam::004316429689:instance-profile/ec2Role";
    private ConcurrentHashMap<String, ApplicationTask> applicationTasks;
    private ConcurrentLinkedQueue<Instance> runningInstances;
    private int numOfWorkers;
    // private SQS
    private QueueHandler newTasksQueue;
    private QueueHandler doneTasksQueue;
    private QueueHandler newImgTasksQueue;
    private QueueHandler doneImgTasksQueue;
    private AmazonS3 s3;
    private AmazonEC2 ec2;

    public static void main(String[] args) {
        System.out.println("Starting manager........\n");
        Manager manager = new Manager();
        new Thread(manager::manageNewTasks).start();
        manager.manageDoneTasks();

    }

    public Manager() {
        this.numOfWorkers = 0;
        this.applicationTasks = new ConcurrentHashMap<>();
        this.runningInstances = new ConcurrentLinkedQueue<>();
        this.s3 = AmazonS3ClientBuilder.standard().withRegion("us-east-1").build();
        this.ec2 = AmazonEC2ClientBuilder.standard()
                .withRegion("us-east-1")
                .build();
        this.newImgTasksQueue = QueueHandler.createQueue(NEW_IMG_TASKS_QUEUE_NAME);
        this.newImgTasksQueue.setVisibilityTimeout(60);
        this.doneImgTasksQueue = QueueHandler.createQueue(DONE_IMG_TASKS_QUEUE_NAME);
        this.doneTasksQueue = QueueHandler.createQueue(DONE_TASKS_QUEUE_NAME);
        this.newTasksQueue = QueueHandler.createQueue(NEW_TASKS_QUEUE_NAME);

    }

    public void manageNewTasks() {
        while (true) {
            List<Message> messages = newTasksQueue.receiveMessages();
            messages.forEach(this::handleNewTask);
            messages.forEach(newTasksQueue::deleteMessage);
        }
    }

    public void manageDoneTasks() {
        //TODO add loop
        while (true) {
            List<Message> messages = doneImgTasksQueue.receiveMessages();
            messages.forEach(this::handleDoneImgTask);
            List<String> toRemove = new ArrayList<>();
            this.applicationTasks.forEach((key, applicationTask) -> {
                if (applicationTask.getTaskCount() == 0) {
                    toRemove.add(key);
                }
            });
            toRemove.forEach(this::createDoneTaskResponse);
            toRemove.forEach((key) -> {

                int n = applicationTasks.get(key).getRatioActiveWorkers();
                shutdownWorkers(n, key);
                applicationTasks.remove(key);
            });
        }
    }

    private void shutdownWorkers(int n, String key) {
        TerminateInstancesRequest request = new TerminateInstancesRequest();
        List<Instance> toRemove = applicationTasks.get(key).getInstances();
        List<String> instanceIds = toRemove.stream().map((Instance::getInstanceId)).collect(Collectors.toList());
        request.withInstanceIds(instanceIds);
        ec2.terminateInstances(request);
        runningInstances.removeAll(toRemove);
    }

    public void createLocalOutputFile(String key) {
        String startTemplate = "<html>\n" + "<title>OCR</title>\n" + "<body>\n";
        String fileName = key + "_output";
        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write(startTemplate);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void createDoneTaskResponse(String key) {
        String fileName = uploadResponse(key);
        DoneTask task = new DoneTask(key, fileName);
        String body = task.toJson();
        doneTasksQueue.sendMessage(body);
    }

    private String uploadResponse(String key) {
        String endTemplate = "</body>\n" + "<html>";
        String fileName = key + "_output";
        try {
            applicationTasks.get(key).getFileWriter().append(endTemplate);
            applicationTasks.get(key).getFileWriter().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        s3.putObject(BUCKET_NAME, fileName, new File(fileName));
        return fileName;
    }

    public void writeResponseToFile(DoneImgTask task, String key) {
        try {
            applicationTasks.get(key).getFileWriter().append(createHtmlImgTemplate(task));
        } catch (IOException e1) {
            e1.printStackTrace();
        }

    }


    private String createHtmlImgTemplate(DoneImgTask task) {
        String text = task.getText().replace("\n", "<br/>\n");
        return "\t<p>\n" +
                "\t\t<img src=\"" + task.getUrl() + "\"><br/>\n" +
                "\t\t" + text + "\n" +
                "\t</p>\n";
    }

    private void handleDoneImgTask(Message message) {
        System.out.println("started done img task");
        String body = message.getBody();
        DoneImgTask task = DoneImgTask.fromJson(body);
        String appId = task.getAppId();
        writeResponseToFile(task, appId);
        this.applicationTasks.get(appId).decreaseTaskCount();
        doneImgTasksQueue.deleteMessage(message);
        System.out.println("finished done img task");
    }

    private void handleNewTask(Message message) {
        System.out.println("started new task");
        String body = message.getBody();
        NewTask task = NewTask.fromJson(body);
        String appId = task.getAppId();
        createLocalOutputFile(appId);
        String imageListKeyName = task.getImageListKeyName();
        List<String> urlList = downloadImageList(imageListKeyName);
        List<Instance> newInstances = turnOnWorkers(task.getN());
        String filename = appId + "_output";
        ApplicationTask app = new ApplicationTask(urlList.size(), task.getN(), newInstances, filename);
        this.applicationTasks.put(appId, app);
        urlList.forEach(url -> {
            System.out.println(url);
            createImageTask(url, appId);
        });

    }

    private List<Instance> turnOnWorkers(Integer n) {
        String userData = getUserData("worker.jar");
        RunInstancesRequest instancesRequest = new RunInstancesRequest("ami-08fe4614a9f89c0ec", n, n);
        instancesRequest.withInstanceType(InstanceType.T2Micro)
                .withIamInstanceProfile(new IamInstanceProfileSpecification().withArn(ARN_NAME))
                .withUserData(userData)
                .withKeyName("aws-key1");
        List<Instance> newInstances = ec2.runInstances(instancesRequest).getReservation().getInstances();
        this.runningInstances.addAll(newInstances);
        return newInstances;
    }

    private List<String> downloadImageList(String keyName) {
        List<String> urlList = null;
        try (S3Object o = s3.getObject(BUCKET_NAME, keyName);
             S3ObjectInputStream s3is = o.getObjectContent();
             BufferedReader reader = new BufferedReader(new InputStreamReader(s3is))) {
            urlList = reader.lines().collect(Collectors.toList());

        } catch (AmazonServiceException e) {
            System.err.println(e.getErrorMessage());
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return urlList;
    }

    private void createImageTask(String url, String appId) {
        NewImgTask task = new NewImgTask(appId, url);
        String body = task.toJson();
        newImgTasksQueue.sendMessage(body);
        System.out.println("finished new img task");
    }


    public static String getUserData(String jarName) {
        String userData =
                "#!/bin/bash\n" +
                        "yum update -y\n" +
                        "yum -y install python-pip\n" +
                        "yum -y install awscli\n" +
                        "yum -y install java-1.8.0\n" +
                        "echo 2 | alternatives --config java\n" +
                        "aws s3 cp s3://" + BUCKET_NAME + "/" + jarName + " . --region us-east-1\n" +
                        "pwd\n" +
                        "java -jar " + jarName + "\n";
        String base64UserData = null;
        try {
            base64UserData = new String(Base64.encodeBase64(userData.getBytes("UTF-8")), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return base64UserData;
    }

}








