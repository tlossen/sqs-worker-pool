# SQS Worker Pool
A worker pool that pulls jobs from an SQS queue and executes them. 
The pool ensures that the number of in-flight jobs is never higher than the number of worker threads.

Multiple worker pools (distributed over multiple machines) can process jobs from the same SQS queue.


## Usage
Subclass `SqsWorkerPool` and implement `handle(Message message)`. If `handle` returns without throwing an exception, the message is automatically deleted from the queue; otherwise it stays in the queue and is processed again (after the [visibility timeout](http://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/AboutVT.html) has passed).

## Example

```java
import com.amazonaws.services.sqs.model.Message;
	
class ExamplePool extends SqsWorkerPool
{
    public ExamplePool(String queueName, int poolSize) {
        super(queueName, poolSize);
    }

    @Override
    protected void handle(Message message) {
        System.out.println(message.getBody());
    }
}
    
ExamplePool pool = new ExamplePool("hello", 4);
```

## Demo
With `./gradlew run` you can start a simple demo.
It queues 10 jobs, then starts a worker pool to process them. Each job has a 50% chance of succeeding.

## Todo
* make visibility timeout configurable
* make AWS region configurable

## License
Copyright © 2016 Tim Lossen.
Distributed under the MIT license.