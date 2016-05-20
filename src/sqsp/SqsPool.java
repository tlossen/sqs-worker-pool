package sqsp;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.Message;

import java.util.List;
import java.util.concurrent.ExecutorService;


public class SqsPool
{
    private final ExecutorService _ex;
    private final AmazonSQS _sqs;
    private final String _queue;
    private boolean _stopped = false;

    public SqsPool(final String queueName, final int poolSize) {
        _ex = new BlockingExecutor(poolSize);
        _sqs = new AmazonSQSClient(new ProfileCredentialsProvider().getCredentials());
        _sqs.setRegion(Region.getRegion(Regions.EU_CENTRAL_1));
        _queue = _sqs.createQueue(queueName).getQueueUrl();
        new Thread(() -> {
            while (!_stopped) {
                List<Message> messages = _sqs.receiveMessage(_queue).getMessages();
                for (Message message : messages) {
                    _ex.submit(() -> handle(message));
                }
            }
        }).start();
    }

    protected void handle(Message message) {
        System.out.println(Thread.currentThread().getName() + " received: " + message.getBody());
        _sqs.deleteMessage(_queue, message.getReceiptHandle());
    }

    public void stop() {
        _stopped = true;
        _ex.shutdown();
    }


}
