package tlossen;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.Message;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;


public class Demo {
    static class DemoPool extends SqsWorkerPool
    {
        private Set<String> _todo;

        public DemoPool(String queueName, int poolSize, Set<String> todo) {
            super(queueName, poolSize);
            _todo = todo;
        }

        @Override
        protected void handle(Message message) {
            String job = message.getBody();
            if (Math.random() > 0.5) {
                System.out.println(job + " FAILED");
                throw new RuntimeException("boom");
            } else {
                _todo.remove(job);
                System.out.println(job + " SUCCESS");
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // connect to sqs
        AWSCredentials credentials = new ProfileCredentialsProvider().getCredentials();
        AmazonSQS sqs = new AmazonSQSClient(credentials);
        sqs.setRegion(Region.getRegion(Regions.EU_CENTRAL_1));
        String queue = sqs.createQueue("demo").getQueueUrl();

        // create 10 jobs
        Set<String> todo = new CopyOnWriteArraySet<>();
        for (int i = 0; i < 10; i++) {
            String job = "job " + i;
            todo.add(job);
            sqs.sendMessage(queue, job);
            System.out.println("created: " + job);
        }

        SqsWorkerPool pool = new DemoPool("demo", 2, todo);
        while (!todo.isEmpty()) {
            System.out.println(todo);
            Thread.sleep(1000);
        }
        pool.stop();
    }
}
