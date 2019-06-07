package edu.buffalo.cse.cse486586.simpledht;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;


public class MessageObject implements Serializable {

    public String key;
    public String value;
    public int myPredecessor;
    public int mySuccessor;
    public String messageType;
    public ConcurrentHashMap<String, String> map = null;
    public int destination;

    public MessageObject() {


    }

    public MessageObject(String key, String value, int myPredecessor, int mySuccessor, String messageType, ConcurrentHashMap<String, String> map, int destination) {
        this.key = key;
        this.value = value;
        this.myPredecessor = myPredecessor;
        this.mySuccessor = mySuccessor;
        this.messageType = messageType;
        this.map = map;
        this.destination = destination;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getMyPredecessor() {
        return myPredecessor;
    }

    public void setMyPredecessor(int myPredecessor) {
        this.myPredecessor = myPredecessor;
    }

    public int getMySuccessor() {
        return mySuccessor;
    }

    public void setMySuccessor(int mySuccessor) {
        this.mySuccessor = mySuccessor;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public ConcurrentHashMap<String, String> getMap() {
        return map;
    }

    public void setMap(ConcurrentHashMap<String, String> map) {
        this.map = map;
    }

    public int getDestination() {
        return destination;
    }

    public void setDestination(int destination) {
        this.destination = destination;
    }



}
