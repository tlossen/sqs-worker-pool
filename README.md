# SQS Worker Pool
A worker pool that pulls jobs from an SQS queue and executes them. 
The pool ensures that the number of in-flight jobs is never higher than the number of worker threads.

You can easily scale horizontally by running multiple pools on multiple machines which consume jobs from the same SQS queue.

## Usage
Subclass `SqsWorkerPool` and implement `handle(Message message)`. If `handle` returns without throwing an exception, the message is automatically deleted from the queue; otherwise it stays in the queue and is processed again (after the configured [visibility timeout](http://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/AboutVT.html)).

## Example

	import com.amazonaws.services.sqs.model.Message;
	
	static class ExamplePool extends SqsWorkerPool
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

## License
Copyright © 2016 Tim Lossen.
Distributed under the MIT license.