package com.sina.k1.chatserver;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
   // private LinearLayout container;

    ToggleButton record;
    private MediaRecorder myRecorder;
    private String outputFile = "";

    private View child;
    private ToggleButton play;

    private List<Message> mMessages = new ArrayList<Message>();

    private RecyclerView mMessagesView;
    private RecyclerView.Adapter mAdapter;
    private Handler mTypingHandler = new Handler();
    private String fileName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        infoIp = (TextView) findViewById(R.id.infoip);
        infoPort = (TextView) findViewById(R.id.infoport);

        spUsers = (Spinner) findViewById(R.id.spusers);
        userList = new ArrayList<ChatClient>();
        spUsersAdapter = new ArrayAdapter<ChatClient>(
                ActivityServer.this, android.R.layout.simple_spinner_item, userList);
        spUsersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spUsers.setAdapter(spUsersAdapter);

        btnSentTo = (ImageButton)findViewById(R.id.sentto);
        btnSentTo.setOnClickListener(btnSentToOnClickListener);

        editTextSay = (EditText)findViewById(R.id.say);
        record = (ToggleButton)findViewById(R.id.tg_record);


        final LayoutInflater inflater = (LayoutInflater) getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        child = inflater.inflate(R.layout.item_voice, null, false);

        ViewGroup view = (ViewGroup) findViewById(android.R.id.content);

        mAdapter = new MessageAdapter(App.context, mMessages);

        mMessagesView = (RecyclerView) findViewById(R.id.messages);
        mMessagesView.setLayoutManager(new LinearLayoutManager(ActivityServer.this));
        mMessagesView.setAdapter(mAdapter);


        infoIp.setText(getIpAddress());

        ChatServerThread chatServerThread = new ChatServerThread();
        chatServerThread.start();

        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(record.isChecked()== false) {
                    stop(view);
                }
                if(record.isChecked()== true) {
                    fileName = new SimpleDateFormat("yyyy-MM-ddHHmmssSSS'.mp3'").format(new Date());


                    outputFile = Environment.getExternalStorageDirectory().
                            getAbsolutePath() + "/"+fileName;

                    myRecorder = new MediaRecorder();
                    myRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    myRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                    myRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
                    myRecorder.setOutputFile(outputFile);

                    start(view);
                }
            }
        });

    }

    View.OnClickListener btnSentToOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ChatClient client = (ChatClient)spUsers.getSelectedItem();

            if(client != null){

                client.chatThread.sendMsg(editTextSay.getText().toString());
                mMessages.add(new Message.Builder(Message.TYPE_RIGHT)
                        .username("").message("من : "+editTextSay.getText().toString()).build());
                mAdapter.notifyItemInserted(mMessages.size() - 1);
                scrollToBottom();

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
////
                //  msgLog += connectClient.name + " متصل شد@" +
                //         connectClient.socket.getInetAddress() +
                //           ":" + connectClient.socket.getPort() + "\n";
                ActivityServer.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        infoIp.setVisibility(View.GONE);
                        infoPort.setVisibility(View.GONE);
                        final LayoutInflater inflater = (LayoutInflater)getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
                        final View child = inflater.inflate(R.layout.item_msg, null, false);
                        TextView msg = (TextView) child.findViewById(R.id.txt_msg);

                        msg.setText(connectClient.name + " متصل شد@" +
                                connectClient.socket.getInetAddress() +
                                ":" + connectClient.socket.getPort() + "\n");

                        mMessages.add(new Message.Builder(Message.TYPE_LOG)
                                .username(connectClient.name).message(" متصل شد ").build());
                        mAdapter.notifyItemInserted(mMessages.size() - 1);
                        scrollToBottom();

                        //container.addView(child);
                    }
                });

                dataOutputStream.writeUTF("خوش آمدید  " + n + "\n");
                dataOutputStream.flush();

                broadcastMsg(n + " وارد شد\n");

                while (true) {
                    if (dataInputStream.available() > 0) {
                        String charT = String.valueOf(dataInputStream.readChar());

                        if (charT.equals("I")) {
                            fileName = new SimpleDateFormat("yyyy-MM-ddHHmmssSSS'.mp3'").format(new Date());
                            final String outputFile = Environment.getExternalStorageDirectory().
                                    getAbsolutePath() + "/"+fileName;
                            File path = new File(outputFile);
                            long length = dataInputStream.readLong();
                            DataOutputStream dataOutputStream2 = new DataOutputStream(new FileOutputStream(path));

                            byte[] buffer = new byte[16384];
                            boolean end= false;
                            while (!end){
                                dataOutputStream2.write(buffer, 0, dataInputStream.read(buffer, 0, buffer.length));

                                if(buffer.length >= length ){
                                    end = true;
                                    dataOutputStream2.flush();
                                    ActivityServer.this.runOnUiThread(new Runnable() {

                                        @Override
                                        public void run() {
                                            final LayoutInflater inflater = (LayoutInflater) getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
                                            child = inflater.inflate(R.layout.item_voice, null, false);
                                            mMessages.add(new Message.Builder(Message.TYPE_VOICE)
                                                    .username("سرور").message(fileName).build());
                                            mAdapter.notifyItemInserted(mMessages.size() - 1);
                                            scrollToBottom();

                                        }
                                    });

                                }

                            }

                        }
                        if (charT.equals("T")) {
                            final String newMsg = dataInputStream.readUTF();


                            //   msgLog += n + ": " + newMsg;
                            ActivityServer.this.runOnUiThread(new Runnable() {

                                @Override
                                public void run() {

                                    mMessages.add(new Message.Builder(Message.TYPE_LEFT)
                                            .username(n).message(n+": "+ newMsg).build());
                                    mAdapter.notifyItemInserted(mMessages.size() - 1);
                                    scrollToBottom();

                                    // container.addView(child);
                                }
                            });
                            broadcastMsg("server$&*K@CHUUFWDWETKMYTK@@JHR" +   newMsg);

                        }


                    }
                    if(msgToSend.toString().equals("voicemessage")){
                        dataOutputStream.writeChar('I'); // as image,
                        byte[] buf = new byte[16384];
                        FileInputStream requestedfile = new FileInputStream(outputFile);
                        dataOutputStream.writeLong(new File(outputFile).length());

                        while((requestedfile.read(buf)!=-1)){
                            dataOutputStream.write(buf);

                            dataOutputStream.flush();
                        }

                        msgToSend = "";
                        fileName = "";


                    }else{
                        if (!msgToSend.equals("")) {
                            dataOutputStream.writeChar('T'); // as image,
                            dataOutputStream.writeUTF(msgToSend);
                            dataOutputStream.flush();
                            msgToSend = "";
                            fileName = "";

                        }
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
                               // container.addView(child);

                                mMessages.add(new Message.Builder(Message.TYPE_LOG)
                                        .username(connectClient.name).message("خارج شد").build());
                                mAdapter.notifyItemInserted(mMessages.size() - 1);
                                scrollToBottom();

                            }
                        });

                        broadcastMsg("" + connectClient.name + " خارج شد\n");
                    }
                });
            }

        }

        public void sendMsg(String msg){
            Log.i("vvvvvvvv" , "3");
            msgToSend = msg;
        }

    }

    private void broadcastMsg(String msg){
        for(int i=0; i<userList.size(); i++){
            Log.i("vvvvvvvv" , "2");
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
    ////////////////////////

    public void start(View view) {
        try {
            myRecorder.prepare();
            myRecorder.start();
        } catch (IllegalStateException e) {
            // start:it is called before prepare()
            // prepare: it is called after start() or before setOutputFormat()
            e.printStackTrace();
        } catch (IOException e) {
            // prepare() fails
            e.printStackTrace();
        }

        Toast.makeText(getApplicationContext(), "Start recording...",
                Toast.LENGTH_SHORT).show();
    }

    public void stop(View view){
        try {
            myRecorder.stop();
            myRecorder.release();
            myRecorder  = null;

            String d = "-";


            Toast.makeText(getApplicationContext(), "Stop recording...",
                    Toast.LENGTH_SHORT).show();


        } catch (IllegalStateException e) {
            //  it is called before start()
            e.printStackTrace();
        } catch (RuntimeException e) {
            // no valid audio/video data has been received
            e.printStackTrace();
        }


        AlertDialog.Builder builder1 = new AlertDialog.Builder(ActivityServer.this);
        builder1.setMessage("صدا ارسال شود؟");
        builder1.setCancelable(true);

        builder1.setPositiveButton(
                "بله",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        ChatClient client = (ChatClient)spUsers.getSelectedItem();
                       if(client != null) {

                           client.chatThread.sendMsg("voicemessage");
                           ActivityServer.this.runOnUiThread(new Runnable() {

                               @Override
                               public void run() {
                                   mMessages.add(new Message.Builder(Message.TYPE_VOICE)
                                           .username("me: ").message(fileName).build());
                                   mAdapter.notifyItemInserted(mMessages.size() - 1);
                                   scrollToBottom();

                               }
                           });

                       }

                            dialog.cancel();
                    }
                });

        builder1.setNegativeButton(
                "خیر",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        AlertDialog alert11 = builder1.create();
        alert11.show();

    }

    private void scrollToBottom() {
        mMessagesView.scrollToPosition(mAdapter.getItemCount() - 1);
    }

}
