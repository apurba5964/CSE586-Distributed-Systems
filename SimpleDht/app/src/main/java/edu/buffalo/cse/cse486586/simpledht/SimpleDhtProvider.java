package edu.buffalo.cse.cse486586.simpledht;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SimpleDhtProvider is the storage for the assignment which stores the
 * key and value pairs in a chord .
 *
 * @author apurbama
 * Reference : https://developer.android.com/training/data-storage/
 * https://developer.android.com/reference/java/util/concurrent/ConcurrentHashMap
 * https://docs.oracle.com/javase/tutorial/essential/concurrency/locksync.html
 *
 */
public class SimpleDhtProvider extends ContentProvider {
    private static final String TAG = SimpleDhtProvider.class.getSimpleName();

    private static final int SERVER_PORT = 10000;
    private static final int MASTER_CHORD = 11108;


    private static ConcurrentHashMap<String, String> keyValuePairs = new ConcurrentHashMap<String, String>();

    private String myNodeId;

    private int myNode;

    private String myPredecessorId;

    private String mySuccessorId;

    private int myPredecessor;

    private int mySuccessor;

    private HashMap<String,String> localMap =  new HashMap<String, String>();



    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        delete(selection, myNode);
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        String key = values.getAsString("key");
        String value= values.getAsString("value");

