package tlossen;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.SetQueueAttributesRequest;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


public class Demo
{
    static class DemoPool extends SqsWorkerPool
    {
        private AtomicInteger _todo;

        public DemoPool(Config config, AtomicInteger todo) {
            super(config);
            _todo = todo;
        }

        @Override
        protected void handle(Message message) {
            String job = message.getBody();
            if (Math.random() > 0.3) {
                System.out.println(job + " FAILED");
                throw new RuntimeException("boom");
            } else {
                _todo.decrementAndGet();
                System.out.println(job + " SUCCESS");
            }
        }
    }

    static class DeadLetterPool extends SqsWorkerPool
    {
        private AtomicInteger _todo;

        public DeadLetterPool(Config config, AtomicInteger todo) {
            super(config);
            _todo = todo;
        }

        @Override
        protected void handle(Message message) {
            String job = message.getBody();
            _todo.decrementAndGet();
            System.out.println(job + " DEAD");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // settings
        Config config = new Config("demo")
                .withRegion(Regions.EU_CENTRAL_1)
                .withPoolSize(2)
                .withVisibilityTimeout(3);

        // connect to sqs
        AWSCredentials credentials = new ProfileCredentialsProvider().getCredentials();
        AmazonSQS sqs = new AmazonSQSClient(credentials);
        sqs.setRegion(Region.getRegion(config.region));
        String queue = sqs.createQueue(config.queueName).getQueueUrl();

        // configure dead letter queue
        String deadLetterQueue = sqs.createQueue(config.queueName + "_dead_letter").getQueueUrl();
        String deadLetterArn = sqs.getQueueAttributes(deadLetterQueue, Arrays.asList("QueueArn")).getAttributes().get("QueueArn");
        sqs.setQueueAttributes(new SetQueueAttributesRequest(queue,
                Collections.singletonMap("RedrivePolicy",
                        "{\"maxReceiveCount\":\"3\", \"deadLetterTargetArn\":\"" + deadLetterArn + "\"}")));

        // create 10 jobs
        AtomicInteger todo = new AtomicInteger(10);
        for (int i = 0; i < todo.get(); i++) {
            String job = "job " + i;
            sqs.sendMessage(queue, job);
            System.out.println("created: " + job);
        }

        // process jobs until all done
        SqsWorkerPool pool = new DemoPool(config, todo);
        SqsWorkerPool deadLetterPool = new DeadLetterPool(new Config(config.queueName + "_dead_letter"), todo);
        do {
            Thread.sleep(1000);
            System.out.println("[todo = " + todo + "]");
        } while (todo.get() > 0);
        pool.stop();
        deadLetterPool.stop();
    }
}
