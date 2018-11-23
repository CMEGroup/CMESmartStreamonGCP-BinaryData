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


#LICENSE
BSD 3-Clause License

Copyright (c) 2018, CME Group Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

* Neither the name of the copyright holder nor the names of its
  contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
