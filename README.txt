▄▄▄▄▄                                                                   ▄▄▄▄▄
    ▀                                                                   ▀
▄▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▄
█ ░ █ --───────────────── ■  Release information   ■ ────────────────-- █ ░ █
▄▄▄▄█ ▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄ █▄▄▄▄
▀                                                                           ▀
▀                                                                           ▀
				"Fear and Loathing in the Cloud" Project

Real-world application to distributively apply OCR algorithms on images,
and then to display each image with its recognized text on a webpage.

▄▄▄▄▄                                                                   ▄▄▄▄▄
    ▀                                                                   ▀
▄▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▄
█ ░ █ --─────────── ■ Requirements for The Project ■ ────────────-- █ ░ █
▄▄▄▄█ ▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄ █▄▄▄▄
▀                                                                           ▀
▀                                                                           ▀
[ 1 ] text input file, which contains urls of images.
[ 2 ] AWS Credentials file configured in ~/.aws/credentials
[ 3 ] Requires java 8 to be installed.
[ 4 ] An IAM Role predefined with the following permissions:
	[a] AmazonSQSFullAccess
	[b] AmazonS3FullAccess
	[c] AmazonEC2FullAccess
	[d] pass role to ec2 
[ 5 ] An S3 Bucket predefined.

▄▄▄▄▄                                                                   ▄▄▄▄▄
    ▀                                                                   ▀
▄▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▄
█ ░ █ --─────────── ■   IAM Information and Machines   ■ ────────────-- █ ░ █
▄▄▄▄█ ▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄ █▄▄▄▄
▀                                                                           ▀
▀                                                                           ▀
[ 1 ] Workers:
Image: ami-08fe4614a9f89c0ec
Machine: T2.Micro

[ 2 ] Manager:
Image: ami-08fe4614a9f89c0ec
Machine: T2.Micro

▄▄▄▄▄                                                                   ▄▄▄▄▄
    ▀                                                                   ▀
▄▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▄
█ ░ █ --─────────── ■    How to run the program    ■ ────────────-- █ ░ █
▄▄▄▄█ ▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄ █▄▄▄▄
▀                                                                           ▀

Run the following command in order to run the Local Application with the necessary information:
java -jar localApp.jar inputFileName outputFileName n

[1] Local Application:
Assuming you have the images as described above, and the correct role, the Local Application will Checks if a Manager node is active on the EC2 cloud.
If it is not, the Local Application will start the Manager, upload the input file to the correct bucket in s3 and start sends a message to the Manager with the input file name,
and the Local Application id to the manager input queue.
Once it has finished to send the meesage, the Local Application listens to the output queue and waits for a message indicating that al 
the images have finished processing and the output file is ready to be downloaded.
Afterwards, local application will download the output file and will turn it into an HTML file.

[2] Manager:
Thread 1:
	The manager waits for messages in its input queue. once it receives a message it performs the following actions:
		[a] Downloading the file from S3
		[b] Parsing it into image tasks
		[c] Sending each image task as a message to the workers' input queue
	For each new application's request, the manager calulates the number of workers that are needed and
	initiating workers according to the result.
Thread 2:
	[ 1 ] continuesly check if application's requests are done.
		Once some application's tasks are done, and the tasks are submitted to workers' output queue,
		the manager receives each message from it and performs the following:
		[a] Gets all the relevate tasks from the queue.
		[b] Parse it into HTML format.
		[c] Write it to the local output file.
		For each request: if the number of messages (of the specific request) that were received in the workers output queue,
		is equal to the number of messages (of the specific request) that were sent to the workers input queue, do the following:
			[a] Sends a message to manager output queue with the filename.
			[b] Terminate workers according to the number of workers were initiated to this request.
	

[3] Worker:
	[a] Get an image message from an SQS queue.
    [b] Download the image file indicated in the message.
    [c] Apply OCR on the image.
    [d] Notify the manager of the text associated with that image.
    [e] remove the image message from the SQS queue. 

▄▄▄▄▄                                                                   ▄▄▄▄▄
    ▀                                                                   ▀
▄▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▄
█ ░ █ --─────────── ■       Questions & Answers        ■ ────────────-- █ ░ █
▄▄▄▄█ ▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄ █▄▄▄▄
▀                                                                           ▀

Q: Did you think for more than 2 minutes about security?
A: Yes, credentials to local application are provided via configuration files and the manager and workers are provided
with credentials through an IAM role.

-------------------------------------------------------------------------------------------------------------------------------

Q: Did you think about scalability?
Will your program work properly when 1 million clients connected at the same time? How about 2 million? 1 billion?
A: Our implementation supports multipule clients using a single manager's input queue.
Moreover, we create workers according to the requests to manage the work and provide fast respones.
The dynamic amount of active workers, that increases with new requests and decreases when they're done,
makes the program scalable.

-------------------------------------------------------------------------------------------------------------------------------

Q: What about persistence? What if a node dies? What if a node stalls for a while?
Have you taken care of all possible outcomes in the system? What did you do to solve it?
A: Our program works such that persistence issues are handled via AWS SQS configurations:
Our workers' input queue is configured to have long invisibility time for two reasons:
	[a] In case a worker dies during processing a task, after the invisibility period is over, another worker
	can continue working on this task.
	[b] In case a worker stalls, the long invisibility period make sure that no other worker will work on the same task
	which will cause conflicts.

-------------------------------------------------------------------------------------------------------------------------------

Q: What about broken communications?
A: We've handled communication fail cases to our best, however, some fail cases such as a message that wasn't sent properly
to the queue from a worker to the manager cannot be handled by our business program rather than Amazon's.

-------------------------------------------------------------------------------------------------------------------------------

Q: Threads in your application, when is it a good idea? When is it bad?
A: We used Threads for the manager logic business since a few logics should be happening at the same time:
Manager Threads:
	[a] Receiving messages from manger input queue for handling new requests.
	[b] Receiving messages from workers' output queue for handling done requests.

-------------------------------------------------------------------------------------------------------------------------------

Q: Did you run more than one client at the same time?
A: Yes, we did, we made some tests by running multipule local applications at once.

-------------------------------------------------------------------------------------------------------------------------------

Q: Did you manage the termination process?
A: Yes. When and manager get all the done tasks for a specific application's request, we terminate
we turn off workers accordingly.

-------------------------------------------------------------------------------------------------------------------------------

Q: Are all your workers working hard? Or some are slacking? Why?
A: There's a limited numbers of tasks that a workers can get from SQS, therefore
there is no worker that works harder than others by definition.

-------------------------------------------------------------------------------------------------------------------------------

Q: Is your manager doing more work than he's supposed to?
Have you made sure each part of your system has properly defined tasks?
A: Our manager does exactly what it is supposed to do according to the instructions.
Moreover, each thread of the manager process has a defined purpose.

-------------------------------------------------------------------------------------------------------------------------------