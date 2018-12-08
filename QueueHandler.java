import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.QueueNameExistsException;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SetQueueAttributesRequest;

import java.util.List;

public class QueueHandler {

    private String queueUrl;
    private AmazonSQS sqs;

    public QueueHandler(String queueName) {
        this.sqs = AmazonSQSClientBuilder.standard().withRegion("us-east-1").build();
        this.queueUrl = sqs.getQueueUrl(queueName).getQueueUrl();
    }

    public static QueueHandler createQueue(String queueName) {
        AmazonSQS sqs = AmazonSQSClientBuilder.standard().withRegion("us-east-1").build();
        try {
            sqs.createQueue(queueName);
        } catch (QueueNameExistsException ignored) {
        }
        return new QueueHandler(queueName);
    }

    public void sendMessage(String body) {
        SendMessageRequest send_msg_request = new SendMessageRequest()
                .withQueueUrl(queueUrl)
                .withMessageBody(body)
                .withDelaySeconds(1);
        sqs.sendMessage(send_msg_request);
    }

    public void setVisibilityTimeout(Integer timeout) {
        final SetQueueAttributesRequest request = new SetQueueAttributesRequest()
                .withQueueUrl(queueUrl)
                .addAttributesEntry("VisibilityTimeout"
                        , timeout.toString());
        this.sqs.setQueueAttributes(request);
    }

    public List<Message> receiveMessages() {
        return sqs.receiveMessage(queueUrl).getMessages();
    }

    public void deleteMessage(Message message) {
        sqs.deleteMessage(queueUrl, message.getReceiptHandle());
    }

}
