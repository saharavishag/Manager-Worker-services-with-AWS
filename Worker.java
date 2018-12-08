import Task.DoneImgTask;
import Task.NewImgTask;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.util.LoadLibs;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class Worker {

    public static void main(String[] args) {
        System.out.println("Begin Worker........");
        Worker worker = new Worker();
        worker.work();
    }

    // private SQS
    private QueueHandler newImgTasksQueue;
    private QueueHandler doneImgTasksQueue;
    private ITesseract tesInstance;

    private Worker() {
        this.newImgTasksQueue = new QueueHandler(Manager.NEW_IMG_TASKS_QUEUE_NAME);
        this.doneImgTasksQueue = new QueueHandler(Manager.DONE_IMG_TASKS_QUEUE_NAME);
        this.tesInstance = new Tesseract();
    }


    private void work() {
        while (true)
            try {
                //TODO: add loop for work
                getImageMessages();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
            }
    }

    private void getImageMessages() {
        List<Message> messages = newImgTasksQueue.receiveMessages();
        messages.forEach(this::handleMessage);
    }

    private void handleMessage(Message message) {
        System.out.println("started task");
        String body = message.getBody();
        NewImgTask task = NewImgTask.fromJson(body);
        String text = applyOCR(task.getImgUrl());
        if (text == null) {
            newImgTasksQueue.sendMessage(body);
        } else {
            notifyManager(text, task);
            newImgTasksQueue.deleteMessage(message);
            System.out.println("finished task");
        }
    }

    // OCR
    private String applyOCR(String stringUrl) {
        String text = "";
        try {
            URL url = new URL(stringUrl);
            BufferedImage bufferedImage = ImageIO.read(url);
            text = this.tesInstance.doOCR(bufferedImage);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TesseractException e) {
            e.printStackTrace();
        }

        return text;
    }

    // send message / enqueue to doneImageTask
    // new DoneImgTask with text+image url
    // sqs.sendMessage...
    private void notifyManager(String text, NewImgTask newImgTask) {
        DoneImgTask task = new DoneImgTask(newImgTask.getAppId(), newImgTask.getImgUrl(), text);
        String body = task.toJson();

        doneImgTasksQueue.sendMessage(body);
    }


}
