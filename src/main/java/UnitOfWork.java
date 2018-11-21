/*
 * Notice: This software is proprietary to CME, its affiliates, partners and/or licensors.
 * Unauthorized copying, distribution or use is strictly prohibited.  All rights reserved.
 */

import java.util.Arrays;

/**
 * @author Sebastian Demian - sebastian.demian@cmegroup.com
 * @since 9/20/18
 */
public class UnitOfWork implements Comparable<UnitOfWork> {

  private final long msgSeqNum;

  private final long sendingTime;

  private final byte[] payload;

  public UnitOfWork(long msgSeqNum, long sendingTime, byte[] payload) {
    this.msgSeqNum = msgSeqNum;
    this.sendingTime = sendingTime;
    this.payload = payload;
  }

  public long getMsgSeqNum() {
    return msgSeqNum;
  }

  public long getSendingTime() {
    return sendingTime;
  }

  public byte[] getPayload() {
    return payload;
  }

  @Override
  public int compareTo(UnitOfWork o) {
    return (int) (this.msgSeqNum - o.getMsgSeqNum());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    UnitOfWork that = (UnitOfWork) o;

    if (getMsgSeqNum() != that.getMsgSeqNum()) return false;
    if (getSendingTime() != that.getSendingTime()) return false;
    return Arrays.equals(getPayload(), that.getPayload());
  }

  @Override
  public int hashCode() {
    int result = (int) (getMsgSeqNum() ^ (getMsgSeqNum() >>> 32));
    result = 31 * result + (int) (getSendingTime() ^ (getSendingTime() >>> 32));
    result = 31 * result + Arrays.hashCode(getPayload());
    return result;
  }
}
