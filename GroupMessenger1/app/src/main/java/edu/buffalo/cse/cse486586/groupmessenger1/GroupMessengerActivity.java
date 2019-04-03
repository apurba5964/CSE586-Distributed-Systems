package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    private static  String TAG = GroupMessengerActivity.class.getSimpleName();
    private static final int SERVER_PORT = 10000;
    int sequence = 0;
    private static final String REMOTE_PORT0 = "11108";
    private static final String REMOTE_PORT1 = "11112";
    private static final String REMOTE_PORT2 = "11116";
    private static final String REMOTE_PORT3 = "11120";
    private static final String REMOTE_PORT4 = "11124";





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * Calculate the port number that this AVD listens on.
         *
         */
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

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



        final Button send = (Button) findViewById(R.id.button4);
        final EditText input = (EditText) findViewById(R.id.editText1);

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = input.getText().toString();
                //Sending Messages to all the 5 avd's using  5 different Async task serial executor
                //Reference : As done in PA1 for 2 avd's.
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,message,REMOTE_PORT0);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,message,REMOTE_PORT1);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,message,REMOTE_PORT2);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,message,REMOTE_PORT3);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,message,REMOTE_PORT4);

                TextView localTextView = (TextView) findViewById(R.id.textView1);
                localTextView.append("\t" + message);
                TextView remoteTextView = (TextView) findViewById(R.id.textView1);
                remoteTextView.append("\n");
                input.setText("");



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

        Uri provider = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger1.provider");

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             */

            Socket socket;
            String message;

            /*
            Infinite while Loop that keeps the server up all time in both the avd
            * */

            while(true){
                try {
                    /*
                    serverSocket.accept() method listens for a connection to be made to this socket
                    and accepts it
                     */

                    socket = serverSocket.accept();

                    /*An ObjectInputStream deserializes primitive data and objects previously
                     written using an OutputStream.*/
                    ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());


                    message = inputStream.readObject().toString();

                    //calls onProgress method
                    publishProgress(message);

                    //closes the socket

                    socket.close();





                }catch (ClassNotFoundException e) {
                    Log.e(TAG,"ServerTask doInBackground method gives ClassNotFoundException");

                }
                catch (IOException e) {
                    Log.e(TAG, "ServerTask doInBackground method gives IOException");
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
            String strReceived = strings[0].trim();



            ContentValues keyValuePair = new ContentValues();
            keyValuePair.put("key",String.valueOf(sequence));
            keyValuePair.put("value",strReceived);
            Log.v(TAG,String.valueOf(sequence)+ " - " + strReceived);
            sequence++;
            Uri groupMessengerUri = getContentResolver().insert(provider,keyValuePair);


            TextView textView = (TextView) findViewById(R.id.textView1);
            textView.append(strReceived + "\t\n");
            TextView localView = (TextView) findViewById(R.id.textView1);
            localView.append("\n");



            return;
        }
    }



    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            try {
                String remotePort = msgs[1];

                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(remotePort));

                String msgToSend = msgs[0];
                /*
                 * TODO: Fill in your client code that sends out a message.
                 */

                /*An ObjectOutputStream writes primitive data types and java objects
                to an OutputStream*/
                ObjectOutputStream outputMsgStream = new ObjectOutputStream(socket.getOutputStream());

                outputMsgStream.writeObject(msgToSend);

                /* This method flushes the stream. This will write any buffered output bytes to the
                 * stream */
                outputMsgStream.flush();
                socket.close();
            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }

            return null;
        }
    }
}
