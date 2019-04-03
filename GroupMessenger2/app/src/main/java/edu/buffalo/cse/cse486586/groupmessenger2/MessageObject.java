package edu.buffalo.cse.cse486586.groupmessenger2;


import java.io.Serializable;
import java.util.Comparator;

/**
 *
 * MessageObject is object that contains all message parameters that is in communication between the
 * avds
 *
 * @author apurbama
 *
 *
 */



public class MessageObject implements Serializable {
    private String message;
    private int fifoOrder;
    private int proposedPriority;
    private int agreedPriority;
    private String avdID;
    private boolean deliverStatus;

    public MessageObject() {
    }


    public MessageObject(String message, int priority, int proposedPriority, int agreedPriority, String avdID, boolean deliverStatus) {
        this.message = message;
        this.fifoOrder = priority;
        this.proposedPriority = proposedPriority;
        this.agreedPriority = agreedPriority;
        this.avdID = avdID;
        this.deliverStatus = deliverStatus;
    }



    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getFifoOrder() {
        return fifoOrder;
    }

    public void setFifoOrder(int fifoOrder) {
        this.fifoOrder = fifoOrder;
    }

    public int getProposedPriority() {
        return proposedPriority;
    }

    public void setProposedPriority(int proposedPriority) {
        this.proposedPriority = proposedPriority;
    }

    public int getAgreedPriority() {
        return agreedPriority;
    }

    public void setAgreedPriority(int agreedPriority) {
        this.agreedPriority = agreedPriority;
    }

    public String getAvdID() {
        return avdID;
    }

    public void setAvdID(String avdID) {
        this.avdID = avdID;
    }

    public boolean getDeliverStatus() {
        return deliverStatus;
    }

    public void setDeliverStatus(boolean deliverStatus) {
        this.deliverStatus = deliverStatus;
    }



}
class MessageObjectComparator implements Comparator<MessageObject> {



    @Override
    public int compare(MessageObject lhs, MessageObject rhs) {
        if(lhs.getAgreedPriority() < rhs.getAgreedPriority())
            return -1;
        else if (lhs.getAgreedPriority() > rhs.getAgreedPriority())
            return 1;
        else
           return 0;
    }
}
