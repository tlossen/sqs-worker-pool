# SQS Worker Pool
A worker pool that pulls jobs from an SQS queue and executes them. 
The pool ensures that the number of in-flight jobs is never higher than the number of worker threads.

Multiple worker pools (distributed over multiple machines) can process jobs from the same SQS queue.


## Usage
Subclass `SqsWorkerPool` and implement `handle(Message message)`. If `handle` returns without throwing an exception, the message is automatically deleted from the queue; otherwise it stays in the queue and is processed again (after the [visibility timeout](http://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/AboutVT.html) has passed).

AWS credentials are loaded from `~/.aws/credentials`

## Example

```java
import com.amazonaws.services.sqs.model.Message;
	
class ExamplePool extends SqsWorkerPool
{
    public ExamplePool(Config config) {
        super(config);
    }

    @Override
    protected void handle(Message message) {
        System.out.println(message.getBody());
    }
}
    
ExamplePool pool = new ExamplePool(new Config("test"));
```

## Demo
Start a simple demo with 

```./gradlew run```

It queues 10 jobs, then starts a worker pool to process them. Each job has a 30% chance of succeeding. If a job fails 3 times, SQS moves it to the dead letter queue, which is handled by a second worker pool.


## License
Copyright © 2016 Tim Lossen.
Distributed under the MIT license.