# Pub Sub Sequencer

This is a sample implementation for consuming Market Data from Google Pub Sub in an ordered fashion.

Today, Google PubSub does not support ordering due to [architectural reasons](https://cloud.google.com/pubsub/docs/ordering).
 
Moreover, it can introduce duplicates along the way which need to be discarded by the consumer.
 
CME Market data needs to be consumed in an ordered fashion and without duplicates.
 


# Solution:

Clients will have to implement a snippet of code which will take care of duplicate removal and ordering of messages in their consumer process.

In order to accommodate ordering, CME will be attaching metadata to each message published on a PubSub topic. 

The metadata fields are:
```java
* SendingTime - Publish Timestamp in milliseconds
* MsgSeqNum   - Publish Sequence Number (uInt16)*
* Channel     - Current market data channel
 ```
 

On the consumer end, we recommend introducing a sorted set (eg: [ConcurrentSkipListSet](https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ConcurrentSkipListSet.html)) which removes duplicates and maintains ordering at insertion time according to the MsgSeqNum attribute. 

The client will consume messages from the sorted set, under one condition: consume the next message from the set only if it has the correct sequence number (current sequence number + 1).

If the next correct sequence number is not available, keep retrying until that message arrives and only then consume it.


Notes:
* For simplicity reasons we are parsing the MsgSeqNum directly as long. In real production scenarios clients will have to account for
the fact that MsgSeqNum is unsigned and parse it accordingly.