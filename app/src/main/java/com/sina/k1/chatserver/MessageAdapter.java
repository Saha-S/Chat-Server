package com.sina.k1.chatserver;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import nl.changer.audiowife.AudioWife;


public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    private List<Message> mMessages;
    private int[] mUsernameColors;
    MediaPlayer player;
    private String outputFile;
    Handler seekHandler = new Handler();
    Runnable run;

    public MessageAdapter(Context context, List<Message> messages) {
        mMessages = messages;
        mUsernameColors = context.getResources().getIntArray(R.array.username_colors);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int layout = -1;
        switch (viewType) {
        case Message.TYPE_LEFT:
            layout = R.layout.left;
            break;
        case Message.TYPE_RIGHT:
            layout = R.layout.right;
            break;
        case Message.TYPE_LOG:
            layout = R.layout.item_log;
            break;
        case Message.TYPE_ACTION:
            layout = R.layout.item_action;
            break;
            case Message.TYPE_VOICE:
                layout = R.layout.item_voice;
                break;
        }
        View v = LayoutInflater
                .from(App.context)
                .inflate(layout, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        Message message = mMessages.get(position);
        holder.setMessage(message.getMessage());
     //   viewHolder.playVoice(message.getMessage(), position);
        holder.setUsername(message.getUsername());

        outputFile = Environment.getExternalStorageDirectory().
                getAbsolutePath() + "/" + message.getMessage();


        // Initializing MediaPlayer
        final MediaPlayer mediaPlayer = new MediaPlayer();

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mediaPlayer.setDataSource(outputFile);
            mediaPlayer.prepare();// might take long for buffering.
        } catch (IOException e) {
            e.printStackTrace();
        }
        try{

            holder.mMediaSeekBar.setMax(mediaPlayer.getDuration());
            holder.mMediaSeekBar.setTag(position);
            //run.run();
            holder.mMediaSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (mediaPlayer != null && fromUser) {
                        mediaPlayer.seekTo(progress);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
            holder.mTotalTime.setText("0:00/" + calculateDuration(mediaPlayer.getDuration()));
            holder.btn_play.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!mediaPlayer.isPlaying()) {
                        mediaPlayer.start();
                        holder.btn_play.setText("Pause");
                        run = new Runnable() {
                            @Override
                            public void run() {
                                // Updateing SeekBar every 100 miliseconds
                                holder.mMediaSeekBar.setProgress(mediaPlayer.getCurrentPosition());
                                seekHandler.postDelayed(run, 100);
                                //For Showing time of audio(inside runnable)
                                int miliSeconds = mediaPlayer.getCurrentPosition();
                                if (miliSeconds != 0) {
                                    //if audio is playing, showing current time;
                                    long minutes = TimeUnit.MILLISECONDS.toMinutes(miliSeconds);
                                    long seconds = TimeUnit.MILLISECONDS.toSeconds(miliSeconds);
                                    if (minutes == 0) {
                                        holder.mTotalTime.setText("0:" + seconds + "/" + calculateDuration(mediaPlayer.getDuration()));
                                    } else {
                                        if (seconds >= 60) {
                                            long sec = seconds - (minutes * 60);
                                            holder.mTotalTime.setText(minutes + ":" + sec + "/" + calculateDuration(mediaPlayer.getDuration()));
                                        }
                                    }
                                } else {
                                    //Displaying total time if audio not playing
                                    int totalTime = mediaPlayer.getDuration();
                                    long minutes = TimeUnit.MILLISECONDS.toMinutes(totalTime);
                                    long seconds = TimeUnit.MILLISECONDS.toSeconds(totalTime);
                                    if (minutes == 0) {
                                        holder.mTotalTime.setText("0:" + seconds);
                                    } else {
                                        if (seconds >= 60) {
                                            long sec = seconds - (minutes * 60);
                                            holder.mTotalTime.setText(minutes + ":" + sec);
                                        }
                                    }
                                }
                            }

                        };
                        run.run();
                    } else {
                        mediaPlayer.pause();
                        holder.btn_play.setText("Play");
                    }
                }
            });

        }catch (Exception e){}


    /*    if(viewHolder.mPlayMedia==null) return;
        else {

            outputFile = Environment.getExternalStorageDirectory().
                    getAbsolutePath() + "/" + message.getMessage();
            final Uri uri = Uri.parse(outputFile);
            Log.i("pathhhh", outputFile);

                    if (outputFile == null)
                        Toast.makeText(App.context, "Pick an audio file before playing", Toast.LENGTH_LONG).show();
                    else {
                        AudioWife.getInstance()
                                .init(App.context, uri)
                                .setPlayView(viewHolder.mPlayMedia)
                                .setPauseView(viewHolder.mPauseMedia)
                                .setSeekBar(viewHolder.mMediaSeekBar)
                                .setRuntimeView(viewHolder.mRunTime)
                                .setTotalTimeView(viewHolder.mTotalTime);




                    }
        }
*/
    }

    @Override
    public int getItemCount() {
        return mMessages.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mMessages.get(position).getType();
    }

     class ViewHolder extends RecyclerView.ViewHolder {
        private TextView mUsernameView;
        private TextView mMessageView;
       private Button btn_play;
      ///  private View mPlayMedia;
     //   private View mPauseMedia;
        private SeekBar mMediaSeekBar;
        private TextView mRunTime;
        private TextView mTotalTime;
        private TextView mPlaybackTime;
        private RelativeLayout item;
        private AudioWife audioWife;



        public ViewHolder(View itemView) {
            super(itemView);

            mUsernameView = (TextView) itemView.findViewById(R.id.username);
            mMessageView = (TextView) itemView.findViewById(R.id.message);
            btn_play = (Button) itemView.findViewById(R.id.play);
     //      mPlayMedia = itemView.findViewById(R.id.play);
     //       mPauseMedia = itemView.findViewById(R.id.pause);
            mMediaSeekBar = (SeekBar) itemView.findViewById(R.id.media_seekbar);
            mTotalTime = (TextView) itemView.findViewById(R.id.total_time);
            item = (RelativeLayout) itemView.findViewById(R.id.ll_server);


        }

        public void setUsername(String username) {
            if (null == mUsernameView) return;
            mUsernameView.setText(username);
            mUsernameView.setTextColor(getUsernameColor(username));
        }

        public void setMessage(String message) {
            if (null == mMessageView) return;
            mMessageView.setText(message);
        }
        public void playVoice(final String message , final int pos) {

            /*
            if(mPlayMedia==null) return;
            else {
                audioWife = new AudioWife();

                outputFile = Environment.getExternalStorageDirectory().
                        getAbsolutePath() + "/" + message;
                final Uri uri = Uri.parse(outputFile);
                Log.i("pathhhh", outputFile);

                mPlayMedia.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        if (outputFile == null)
                            Toast.makeText(App.context, "Pick an audio file before playing", Toast.LENGTH_LONG).show();
                        else {
                            audioWife.getInstance()
                                    .init(App.context, uri)
                                    .setPlayView(mPlayMedia)
                                    .setPauseView(mPauseMedia)
                                    .setSeekBar(mMediaSeekBar)
                                    .setRuntimeView(mRunTime)
                                    .setTotalTimeView(mTotalTime);

                            audioWife.getInstance().addOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                                @Override
                                public void onCompletion(MediaPlayer mp) {
                                    Toast.makeText(App.context, "Completed", Toast.LENGTH_SHORT).show();

                                    notifyDataSetChanged();

                                    // do you stuff.
                                }
                            });

                            audioWife.getInstance().addOnPlayClickListener(new View.OnClickListener() {

                                @Override
                                public void onClick(View v) {
                                    Toast.makeText(App.context, "Play", Toast.LENGTH_SHORT).show();
                                    // get-set-go. Lets dance.
                                }
                            });

                            audioWife.getInstance().addOnPauseClickListener(new View.OnClickListener() {

                                @Override
                                public void onClick(View v) {
                                    Toast.makeText(App.context, "Pause", Toast.LENGTH_SHORT).show();
                                    // Your on audio pause stuff.
                                }
                            });

                        }
                    }
                });

       /*    AudioWife.getInstance()
                    .init(App.context, Uri.parse(outputFile))
                    .setPlayView(mPlayMedia)
                    .setPauseView(mPauseMedia)
                    .setSeekBar(mMediaSeekBar)
                    .setRuntimeView(mRunTime)
                    .setTotalTimeView(mTotalTime);

*/


            }
        }

        private int getUsernameColor(String username) {
            int hash = 7;
            for (int i = 0, len = username.length(); i < len; i++) {
                hash = username.codePointAt(i) + (hash << 5) - hash;
            }
            int index = Math.abs(hash % mUsernameColors.length);
            return mUsernameColors[index];
        }

        public  void play(String file){
/*
          //  App.CustomToast(file);
            AudioWife.getInstance()
                    .init(App.context, Uri.parse(file))
                    .setPlayView(mPlayMedia)
                    .setPauseView(mPauseMedia)
                    .setSeekBar(mMediaSeekBar)
                    .setRuntimeView(mRunTime)
                    .setTotalTimeView(mTotalTime);*/
        }


    private String calculateDuration(int duration) {
        String finalDuration = "";
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(duration);
        if (minutes == 0) {
            finalDuration = "0:" + seconds;
        } else {
            if (seconds >= 60) {
                long sec = seconds - (minutes * 60);
                finalDuration = minutes + ":" + sec;
            }
        }
        return finalDuration;
    }

}
