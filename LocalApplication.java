import Task.DoneTask;
import Task.NewTask;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.IamInstanceProfileSpecification;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import org.apache.commons.codec.binary.Base64;

import java.io.*;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class LocalApplication {

    static private String appId;
    static private String inputPath;
    static private String outputPath;
    static private Integer n;
    static private QueueHandler newTasksQueue;
    static private QueueHandler doneTasksQueue = null;
    static private AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion("us-east-1").build();
    static private DoneTask doneTask = null;
    static private AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard().withRegion("us-east-1").build();

    public static void main(String[] args) {
        if (args.length == 3) {
            try {
                inputPath = args[0];
                outputPath = args[1];
                n = Integer.parseInt(args[2]);
                appId = UUID.randomUUID().toString();
                System.out.println("Running application with id: " + appId + " on: " + inputPath + " with n = " + n.toString() +
                        " your output can be found here: " + outputPath);

                turnOnManager();

                createTask();

                receiveDoneTask();

                saveFile();

                System.out.println("Finish");

            } catch (Exception e) {

            }
        }
    }

    private static void turnOnManager() {
        try {
            newTasksQueue = new QueueHandler(Manager.NEW_TASKS_QUEUE_NAME);
        } catch (QueueDoesNotExistException e) {
            turnOn();
        }
    }

    private static void turnOn() {
        String userData = getUserData("manager.jar");
        RunInstancesRequest instancesRequest = new RunInstancesRequest("ami-08fe4614a9f89c0ec", 1, 1);
        instancesRequest.withInstanceType(InstanceType.T2Micro)
                .withIamInstanceProfile(new IamInstanceProfileSpecification().withArn(Manager.ARN_NAME))
                .withUserData(userData)
                .withKeyName("aws-key1");
        ec2.runInstances(instancesRequest).getReservation().getInstances();
        boolean in = true;
        while (in) {
            try {
                newTasksQueue = new QueueHandler(Manager.NEW_TASKS_QUEUE_NAME);
                in = false;
            } catch (QueueDoesNotExistException e) {
            }
        }
    }

    private static void createTask() {
        File file = new File(inputPath);
        s3.putObject(Manager.BUCKET_NAME, inputPath, file);
        NewTask task = new NewTask(appId, inputPath, n);
        String body = task.toJson();
        newTasksQueue.sendMessage(body);
    }

    private static void receiveDoneTask() {
        // receive message
        doneTasksQueue = new QueueHandler(Manager.DONE_TASKS_QUEUE_NAME);
        while (true) {
            List<Message> messages = doneTasksQueue.receiveMessages();
            for (Message message : messages) {
                doneTask = DoneTask.fromJson(message.getBody());
                System.out.println(message.getBody());
                if (doneTask.getAppId() .equals(appId)) {
                    // remove from queue
                    doneTasksQueue.deleteMessage(message);
                    return;
                }
            }
        }
    }

    private static void saveFile() {
        try (S3Object o = s3.getObject(Manager.BUCKET_NAME, doneTask.getOutputKeyName());
             S3ObjectInputStream s3is = o.getObjectContent();
             BufferedReader reader = new BufferedReader(new InputStreamReader(s3is));
             BufferedWriter writer = new BufferedWriter(new FileWriter(new File(outputPath+ ".html")))
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
            }
            s3.deleteObject(Manager.BUCKET_NAME, doneTask.getOutputKeyName());
            s3.deleteObject(Manager.BUCKET_NAME, inputPath);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getUserData(String jarName) {
        String userData =
                "#!/bin/bash\n" +
                        "yum update -y\n" +
                        "yum -y install python-pip\n" +
                        "yum -y install awscli\n" +
                        "yum -y install java-1.8.0\n" +
                        "echo 2 | alternatives --config java\n" +
                        "aws s3 cp s3://" + Manager.BUCKET_NAME + "/" + jarName + " . --region us-east-1\n" +
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
