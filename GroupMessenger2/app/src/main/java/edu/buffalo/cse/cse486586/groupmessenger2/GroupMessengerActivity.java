package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    private static  String TAG = GroupMessengerActivity.class.getSimpleName();
    private static final int SERVER_PORT = 10000;
    private static final String REMOTE_PORT0 = "11108";
    private static final String REMOTE_PORT1 = "11112";
    private static final String REMOTE_PORT2 = "11116";
    private static final String REMOTE_PORT3 = "11120";
    private static final String REMOTE_PORT4 = "11124";
    private static final String[] ports_list = { REMOTE_PORT0, REMOTE_PORT1, REMOTE_PORT2,
            REMOTE_PORT3, REMOTE_PORT4};
    static final String[] avd_list= { "5554", "5556", "5558", "5560", "5562"};
    static int sequence = 0, timeout = 1500;
    static String avd_server_id;
    //initial capacity of priority queue is 10
    //https://docs.oracle.com/javase/7/docs/api/java/util/PriorityQueue.html
    static PriorityQueue<MessageObject> message_queue = new PriorityQueue<MessageObject>( 10,new MessageObjectComparator());
    HashMap<String,Integer> messageSequenceAvdMap = new HashMap<String, Integer>();



    protected void initializeSequencerAvdMap(){
        for(String avd : avd_list){
            messageSequenceAvdMap.put(avd,sequence);
        }
    }






    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        //The sequencer of each avd is initialized to 0 and the hashmap is to store current local
        // sequence active on that avd
        initializeSequencerAvdMap();




        /*
         * Calculate the port number that this AVD listens on.
         *
         */
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        avd_server_id = portStr;

        /*
         * Created a server socket as well as a thread (AsyncTask) that listens on the server
         * port.
         * http://developer.android.com/reference/android/os/AsyncTask.html
         */


        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "ServerSocket creation failed");
            return;
        }



        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         *
         * Reference : https://developer.android.com/reference/android/widget/Button
         */



        // Submit by clicking SEND Button
        final EditText editText = (EditText) findViewById(R.id.editText1);

        Button b = (Button) findViewById(R.id.button4);
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String msg = editText.getText().toString() + "\n";
                editText.setText("");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }



    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        private Uri buildUri(String scheme, String authority) {
            Uri.Builder uriBuilder = new Uri.Builder();
            uriBuilder.authority(authority);
            uriBuilder.scheme(scheme);
            return uriBuilder.build();
        }

        Uri provider = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             */


            MessageObject message,temp1;
            int proposed;
            String avd_client_id = "";
            Iterator<MessageObject> messageIterator;

            /*
            Infinite while Loop that keeps the server up all time in all the avds
            * */

            while(true){
                Socket socket = null;
                try {
                    /*
                    serverSocket.accept() method listens for a connection to be made to this socket
                    and accepts it
                     */

                    socket = serverSocket.accept();

                    /*An ObjectInputStream deserializes primitive data and objects previously
                     written using an OutputStream.An ObjectOutputStream writes primitive data types and java objects
                     to an OutputStream . We need ObjectOutputStream to send messages to all avds when the delivery status
                     is set to true along  with the agreed sequence .If set to false we need to store the
                     message in the buffer queue and send the proposed sequence*/
                    ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                    ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());




                    message = (MessageObject)inputStream.readObject();
                    avd_client_id = message.getAvdID();


                    //setting the latest proposed priority for the recieved message from clients
                    proposed = messageSequenceAvdMap.get(avd_client_id);

                    Log.v( avd_client_id+" Send ",message.getMessage()+" "+String.valueOf(message.getFifoOrder()));
                    Log.v( avd_server_id+" Proposed ",message.getMessage()+" "+String.valueOf(proposed));


                    //Now we need to check if the proposed priority is greater than the priority of
                    //the recieved message.If so set the proposed priority otherwise keep the fifo sequence of the
                    //recieved message
                    if(proposed > message.getFifoOrder()){
                        message.setAvdID(avd_server_id);
                        message.setProposedPriority(proposed);

                    }else{
                        message.setProposedPriority(message.getFifoOrder());
                    }
                    message.setDeliverStatus(false);
                    message_queue.add(message);//added to the queue
                    outputStream.writeObject(message);
                    outputStream.flush();

                    // updating the  proposal in the hashmap
                    proposed ++;
                    messageSequenceAvdMap.put(avd_client_id, proposed);


                    //Now we need to iterate over the queue to check if we recieved a message which is
                    //already in the queue. We then set its  delivery status to true
                    //as it would be the message with the agreed priority from the sender

                    message = (MessageObject)inputStream.readObject();
                    messageIterator = message_queue.iterator();
                    while(messageIterator.hasNext()){
                        temp1 = messageIterator.next();
                        if(temp1.getFifoOrder() == message.getFifoOrder()
                                && temp1.getAvdID().equalsIgnoreCase(message.getAvdID())){

                            message_queue.remove(temp1);
                            message.setDeliverStatus(true);
                            message_queue.add(message);
                            //updating map with agreed priority
                            messageSequenceAvdMap.put(message.getAvdID(), message.getAgreedPriority());
                            break;

                        }
                    }


                    while (message_queue.peek()!=null && message_queue.peek().getDeliverStatus()){

                        message = message_queue.poll();
                        int key = sequence;
                        sequence++;

                        publishProgress(Integer.toString(key),message.getMessage());

                    }



                }
                catch (SocketTimeoutException e){
                    Log.e(TAG, "SocketTimeoutException: " + e.toString());
                }
                catch (ClassNotFoundException e) {
                    Log.e(TAG,"ServerTask doInBackground method gives ClassNotFoundException");

                }
                catch (IOException e) {
                    Log.e(TAG, "ServerTask doInBackground method gives IOException");
                }
                finally {

                    //Clear the queue of messages of failed client avd
                    MessageObject failed;
                    Iterator<MessageObject> failedIterator = message_queue.iterator();
                    while (failedIterator.hasNext()){
                        failed = failedIterator.next();
                        if(failed.getAvdID().equalsIgnoreCase(avd_client_id) && !failed.getDeliverStatus()){
                            Log.v(TAG+" Failed avd :",failed.getAvdID());
                            message_queue.remove(failed);
                        }
                    }
                }


            }


            //return null;
        }

        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().Key Value pair is
             * created and Content resolver is called to insert the values in the Content provider
             * which is also displayed using text view.
             */
            String key = strings[0];
            String message = strings[1];


            ContentResolver contentResolver = getContentResolver();
            ContentValues keyValuePair = new ContentValues();
            keyValuePair.put("key",key);
            keyValuePair.put("value",message);
            Log.v(TAG,key+ " - " + message);

            contentResolver.insert(provider,keyValuePair);



            TextView textView = (TextView) findViewById(R.id.textView1);
            textView.append(message + "\t\n");
            //TextView localView = (TextView) findViewById(R.id.textView1);
            //localView.append("\n");



            return;
        }
    }



    private class ClientTask extends AsyncTask<String, Void, Void> {
        int fifoSequence = 0;

        @Override
        protected Void doInBackground(String... msgs) {
            //5 avds
            Socket socketList[] = new Socket[5];
            ObjectOutputStream out_stream;
            ObjectInputStream in_stream;
            ObjectOutputStream outputStreamList[] = new ObjectOutputStream[5];
            ObjectInputStream inputStreamList[] = new ObjectInputStream[5];
            List<MessageObject> recievedMessageList = new ArrayList<MessageObject>();

            MessageObject message;
            String msgToSend;
            Socket socket;



            for (int i = 0;i<5;i++) {
                try {
                    socketList[i] = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(ports_list[i]));
                    socket = socketList[i];
                    outputStreamList[i] = new ObjectOutputStream(socket.getOutputStream());
                    inputStreamList[i] = new ObjectInputStream(socket.getInputStream());
                    out_stream = outputStreamList[i];
                    in_stream = inputStreamList[i];
                    socket.setSoTimeout(timeout);
                    msgToSend = msgs[0];
                    message = new MessageObject(msgToSend,fifoSequence,-1,-1,
                            avd_server_id,false);
                    out_stream.writeObject(message);
                    out_stream.flush();
                    message = (MessageObject) in_stream.readObject();
                    recievedMessageList.add(message);

                }catch (SocketTimeoutException e){
                    Log.e(TAG, "SocketTimeoutException: " + e.toString());
                }
                catch (ClassNotFoundException e) {
                    Log.e(TAG,"ClientTask doInBackground method gives ClassNotFoundException");

                }
                catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException");
                }
                catch (IOException e) {
                    Log.e(TAG, "ClientTask doInBackground method gives IOException"+socketList[i]);
                }
            }

            
            decideAndSendMsgWithMaxPriority(recievedMessageList,socketList,
                    outputStreamList);

            fifoSequence++;//increament for each message recieved






            return null;
        }

        private void decideAndSendMsgWithMaxPriority( List<MessageObject> recievedMessageList,
                                                     Socket[] socketList, ObjectOutputStream[] outputStreamList) {


            Socket socket;
            ObjectOutputStream outputStream;
            MessageObject msgToSendWithMaxPriorityRecieved = recievedMessageList.get(0);
            int finalPriority = fifoSequence;
            for(int i=0;i< recievedMessageList.size();i++){
                if(recievedMessageList.get(i).getProposedPriority()>finalPriority){

                    finalPriority = recievedMessageList.get(i).getProposedPriority();
                    msgToSendWithMaxPriorityRecieved = recievedMessageList.get(i);
                }


            }


            msgToSendWithMaxPriorityRecieved.setAgreedPriority(finalPriority);
            fifoSequence = finalPriority;


            for (int i=0;i<5;i++){
                try {
                    socket = socketList[i];
                    outputStream = outputStreamList[i];
                    Log.v("Outstream",outputStream.toString()+ i);
                    outputStream.writeObject(msgToSendWithMaxPriorityRecieved);
                    outputStream.flush();
                    socket.close();


                }catch (SocketTimeoutException e){
                    Log.e(TAG, "SocketTimeoutException: " + e.toString());
                }
                catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException");
                }
                catch (IOException e) {
                    Log.e(TAG, "ClientTask doInBackground method gives IOException"+socketList[i]);
                }
                catch (Exception e){
                    Log.e(TAG,"Exception: " + e.toString());
                }


            }

        }
    }



}
