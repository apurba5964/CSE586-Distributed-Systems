package edu.buffalo.cse.cse486586.simpledynamo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SimpleDynamoProvider extends ContentProvider {

	private static final String TAG = SimpleDynamoProvider.class.getSimpleName();

	private static final int SERVER_PORT = 10000;



	private static ConcurrentHashMap<String, String> keyValuePairs = new ConcurrentHashMap<String, String>();

	private String myNodeId;

	private int myNode;

	private String mySuccessor2Id;

	private String mySuccessor1Id;

	private int mySuccessor1;

	private int mySuccessor2;
	private static final String[] avd_list= { "5562", "5556", "5554", "5558", "5560"};

	private HashMap<String,String> localMap =  new HashMap<String, String>();
	private TreeMap<String,String> nodeList = new TreeMap<String, String>();
	private ArrayList<Integer> portList = new ArrayList<Integer>();
	private ArrayList<String> avds = new ArrayList<String>();
	private boolean isSyncDone = false;

	@Override
	public  int delete(Uri uri, String selection, String[] selectionArgs) {
		delete(selection, myNode);
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public synchronized  Uri insert(Uri uri, ContentValues values) {


		if (!isSyncDone){
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		String key = values.getAsString("key");
		String value= values.getAsString("value");


		if(key!=null) {
			try {
				String hash = genHash(key);


				int index = getNodesToInsert(hash);


				for(int i = 0 ;i<3;i++){
					int nodeIndex = ((index+i) % 5);
					int node = portList.get(nodeIndex);
					Log.v("Insert:",node/2+":" +key);
					sendMessageToClient(key, value, 0, 0, "INSERT", null, node);


				}


			} catch (NoSuchAlgorithmException e) {
				Log.e(TAG, "Insert hash key generation failed:\n" + e.getMessage());
			}
		}
		return uri;
	}



	@Override
	public boolean onCreate() {
		// TODO Auto-generated method stub
		TelephonyManager tel = (TelephonyManager)this.getContext().getSystemService(
				Context.TELEPHONY_SERVICE);
		String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
		myNode = Integer.parseInt(portStr) * 2;

		try {

			myNodeId = genHash(portStr);


			for (int i=0;i<avd_list.length;i++){
				nodeList.put(genHash(avd_list[i]),avd_list[i]);
				portList.add(Integer.parseInt(avd_list[i]) * 2);
				avds.add(avd_list[i]);
			}

		} catch (NoSuchAlgorithmException e) {
			Log.e(TAG, "Hash generation failed for node id:\n" + e.getMessage());
			return false;
		}
		Iterator<String> itr = nodeList.navigableKeySet().iterator();
		while (itr.hasNext()){
			String check = itr.next();
			Log.v("TreeMapcheck",check+":"+nodeList.get(check));
		}





		if (portStr.equalsIgnoreCase(avd_list[0])) {
			mySuccessor1 = 11112;
			mySuccessor2 = 11116;

		}
		if (portStr.equalsIgnoreCase(avd_list[1])) {
			mySuccessor1 = 11116;
			mySuccessor2 = 11120;
		}
		if (portStr.equalsIgnoreCase(avd_list[2])) {
			mySuccessor1 = 11120;
			mySuccessor2 = 11124;
		}
		if (portStr.equalsIgnoreCase(avd_list[3])) {
			mySuccessor1 = 11124;
			mySuccessor2 = 11108;
		}
		if (portStr.equalsIgnoreCase(avd_list[4])) {
			mySuccessor1 = 11108;
			mySuccessor2 = 11112;
		}
		try {
			ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
			new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(TAG, "ServerSocket creation failed");

		}


		synchronized (this) {
			//keyValuePairs.clear();
			Log.e("Recovery check:", "" + keyValuePairs.size());
			copyDatafromNeighbours();
			isSyncDone=true;
		}






		return true;
	}

	private synchronized void copyDatafromNeighbours() {

		keyValuePairs.clear();
		String node1 = nodeList.higherKey(myNodeId);
		if (node1==null)
			node1=nodeList.firstKey();
		String node2 = nodeList.lowerKey(myNodeId);
		if (node2==null)
			node2=nodeList.lastKey();
		String node3 = nodeList.lowerKey(node2);
		if (node3==null)
			node3=nodeList.lastKey();


		int succNode = portList.get(avds.indexOf(nodeList.get(node1)));
		int prevNode = portList.get(avds.indexOf(nodeList.get(node2)));

		Log.e("Check pre and succ: ",""+prevNode+":"+myNode+":"+succNode);
		MessageObject reMessageObject1 = sendMessageToClient( "RECOVER", null, 0, myNode, "QUERY_ALL", null,
				succNode);
		MessageObject reMessageObject2 = sendMessageToClient( "RECOVER", null, 0, myNode, "QUERY_ALL", null,
				prevNode);

		if (reMessageObject1.getKey()!="FAILED") {
			for (Map.Entry<String, String> entry : reMessageObject1.getMap().entrySet()) {


				if (checkIfItBelongsToMe(entry.getKey())) {
					Log.v("Recovery Key: ",""+entry.getKey());
					keyValuePairs.put(entry.getKey(), entry.getValue());

				}
			}
		}
		if (reMessageObject2.getKey()!="FAILED") {
			for (Map.Entry<String, String> entry : reMessageObject2.getMap().entrySet()) {


				if (checkIfItBelongsToMe(entry.getKey(),node2,node3)) {
					Log.v("Recovery Key: ",""+entry.getKey());
					keyValuePairs.put(entry.getKey(), entry.getValue());

				}
			}
		}
	}


	private  synchronized boolean checkIfItBelongsToMe(String key) {

		try {
			String hash = genHash(key);
			String myNodeCheck = nodeList.higherKey(hash);
			if (myNodeCheck==null)
				myNodeCheck=nodeList.firstKey();
			if (myNodeCheck.equalsIgnoreCase(myNodeId))
				return true;
			else
				return false;


		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return false;


	}

	private synchronized boolean checkIfItBelongsToMe(String key,String prevNode, String prevNode1) {
		try {
			String hash = genHash(key);
			String myNodeCheck = nodeList.higherKey(hash);
			if (myNodeCheck==null)
				myNodeCheck=nodeList.firstKey();
			if (myNodeCheck.equalsIgnoreCase(prevNode) ||myNodeCheck.equalsIgnoreCase(prevNode1))
				return true;
			else
				return false;


		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return false;




	}

	@Override
	public synchronized Cursor  query(Uri uri, String[] projection, String selection,
						String[] selectionArgs, String sortOrder) {
		if (!isSyncDone){
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
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
	public int update(Uri uri, ContentValues values, String selection,
					  String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	private synchronized String genHash(String input) throws NoSuchAlgorithmException {
		MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
		byte[] sha1Hash = sha1.digest(input.getBytes());
		Formatter formatter = new Formatter();
		for (byte b : sha1Hash) {
			formatter.format("%02x", b);
		}
		return formatter.toString();
	}


	private synchronized int getNodesToInsert(String key){



		String node = nodeList.higherKey(key);
		if (node==null)
			node=nodeList.firstKey();
		Log.v("Node",nodeList.get(node));
		Log.v("NodeIndex",""+avds.indexOf(nodeList.get(node)));
		return avds.indexOf(nodeList.get(node));


	}



	private class ServerTask extends AsyncTask<ServerSocket, MessageObject, Void> {



		@Override
		protected Void doInBackground(ServerSocket... sockets) {
			ServerSocket serverSocket = sockets[0];

			/*
			 * TODO: Fill in your server code that receives messages and passes them
			 * to onProgressUpdate().
			 */




            /*
            Infinite while Loop that keeps the server up all time in  the avd
            * */


			try {
				while(true) {
					Socket socket = serverSocket.accept();
					ObjectInputStream objectInputStream =
							new ObjectInputStream(socket.getInputStream());
					ObjectOutputStream objectOutputStream = new ObjectOutputStream(
							socket.getOutputStream());
					MessageObject message = (MessageObject) objectInputStream.readObject();

					if (message.getMessageType().equalsIgnoreCase("INSERT")) {

						String key = message.getKey();
						String value= message.getValue();

						if (key!=null) {
							keyValuePairs.put(key, value);

						}
						objectOutputStream.writeObject(message);
						objectOutputStream.flush();
						objectOutputStream.close();

					} else if (message.getMessageType().equalsIgnoreCase("QUERY")) {

						message.setValue(keyValuePairs.get(message.getKey()));
						objectOutputStream.writeObject(message);
						objectOutputStream.flush();
						objectOutputStream.close();




					} else if (message.getMessageType().equalsIgnoreCase("QUERY_ALL")) {
						message.setMap(keyValuePairs);
						objectOutputStream.writeObject(message);
						objectOutputStream.flush();
						objectOutputStream.close();


					} else if (message.getMessageType().equalsIgnoreCase("DELETE_ALL")) {
						keyValuePairs.clear();
						objectOutputStream.writeObject(message);
						objectOutputStream.flush();
						objectOutputStream.close();


					} else if (message.getMessageType().equalsIgnoreCase("DELETE")) {
						keyValuePairs.remove(message.getKey());
						objectOutputStream.writeObject(message);
						objectOutputStream.flush();
						objectOutputStream.close();


					}
					else {
						Log.e(TAG, "Wrong Message Type: " + message.getMessageType());
					}
					//socket.close();
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
	private synchronized Cursor query(String key, int queryNode, boolean isQueryComplete) {
		MatrixCursor cursor = new MatrixCursor(new String[]{"key", "value"});


		try {
			String hash = genHash(key);
			String node = nodeList.higherKey(hash);
			if (node==null)
				node=nodeList.firstKey();

			String node1 = nodeList.higherKey(node);
			if (node1==null)
				node1=nodeList.firstKey();

			Log.v("Querkey",""+key+nodeList.get(node)+queryNode);
			MessageObject msgRecieved = sendMessageToClient( key, null, 0, queryNode,
					"QUERY", null,Integer.valueOf(nodeList.get(node))*2);
			MessageObject msgRecieved1 = sendMessageToClient( key, null, 0, queryNode,
					"QUERY", null,Integer.valueOf(nodeList.get(node1))*2);

			if(msgRecieved.getKey()!="FAILED")
				cursor.addRow(new String[]{msgRecieved.getKey(), msgRecieved.getValue()});
			else if(msgRecieved.getKey()=="FAILED"){
				cursor.addRow(new String[]{msgRecieved1.getKey(), msgRecieved1.getValue()});
			}



		} catch (NoSuchAlgorithmException e) {
			Log.e(TAG, "Hash Generation failed  for  query key:\n" + e.getMessage());
		}

		return cursor;
	}


	private synchronized Cursor queryAll(String key, ConcurrentHashMap<String, String> map, int queryNode,
							boolean isQueryComplete) {
		MatrixCursor cursor = new MatrixCursor(new String[]{"key", "value"});

		if (key.equals("@")) {
			//Log.e("Map size",":"+keyValuePairs.size());
			for(Map.Entry<String, String> entry : keyValuePairs.entrySet()) {
				cursor.addRow(new String[]{entry.getKey(), entry.getValue()});
			}
			return cursor;
		}
		if (key.equals("*")){
			map.putAll(keyValuePairs);
			for (int i =0;i<portList.size();i++){
				if (portList.get(i)!=queryNode){
					MessageObject reMessageObject = sendMessageToClient( key, null, 0, queryNode, "QUERY_ALL", null,portList.get(i));
					if(reMessageObject.getKey()!="FAILED")
						map.putAll(reMessageObject.getMap());
				}
			}

			for(Map.Entry<String, String> entry : map.entrySet()) {
				cursor.addRow(new String[]{entry.getKey(), entry.getValue()});
			}



		}

		return cursor;
	}

	private  synchronized void delete(String key, int queryNode) {
		//Log.v(TAG,"Delete request key: "+key);
		if (key.equals("*")) {
			keyValuePairs.clear();
			for (int i =0;i<portList.size();i++){

				sendMessageToClient( key, null, 0, queryNode, "DELETE_ALL", null,portList.get(i));


			}
		}else if(key.equals("@")){
			keyValuePairs.clear();

		}
		else {
			if (key!=null) {
				try {

					String hash = genHash(key);


					int index = getNodesToInsert(hash);


					for(int i = 0 ;i<3;i++){
						int nodeIndex = ((index+i) % 5);
						int node = portList.get(nodeIndex);
						Log.v("Delete:",node/2+":" +key);
						sendMessageToClient(key, null, 0, 0, "DELETE", null, node);


					}





				} catch (NoSuchAlgorithmException e) {
					Log.e(TAG, "Hash Generation for delete kay failed :\n" + e.getMessage());
				}
			}
		}
	}


	private synchronized MessageObject sendMessageToClient( String key,  String value,  int predecessor,
											   int successor,  String messageType,  ConcurrentHashMap<String, String> keyValuePair, int destination) {


		MessageObject message = new MessageObject();
		message.setKey(key);
		message.setValue(value);
		message.setMyPredecessor(predecessor);
		message.setMySuccessor(successor);
		message.setMessageType(messageType);
		message.setMap(keyValuePair);
		message.setDestination(destination);

		MessageObject recieved=null;
		try {
			recieved = new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,message
					,message).get();
		}catch (java.lang.InterruptedException e){

		}catch (java.util.concurrent.ExecutionException e){

		}


		return recieved;
	}


	private class ClientTask extends AsyncTask<MessageObject, Void, MessageObject> {

		@Override
		protected MessageObject doInBackground(MessageObject... msgs) {
			MessageObject message = msgs[0];
			MessageObject recieved = null;

			try {
				Socket socket =
						new Socket(InetAddress.getByAddress(new byte[] {10, 0, 2, 2}), message.getDestination());
				ObjectOutputStream objectOutputStream = new ObjectOutputStream(
						socket.getOutputStream());
				socket.setSoTimeout(200);
				Log.v("Socket",""+socket.getPort());


				objectOutputStream.writeObject(message);
				objectOutputStream.flush();



				ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
				recieved = (MessageObject) objectInputStream.readObject();

				objectInputStream.close();

				socket.close();
			} catch (SocketTimeoutException e){
				Log.e(TAG, "SocketTimeoutException: " + e.toString());
				MessageObject failed = new MessageObject();
				failed.setKey("FAILED");
				recieved = failed;
			}
			catch (ClassNotFoundException e) {
				Log.e(TAG,"ClientTask doInBackground method gives ClassNotFoundException");
				MessageObject failed = new MessageObject();
				failed.setKey("FAILED");
				recieved = failed;

			}
			catch (UnknownHostException e) {
				Log.e(TAG, "ClientTask UnknownHostException");
				MessageObject failed = new MessageObject();
				failed.setKey("FAILED");
				recieved = failed;
			}
			catch (IOException e) {

				Log.e(TAG, "ClientTask doInBackground method gives IOException" + e.toString());
				MessageObject failed = new MessageObject();
				failed.setKey("FAILED");
				recieved= failed;
			}

			return recieved;
		}
	}
}
