package com.example.alex.vuzixftw;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.TextView;

import com.example.alex.vuzixftw.MusicService.MusicBinder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

//import android.R;

//Change away from example

//Note: to add song, android evice manager, file system, mnt/Sdcard/Music, press the push button (top right)
public class MainActivity extends AppCompatActivity implements MediaPlayerControl {
    private MusicService musicSrv;
    private Intent playIntent;
    private boolean musicBound = false;
    private Cursor musicCursor;
    private ArrayList<Song> songList;
    private ListView songView;
    private MusicController controller;
    private boolean paused = false, playbackPaused = false;
    public TextView lyricsView;
    private int currentTime;
    public String nextLyrics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        songView = (ListView) findViewById(R.id.song_list);
        songList = new ArrayList<Song>();

        getSongList();

        Collections.sort(songList, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });

        SongAdapter songAdt = new SongAdapter(this, songList);
        songView.setAdapter(songAdt);

        setController();
    }

    private ServiceConnection musicConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicBinder binder = (MusicBinder) service;
            musicSrv = binder.getService();
            musicSrv.setList(songList);
            musicBound = true;
        }

        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    public void getSongList() {
        //retrieve song info
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if (musicCursor != null && musicCursor.moveToFirst()) {
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            //add songs to listg
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                songList.add(new Song(thisId, thisTitle, thisArtist));
            }
            while (musicCursor.moveToNext());
        }
    }

    protected void onStart() {
        super.onStart();
        if (playIntent == null) {
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    public void songPicked(View view) {
        int viewTag = (Integer) view.getTag();
        musicSrv.setSong(viewTag);
        musicSrv.playSong();
        if (playbackPaused) {
            setController();
            playbackPaused = false;
        }
        controller.show(0);
        display_lyrics();
    }

    public void display_lyrics() {
        currentTime = 0;
        setContentView(R.layout.lyrics_display);
        lyricsView = (TextView) findViewById(R.id.lyricsTextView);

        LyricsDisplay lyrics_display = new LyricsDisplay();
        lyricsView = (TextView) findViewById(R.id.lyricsTextView);

        String testFile = "George Michael - Careless Whisper (Lyrics)";
        nextLyrics = lyrics_display.parseLyric(testFile, currentTime);

        while (nextLyrics != "END_LYRICS") {
            System.out.println("in while");
            currentTime += 500; //TODO: Replace with current time.
            nextLyrics = lyrics_display.parseLyric(testFile, currentTime);

            final String finalNextLyrics = nextLyrics;
            Thread t = new Thread() { //TODO: Not going into here!!!
                public void run() {
                    System.out.println(finalNextLyrics);
                    testView(finalNextLyrics);
                    try {
                        TimeUnit.SECONDS.sleep(2);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };

            t.start();

            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        /*
        int i = 0;
        while (i == 0) {
            currentTime += 500; //TODO: Replace with current time.
            testView(next_lyrics);
            next_lyrics = lyrics_display.parseLyric(testFile, currentTime);
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            i++;
            System.out.println("in while");
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("after while");
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        */


        /*
        String next_lyrics = "";
        while (next_lyrics != "END_LYRICS") {
            currentTime += 500; //TODO: Replace with current time.
            next_lyrics = lyrics_display.parseLyric(testFile, currentTime);
            testView(next_lyrics);
            lyricsView.setText(next_lyrics);
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        */
    }

    public void testView(String newText){
//        System.out.println(newText);
        lyricsView.setText(newText);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_shuffle:
                musicSrv.setShuffle();
                break;
            case R.id.action_end:
                stopService(playIntent);
                musicSrv = null;
                System.exit(0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void onDestroy() {
        stopService(playIntent);
        unbindService(musicConnection);
        musicSrv = null;
        musicCursor.close();
        super.onDestroy();
    }

    @Override
    public void start() {
        musicSrv.go();

    }

    @Override
    public void pause() {
        playbackPaused = true;
        musicSrv.pausePlayer();
    }

    @Override
    public int getDuration() {
        if (musicSrv != null && musicBound && musicSrv.isPng())
            return musicSrv.getDur();
        else return 0;
    }

    @Override
    public int getCurrentPosition() {
        if (musicSrv != null && musicBound && musicSrv.isPng())
            return musicSrv.getPosn();
        else return 0;
    }

    @Override
    public void seekTo(int i) {
        musicSrv.seek(i);
    }

    @Override
    public boolean isPlaying() {
        if (musicSrv != null && musicBound)
            return musicSrv.isPng();
        return false;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return false;
    }

    @Override
    public boolean canSeekForward() {
        return false;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    @Override
    protected void onPause() {
        super.onPause();
        paused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (paused) {
            setController();
            paused = false;
        }
    }

    @Override
    protected void onStop() {
        controller.hide();
        super.onStop();
    }

    private void setController() {
        //set the controller up
        controller = new MusicController(this);

        controller.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNext();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrev();
            }
        });

        controller.setMediaPlayer(this);
        controller.setAnchorView(findViewById(R.id.song_list));
        controller.setEnabled(true);
    }

    //play next
    private void playNext() {
        musicSrv.playNext();
        if (playbackPaused) {
            setController();
            playbackPaused = false;
        }
        controller.show(0);
    }

    //play previous
    private void playPrev() {
        musicSrv.playPrev();
        if (playbackPaused) {
            setController();
            playbackPaused = false;
        }
        controller.show(0);
    }
}