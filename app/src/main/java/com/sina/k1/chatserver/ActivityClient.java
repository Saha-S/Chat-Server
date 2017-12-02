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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ActivityClient extends AppCompatActivity {

    static final int SocketServerPORT = 8080;

    LinearLayout loginPanel, chatPanel;

    EditText editTextUserName, editTextAddress;
    Button buttonConnect;
    TextView chatMsg, textPort;

    EditText editTextSay;
    ImageButton buttonSend;
    Button buttonDisconnect;

    String msgLog = "";

    ChatClientThread chatClientThread = null;
  //  private LinearLayout container;
    private View child;
    private ToggleButton play;
    private TextView txtClient;
    private String voice;

    private List<Message> mMessages = new ArrayList<Message>();

    private RecyclerView mMessagesView;
    private RecyclerView.Adapter mAdapter;
    private Handler mTypingHandler = new Handler();
    private ToggleButton record;
    private String fileName;
    private String outputFile;
    private MediaRecorder myRecorder;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        loginPanel = (LinearLayout)findViewById(R.id.loginpanel);
        chatPanel = (LinearLayout)findViewById(R.id.chatpanel);

        editTextUserName = (EditText) findViewById(R.id.username);
        editTextAddress = (EditText) findViewById(R.id.address);
        textPort = (TextView) findViewById(R.id.port);
        textPort.setText("port: " + SocketServerPORT);
        buttonConnect = (Button) findViewById(R.id.connect);
        buttonDisconnect = (Button) findViewById(R.id.disconnect);
        // chatMsg = (TextView) findViewById(R.id.chatmsg);

        buttonConnect.setOnClickListener(buttonConnectOnClickListener);
        buttonDisconnect.setOnClickListener(buttonDisconnectOnClickListener);

        editTextSay = (EditText)findViewById(R.id.say);
        buttonSend = (ImageButton)findViewById(R.id.send);
        record = (ToggleButton)findViewById(R.id.tg_record);



        mAdapter = new MessageAdapter(App.context, mMessages );

        mMessagesView = (RecyclerView) findViewById(R.id.messages);
        mMessagesView.setLayoutManager(new LinearLayoutManager(ActivityClient.this));
        mMessagesView.setAdapter(mAdapter);


        buttonSend.setOnClickListener(buttonSendOnClickListener);

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


    View.OnClickListener buttonDisconnectOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if(chatClientThread==null){
                return;
            }
            chatClientThread.disconnect();
        }

    };

    View.OnClickListener buttonSendOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (editTextSay.getText().toString().equals("")) {
                return;
            }

            if(chatClientThread==null){
                return;
            }

            chatClientThread.sendMsg(editTextSay.getText().toString() + "\n");
            editTextSay.setText("");
        }

    };

    View.OnClickListener buttonConnectOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            String textUserName = editTextUserName.getText().toString();
            if (textUserName.equals("")) {
                Toast.makeText(ActivityClient.this, "Enter User Name",
                        Toast.LENGTH_LONG).show();
                return;
            }

            String textAddress = editTextAddress.getText().toString();
            if (textAddress.equals("")) {
                Toast.makeText(ActivityClient.this, "Enter Addresse",
                        Toast.LENGTH_LONG).show();
                return;
            }

            //   msgLog = "";
            // chatMsg.setText(msgLog);
            loginPanel.setVisibility(View.GONE);
            chatPanel.setVisibility(View.VISIBLE);

            chatClientThread = new ChatClientThread(
                    textUserName, textAddress, SocketServerPORT);
            chatClientThread.start();
        }

    };

    private class ChatClientThread extends Thread {

        String name;
        String dstAddress;
        int dstPort;

        String msgToSend = "";
        boolean goOut = false;

        ChatClientThread(String name, String address, int port) {
            this.name = name;
            dstAddress = address;
            dstPort = port;
        }

        @Override
        public void run() {
            Socket socket = null;
            DataOutputStream dataOutputStream = null;
            DataInputStream dataInputStream = null;

            try {
                socket = new Socket(dstAddress, dstPort);
                dataOutputStream = new DataOutputStream(
                        socket.getOutputStream());
                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream.writeUTF(name);
                dataOutputStream.flush();

                while (!goOut) {
                    if (dataInputStream.available() > 0) {
                        String charT = String.valueOf(dataInputStream.readChar());
                        Log.i("ttttt", "char: " + charT);

                        if (charT.equals("I")) {

                            fileName = new SimpleDateFormat("yyyy-MM-ddHHmmssSSS'.mp3'").format(new Date());
                            final String outputFile = Environment.getExternalStorageDirectory().
                                    getAbsolutePath() + "/"+fileName;
                            File path = new File(outputFile);
                            voice=outputFile;
                            long length = dataInputStream.readLong();
                            DataOutputStream dataOutputStream2 = new DataOutputStream(new FileOutputStream(path));

                            byte[] buffer = new byte[16384];
                            boolean end= false;
                            while (!end){
                                dataOutputStream2.write(buffer, 0, dataInputStream.read(buffer, 0, buffer.length));

                                if(buffer.length >= length ){
                                    end = true;
                                    dataOutputStream2.flush();
                                    ActivityClient.this.runOnUiThread(new Runnable() {

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


                            if (charT.equals("T")){
                                msgLog = dataInputStream.readUTF();

                                if (msgLog.contains("server$&*K@CHUUFWDWETKMYTK@@JHR")) {

                                msgLog = msgLog.replace("server$&*K@CHUUFWDWETKMYTK@@JHR", " ");

                                ActivityClient.this.runOnUiThread(new Runnable() {

                                    @Override
                                    public void run() {
                                        //  chatMsg.setText(msgLog);
                                        final LayoutInflater inflater = (LayoutInflater) getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
                                        final View child = inflater.inflate(R.layout.right, null, false);
                                        //  TextView txtServer = (TextView) child.findViewById(R.id.txt_cserver);
                                      //  TextView txtClient = (TextView) child.findViewById(R.id.txt_client);
                                      //  txtClient.setText(msgLog);
                                      //  container.addView(child);
                                        mMessages.add(new Message.Builder(Message.TYPE_RIGHT)
                                                .username("").message("من : "+msgLog).build());
                                        mAdapter.notifyItemInserted(mMessages.size() - 1);
                                        scrollToBottom();



                                    }
                                });

                            } else {
                                // final String ss = msgLog;

                                ActivityClient.this.runOnUiThread(new Runnable() {

                                    @Override
                                    public void run() {
                                        mMessages.add(new Message.Builder(Message.TYPE_LEFT)
                                                .username("").message(msgLog).build());
                                        mAdapter.notifyItemInserted(mMessages.size() - 1);
                                        scrollToBottom();



                                    }
                                });
                            }

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

            } catch (UnknownHostException e) {
                e.printStackTrace();
                final String eString = e.toString();
                ActivityClient.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(ActivityClient.this, eString, Toast.LENGTH_LONG).show();
                    }

                });
            } catch (IOException e) {
                e.printStackTrace();
                final String eString = e.toString();
                ActivityClient.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(ActivityClient.this, eString, Toast.LENGTH_LONG).show();
                    }

                });
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
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

                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                ActivityClient.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        loginPanel.setVisibility(View.VISIBLE);
                        chatPanel.setVisibility(View.GONE);
                    }

                });
            }

        }

        private void sendMsg(String msg){
            msgToSend = msg;
            Log.i("hhhhhhh" , "1");
        }

        private void disconnect(){
            goOut = true;
        }
    }

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


        AlertDialog.Builder builder1 = new AlertDialog.Builder(ActivityClient.this);
        builder1.setMessage("صدا ارسال شود؟");
        builder1.setCancelable(true);

        builder1.setPositiveButton(
                "بله",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {


                        chatClientThread.sendMsg("voicemessage");
                            ActivityClient.this.runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    //  txtClient.setText("me: " + "موزیک");
                                    //  container.addView(child);
                                    mMessages.add(new Message.Builder(Message.TYPE_VOICE)
                                            .username("me: ").message(fileName).build());
                                    mAdapter.notifyItemInserted(mMessages.size() - 1);
                                    scrollToBottom();

                                }
                            });



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
