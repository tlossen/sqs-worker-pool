package tlossen;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.Message;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public abstract class SqsWorkerPool
{
    private final ExecutorService _executor;
    private final AmazonSQS _sqs;
    private final String _queue;
    private boolean _stopped = false;

    public SqsWorkerPool(final String queueName, final int poolSize) {
        _executor = new BlockingExecutor(poolSize);
        _sqs = new AmazonSQSClient(new ProfileCredentialsProvider().getCredentials());
        _sqs.setRegion(Region.getRegion(Regions.EU_CENTRAL_1));
        _queue = _sqs.createQueue(queueName).getQueueUrl();
        Executors.newSingleThreadExecutor().submit(() -> fetcher());
    }

    public void stop() {
        _stopped = true;
        _executor.shutdown();
    }

    protected abstract void handle(Message message);

    private void fetcher() {
        while (!_stopped) {
            try {
                List<Message> messages = _sqs.receiveMessage(_queue).getMessages();
                for (Message message : messages) {
                    _executor.submit(() -> process(message));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void process(Message message) {
        try {
            handle(message);
            _sqs.deleteMessage(_queue, message.getReceiptHandle());
        } catch (Exception ignored) {
            // message is automatically retried
        }
    }
}
