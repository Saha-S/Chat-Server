package com.sina.k1.chatserver;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class ActivityServer extends AppCompatActivity {

    static final int SocketServerPORT = 8080;

    TextView infoIp, infoPort ;
    Spinner spUsers;
    ArrayAdapter<ChatClient> spUsersAdapter;
    ImageButton btnSentTo;

    String msgLog = "";

    List<ChatClient> userList;

    ServerSocket serverSocket;
    private EditText editTextSay;
    private LinearLayout container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        infoIp = (TextView) findViewById(R.id.infoip);
        infoPort = (TextView) findViewById(R.id.infoport);
       // msgServer = (TextView) findViewById(R.id.server_msg);
      //  msgClient = (TextView) findViewById(R.id.client_msg);
        container = (LinearLayout) findViewById(R.id.container);

        spUsers = (Spinner) findViewById(R.id.spusers);
        userList = new ArrayList<ChatClient>();
        spUsersAdapter = new ArrayAdapter<ChatClient>(
                ActivityServer.this, android.R.layout.simple_spinner_item, userList);
        spUsersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spUsers.setAdapter(spUsersAdapter);

        btnSentTo = (ImageButton)findViewById(R.id.sentto);
        btnSentTo.setOnClickListener(btnSentToOnClickListener);

        editTextSay = (EditText)findViewById(R.id.say);


        infoIp.setText(getIpAddress());

        ChatServerThread chatServerThread = new ChatServerThread();
        chatServerThread.start();
    }

    View.OnClickListener btnSentToOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ChatClient client = (ChatClient)spUsers.getSelectedItem();
            if(client != null){
                //String dummyMsg = "Dummy message from server.\n";
                final LayoutInflater inflater = (LayoutInflater)getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
                    final View child = inflater.inflate(R.layout.item_server, null, false);
                    TextView txtServer = (TextView) child.findViewById(R.id.txt_cserver);
                   // TextView txtClient = (TextView) child.findViewById(R.id.txt_client);

                    client.chatThread.sendMsg(editTextSay.getText().toString());
             //   msgLog += "-  "+ client.name +": " + editTextSay.getText().toString()+"\n";
                txtServer.setText("me: " + editTextSay.getText().toString()+"\n");

                container.addView(child);
                editTextSay.setText("");


            }else{
                Toast.makeText(ActivityServer.this, "اتصال به کاربر برقرار نیست", Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private class ChatServerThread extends Thread {

        @Override
        public void run() {
            Socket socket = null;

            try {
                serverSocket = new ServerSocket(SocketServerPORT);
                ActivityServer.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        infoPort.setText("I'm waiting here: "
                                + serverSocket.getLocalPort());
                    }
                });

                while (true) {
                    socket = serverSocket.accept();
                    ChatClient client = new ChatClient();
                    userList.add(client);
                    ConnectThread connectThread = new ConnectThread(client, socket);
                    connectThread.start();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            spUsersAdapter.notifyDataSetChanged();
                        }
                    });
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }

        }

    }

    private class ConnectThread extends Thread {

        Socket socket;
        ChatClient connectClient;
        String msgToSend = "";

        ConnectThread(ChatClient client, Socket socket){
            connectClient = client;
            this.socket= socket;
            client.socket = socket;
            client.chatThread = this;
        }

        @Override
        public void run() {
            DataInputStream dataInputStream = null;
            DataOutputStream dataOutputStream = null;

            try {
                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream = new DataOutputStream(socket.getOutputStream());

                final String n = dataInputStream.readUTF();

                connectClient.name = n;

              //  msgLog += connectClient.name + " متصل شد@" +
               //         connectClient.socket.getInetAddress() +
             //           ":" + connectClient.socket.getPort() + "\n";
                ActivityServer.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        final LayoutInflater inflater = (LayoutInflater)getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
                        final View child = inflater.inflate(R.layout.item_msg, null, false);
                        TextView msg = (TextView) child.findViewById(R.id.txt_msg);

                        msg.setText(connectClient.name + " متصل شد@" +
                                connectClient.socket.getInetAddress() +
                                ":" + connectClient.socket.getPort() + "\n");
                        container.addView(child);
                    }
                });

                dataOutputStream.writeUTF("خوش آمدید  " + n + "\n");
                dataOutputStream.flush();

                broadcastMsg(n + " وارد شد\n");

                while (true) {
                    if (dataInputStream.available() > 0) {
                        final String newMsg = dataInputStream.readUTF();


                     //   msgLog += n + ": " + newMsg;
                        ActivityServer.this.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                final LayoutInflater inflater = (LayoutInflater)getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
                                final View child = inflater.inflate(R.layout.item_client, null, false);
                                //TextView txtServer = (TextView) child.findViewById(R.id.txt_cserver);
                                 TextView txtClient = (TextView) child.findViewById(R.id.txt_client);

                                txtClient.setText(n + ": " + newMsg);
                                container.addView(child);
                            }
                        });

                        broadcastMsg("server$&*K@CHUUFWDWETKMYTK@@JHR"+n + ": " + newMsg);
                    }

                    if(!msgToSend.equals("")){
                        dataOutputStream.writeUTF(msgToSend);
                        dataOutputStream.flush();
                        msgToSend = "";
                    }

                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                userList.remove(connectClient);

                ActivityServer.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        spUsersAdapter.notifyDataSetChanged();
                        Toast.makeText(ActivityServer.this,
                                connectClient.name + " حذف شد.", Toast.LENGTH_LONG).show();

                     //   msgLog += "-- " + connectClient.name + " خارج شد\n";
                        ActivityServer.this.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                final LayoutInflater inflater = (LayoutInflater)getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
                                final View child = inflater.inflate(R.layout.item_msg, null, false);
                                TextView msg = (TextView) child.findViewById(R.id.txt_msg);

                                msg.setText("" + connectClient.name + " خارج شد\n");
                                container.addView(child);
                            }
                        });

                        broadcastMsg("" + connectClient.name + " خارج شد\n");
                    }
                });
            }

        }

        private void sendMsg(String msg){
            msgToSend = msg;
        }

    }

    private void broadcastMsg(String msg){
        for(int i=0; i<userList.size(); i++){
            userList.get(i).chatThread.sendMsg(msg);
         //   msgLog += "- send to " + userList.get(i).name + "\n";
           msgLog += "\n";
        }

        ActivityServer.this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
               // msgServer.setText(msgLog);
            }
        });
    }

    private String getIpAddress() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        ip += "SiteLocalAddress: "
                                + inetAddress.getHostAddress() + "\n";
                    }

                }

            }

        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ip += "مشکلی پیش آمده است! " + e.toString() + "\n";
        }

        return ip;
    }

    class ChatClient {
        String name;
        Socket socket;
        ConnectThread chatThread;

        @Override
        public String toString() {
            return name + ": " + socket.getInetAddress().getHostAddress();
        }
    }
}
