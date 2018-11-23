

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * @author Sebastian Demian - sebastian.demian@cmegroup.com
 * @since 9/20/18
 */
public class OrderedPubSubListener {

  // - the role of the ordered set is to remove duplicate messages and maintain order based on the MsgSeqNum attribute
  private static final ConcurrentSkipListSet<UnitOfWork> sortedMessageSet = new ConcurrentSkipListSet<>();

  static final String PUBLISH_SEQ_NUM_ATTRIBUTE = "MsgSeqNum";

  static final String SENDING_TIME_ATTRIBUTE = "SendingTime";

  // Recommendation: from our perf tests, we found that in order to keep up with consuming 10000 messages/second
  // we need to set this configuration to 4 on a 4 core virtual machine (defaults to 1). Please tune it empirically.
  private static final int PARALLEL_PULL_COUNT = 4;


  static class PubSubMessageReceiver implements MessageReceiver {

    private static long counter = 0;

    private UnitOfWork convertMessageToUnitOfWork(PubsubMessage message){

      //extract sequence Number
      String publishSeqNumAttribute = message.getAttributesOrDefault(PUBLISH_SEQ_NUM_ATTRIBUTE, "0");
      long publishSeqNum = Long.parseLong(publishSeqNumAttribute);

      //extract sending time
      String sendingTimeAttribute = message.getAttributesOrDefault(SENDING_TIME_ATTRIBUTE, "0");
      long sendingTime = Long.parseLong(sendingTimeAttribute);

      //extract payload (sbe message)
      byte[] payload = message.toByteArray();

       // - return a new unit of work
       // - alternatively introduce an object pool here to reduce object creation
      return new UnitOfWork(publishSeqNum, sendingTime, payload);
    }


    @Override
    public void receiveMessage(PubsubMessage message, AckReplyConsumer consumer) {

      UnitOfWork unitOfWork = convertMessageToUnitOfWork(message);

      sortedMessageSet.add(unitOfWork);

      counter++;

      if (counter % 1000 == 0){
        System.out.println("Added " + counter + " messages to the set");
      }

      consumer.ack();
    }
  }

  OrderedPubSubListener(String keyPath, String projectId, String subscription) {

    GoogleCredentials credentials = null;
    try {
      credentials = GoogleCredentials.fromStream(new FileInputStream(keyPath));
    } catch (IOException e) {
      System.err.println("Could not load credentials from :" + keyPath + e);
      System.exit(-1);
    }

    ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(projectId, subscription);

    Subscriber subscriber = Subscriber.newBuilder(subscriptionName, new PubSubMessageReceiver())
          .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
          .setParallelPullCount(PARALLEL_PULL_COUNT)
          .build();
      subscriber.startAsync().awaitRunning();
    }



  public void start() throws InterruptedException {

    while (sortedMessageSet.isEmpty()){
      // - if we start listener before we start publisher there will be no messages in the topic
      // - wait here until we receive the first few messages
      Thread.sleep(1000);
      System.out.println("Waiting for publisher to be started...");
    }

    // - allow first messages to come in and get ordered before we start processing
    System.out.println("Buffering for 10 seconds...\n");
    Thread.sleep(10000);
    System.out.println("Internal sorted set size after buffering is: " + sortedMessageSet.size());

    //remove the first element from the set and initialize the first sequence number
    long currentSequenceNumber = sortedMessageSet.pollFirst().getMsgSeqNum();

    while (true){

      //if set is ever emptied, wait for new messages
      if (sortedMessageSet.isEmpty()){
        continue;
      }

      long nextSetSequenceNumber = sortedMessageSet.first().getMsgSeqNum();

      //if the next message in the set has correct sequence, retrieve it
      if (nextSetSequenceNumber == currentSequenceNumber + 1){

        //remove the next element
        UnitOfWork unitOfWork = sortedMessageSet.pollFirst();

        System.out.println("Received message with Sequence Number: " + unitOfWork.getMsgSeqNum());

        // More usages:
        //long sendingTime = unitOfWork.getSendingTime();
        //byte[] payload = unitOfWork.getPayload();

        currentSequenceNumber++;


      }
    }
  }
}



