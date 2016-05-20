package sqsp;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;


public class Main
{
    public static void main(String[] args) throws InterruptedException
    {
        // sender
        new Thread(() -> {
            AWSCredentials credentials = new ProfileCredentialsProvider().getCredentials();
            AmazonSQS sqs = new AmazonSQSClient(credentials);
            sqs.setRegion(Region.getRegion(Regions.EU_CENTRAL_1));
            String queue = sqs.createQueue("demo").getQueueUrl();

            for (int i = 0; i < 10; i++) {
                System.out.println("sending: message " + i);
                sqs.sendMessage(queue, "message " + i);
                try {
                    Thread.sleep((long) (Math.random() * 1000));
                } catch (InterruptedException ignored) {}

            }
        }).start();

        // receiver
        SqsPool pool = new SqsPool("demo", 2);
    }
}
