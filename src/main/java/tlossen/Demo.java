package tlossen;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.SetQueueAttributesRequest;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;


public class Demo
{
    static class DemoPool extends SqsWorkerPool
    {
        private Set<String> _todo;

        public DemoPool(Config config, Set<String> todo) {
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
                _todo.remove(job);
                System.out.println(job + " SUCCESS");
            }
        }
    }

    static class BrokenPool extends SqsWorkerPool
    {
        private Set<String> _todo;

        public BrokenPool(Config config, Set<String> todo) {
            super(config);
            _todo = todo;
        }

        @Override
        protected void handle(Message message) {
            String job = message.getBody();
            _todo.remove(job);
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
        String brokenQueue = sqs.createQueue(config.queueName + "_broken").getQueueUrl();
        String brokenArn = sqs.getQueueAttributes(brokenQueue, Arrays.asList("QueueArn")).getAttributes().get("QueueArn");
        sqs.setQueueAttributes(new SetQueueAttributesRequest(queue,
                Collections.singletonMap("RedrivePolicy",
                        "{\"maxReceiveCount\":\"3\", \"deadLetterTargetArn\":\"" + brokenArn + "\"}")));

        // create 10 jobs
        Set<String> todo = new CopyOnWriteArraySet<>();
        for (int i = 0; i < 10; i++) {
            String job = "job " + i;
            todo.add(job);
            sqs.sendMessage(queue, job);
            System.out.println("created: " + job);
        }

        // start processing jobs
        SqsWorkerPool pool = new DemoPool(config, todo);
        SqsWorkerPool brokenPool = new BrokenPool(new Config(config.queueName + "_broken"), todo);
        while (!todo.isEmpty()) {
            System.out.println(todo);
            Thread.sleep(1000);
        }
        pool.stop();
        brokenPool.stop();
    }
}
