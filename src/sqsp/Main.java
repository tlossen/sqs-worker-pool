package sqsp;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.Message;

import java.util.concurrent.Executors;


public class Main {
    static class DemoPool extends SqsPool {
        public DemoPool(String queueName, int poolSize) {
            super(queueName, poolSize);
        }

        @Override
        protected void handle(Message message) {
            if (Math.random() > 0.5) {
                System.out.println(message.getBody() + " FAILURE");
                throw new RuntimeException("BOOM");
            } else {
                System.out.println(message.getBody() + " SUCCESS");
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // sender
        Executors.newSingleThreadExecutor().submit(
            () -> {
                AWSCredentials credentials = new ProfileCredentialsProvider().getCredentials();
                AmazonSQS sqs = new AmazonSQSClient(credentials);
                sqs.setRegion(Region.getRegion(Regions.EU_CENTRAL_1));
                String queue = sqs.createQueue("demo").getQueueUrl();

                for (int i = 0; i < 10; i++) {
                    System.out.println("sending: message " + i);
                    sqs.sendMessage(queue, "message " + i);
                }
            }
        );

        // receiver
        SqsPool pool = new DemoPool("demo", 2);
    }
}