        if(key!=null) {
            try {
                String hash = genHash(key);
                if (isInMyPartition(hash)) {
                    keyValuePairs.put(key, value);
                } else {
                    sendMessageToClient(key, value, 0, 0, "INSERT", null, mySuccessor);
                }
            } catch (NoSuchAlgorithmException e) {
                Log.e(TAG, "Insert hash key generation failed:\n" + e.getMessage());
            }
        }
        return uri;
    }

    @Override
    public boolean onCreate() {
        TelephonyManager tel = (TelephonyManager)this.getContext().getSystemService(
                Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myNode = Integer.parseInt(portStr) * 2;

        try {
            myNodeId = genHash(portStr);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Hash generation failed for node id:\n" + e.getMessage());
            return false;
        }

        myPredecessorId = myNodeId;
        mySuccessorId = myNodeId;
        myPredecessor = myNode;
        mySuccessor = myNode;


        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "ServerSocket creation failed");
            ;
        }




        if (myNode != MASTER_CHORD) {

            sendMessageToClient(myNodeId, myNodeId, myNode, myNode, "JOIN", null,MASTER_CHORD);
        }

        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        if (selection.equals("*") || selection.equals("@")) {
            return queryAll(selection, new ConcurrentHashMap<String, String>(), myNode, false);
        } else {
            if (selection!=null) {
                return query(selection, myNode, false);
            }
        }
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }


    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }



    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {



        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             */

            Socket socket;


            /*
            Infinite while Loop that keeps the server up all time in  the avd
            * */


            try {
                while(true) {
                    socket = serverSocket.accept();
                    ObjectInputStream objectInputStream =
                            new ObjectInputStream(socket.getInputStream());
                    MessageObject message = (MessageObject) objectInputStream.readObject();

                    if (message.getMessageType().equalsIgnoreCase("JOIN")) {
                        join(message);
                        Log.v(TAG,"Joined Chord: "+ message.getMyPredecessor());

                    } else if (message.getMessageType().equalsIgnoreCase("ACCEPT_ME")) {
                        //Updating predecessor and successor after joining
                        myPredecessorId = message.getKey();
                        mySuccessorId = message.getValue();
                        myPredecessor = message.getMyPredecessor();
                        mySuccessor = message.getMySuccessor();

                    } else if (message.getMessageType().equalsIgnoreCase("UPDATE_SUCCESSOR")) {
                        //Updating prior predecessor to update its successor to the newly joined node
                        mySuccessorId = message.getKey();
                        mySuccessor = message.getMySuccessor();

                    } else if (message.getMessageType().equalsIgnoreCase("INSERT")) {

                        String key = message.getKey();
                        String value= message.getValue();

                        if (key!=null) {
                            try {
                                String hash = genHash(key);
                                if (isInMyPartition(hash)) {
                                    keyValuePairs.put(key, value);
                                } else {
                                    sendMessageToClient(key, value, 0, 0, "INSERT", null, mySuccessor);
                                }
                            } catch (NoSuchAlgorithmException e) {
                                Log.e(TAG, "Insert hash key generation failed:\n" + e.getMessage());
                            }
                        }

                    } else if (message.getMessageType().equalsIgnoreCase("QUERY")) {
                        if (myNode == message.getMySuccessor()) {

                            localMap.put(message.getKey(),message.getValue());
                            synchronized (localMap) {
                                localMap.notify();
                            }
                        } else {
                            query(message.getKey(), message.getMySuccessor(), true);
                        }

                    } else if (message.getMessageType().equalsIgnoreCase("QUERY_ALL")) {
                        if (myNode == message.getMySuccessor()) {

                            localMap.putAll(message.getMap());
                            synchronized (localMap) {
                                localMap.notify();
                            }
                        } else {
                            queryAll(message.getKey(), message.getMap(), message.getMySuccessor(), true);
                        }

                    } else if (message.getMessageType().equalsIgnoreCase("DELETE")) {
                        delete(message.getKey(), message.getMySuccessor());

                    } else {
                        Log.e(TAG, "Wrong Message Type: " + message.getMessageType());
                    }
                }



            }catch (ClassNotFoundException e) {
                Log.e(TAG,"ServerTask doInBackground method gives ClassNotFoundException");

            }
            catch (IOException e) {
                Log.e(TAG, "ServerTask doInBackground method gives IOException");
            }

            return null;
        }



    }


    private void join(MessageObject message) {
        if (isInMyPartition(message.getKey())) {
            if (myNode == myPredecessor) {
                sendMessageToClient(myPredecessorId, mySuccessorId, myPredecessor, mySuccessor, "ACCEPT_ME", null,message.myPredecessor);
                myPredecessorId = message.getKey();
                mySuccessorId = message.getKey();
                myPredecessor = message.getMyPredecessor();
                mySuccessor = message.getMySuccessor();
            } else {
                sendMessageToClient(message.getKey(), null, 0, message.getMySuccessor(), "UPDATE_SUCCESSOR", null,myPredecessor);
                sendMessageToClient(myPredecessorId, myNodeId, myPredecessor, myNode, "ACCEPT_ME", null,message.getMyPredecessor());
                myPredecessorId = message.getKey();
                myPredecessor = message.getMyPredecessor();
            }
        } else {
            sendMessageToClient(message.getKey(), message.getValue(), message.getMyPredecessor(), message.getMySuccessor(), message.getMessageType(),
                    message.getMap(),mySuccessor);
        }
    }




    private void delete(String key, int queryNode) {
        Log.v(TAG,"Delete request key: "+key);
        if (key.equals("*")) {
            keyValuePairs.clear();
            if ( mySuccessor != queryNode) {
                sendMessageToClient(key, null, 0, queryNode, "DELETE", null,mySuccessor);
            }
        }else if(key.equals("@")){
            keyValuePairs.clear();
            if ( mySuccessor != queryNode) {
                sendMessageToClient(key, null, 0, queryNode, "DELETE", null,mySuccessor);
            }
        }
        else {
            if (key!=null) {
                try {
                    String hash = genHash(key);
                    if (isInMyPartition(hash)) {
                        keyValuePairs.remove(key);
                    } else {
                        sendMessageToClient(key, null, 0, queryNode, "DELETE", null, mySuccessor);
                    }
                } catch (NoSuchAlgorithmException e) {
                    Log.e(TAG, "Hash Generation for delete kay failed :\n" + e.getMessage());
                }
            }
        }
    }

    private Cursor query(String key, int queryNode, boolean isQueryComplete) {
        MatrixCursor cursor = new MatrixCursor(new String[]{"key", "value"});

        try {
            String hash = genHash(key);
            if (isInMyPartition(hash)) {
                if (myNode == queryNode) {
                    cursor.addRow(new String[]{key, keyValuePairs.get(key)});
                } else {
                    sendMessageToClient(key, keyValuePairs.get(key), 0, queryNode, "QUERY", null,queryNode);
                }
                return cursor;
            } else {
                sendMessageToClient( key, null, 0, queryNode, "QUERY", null,mySuccessor);
                if (!isQueryComplete) {
                    synchronized(localMap) {
                        try {
                            localMap.wait();
                        } catch (InterruptedException e) {
                            Log.e(TAG, "Query interrupted");
                        }
                    }
                    cursor.addRow(new String[]{key, localMap.get(key)});
                    localMap.clear();
                }
            }
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Hash Generation failed  for  query key:\n" + e.getMessage());
        }

        return cursor;
    }


    private Cursor queryAll(String key, ConcurrentHashMap<String, String> map, int queryNode,
                            boolean isQueryComplete) {
        MatrixCursor cursor = new MatrixCursor(new String[]{"key", "value"});

        if (key.equals("@")) {
            for(Entry<String, String> entry : keyValuePairs.entrySet()) {
                cursor.addRow(new String[]{entry.getKey(), entry.getValue()});
            }
            return cursor;
        }
        if (key.equals("*")){
            map.putAll(keyValuePairs);
            sendMessageToClient( key, null, 0, queryNode, "QUERY_ALL", map,mySuccessor);
            if (!isQueryComplete) {

                synchronized(localMap) {
                    try {
                        localMap.wait();
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Query all interrupted");
                    }
                }
                for(Entry<String, String> entry : localMap.entrySet()) {
                    cursor.addRow(new String[]{entry.getKey(), entry.getValue()});
                }

                localMap.clear();
            }
        }

        return cursor;
    }





    private boolean isInMyPartition(String key) {
        String predHash = myPredecessorId;
        String myNodeHash = myNodeId;
        String keyHash = key;

        if(myNodeHash.compareTo(predHash) == 0)
            return true;
        if(predHash.compareTo(myNodeHash)>0){
            if (keyHash.compareTo(predHash) > 0 && keyHash.compareTo(myNodeHash) > 0)
                return true;
            if (keyHash.compareTo(predHash) < 0 && keyHash.compareTo(myNodeHash)<=0)
                return true;

        }else{
            if(keyHash.compareTo(predHash)>0 && keyHash.compareTo(myNodeHash)<=0)
                return true;
        }
        return false;


    }




    private void sendMessageToClient( String key,  String value,  int predecessor,
                                      int successor,  String messageType,  ConcurrentHashMap<String, String> keyValuePair, int destination) {


        MessageObject message = new MessageObject();
        message.setKey(key);
        message.setValue(value);
        message.setMyPredecessor(predecessor);
        message.setMySuccessor(successor);
        message.setMessageType(messageType);
        message.setMap(keyValuePair);
        message.setDestination(destination);

        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,message
                ,message);
    }


    private class ClientTask extends AsyncTask<MessageObject, Void, Void> {

        @Override
        protected Void doInBackground(MessageObject... msgs) {
            MessageObject message = msgs[0];

            try {
                Socket socket =
                        new Socket(InetAddress.getByAddress(new byte[] {10, 0, 2, 2}), message.destination);
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(
                        socket.getOutputStream());

                objectOutputStream.writeObject(message);
                objectOutputStream.close();
                socket.close();
            } catch (UnknownHostException e) {
                Log.e(TAG, "Client task UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "Client task socket IOException:\n" + e.getMessage());
            }

            return null;
        }
    }
}