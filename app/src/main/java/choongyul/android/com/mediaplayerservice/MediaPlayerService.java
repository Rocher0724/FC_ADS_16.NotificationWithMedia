package choongyul.android.com.mediaplayerservice;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.media.MediaPlayer;
import android.media.Rating;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;

public class MediaPlayerService extends Service {

    // 서비스플레이 액션 정의
    public static final String ACTION_PLAY = "choongyul.android.com.mediaplayerservice.play";
    public static final String ACTION_PAUSE = "choongyul.android.com.mediaplayerservice.pause";
    public static final String ACTION_REWIND = "choongyul.android.com.mediaplayerservice.rewind";
    public static final String ACTION_FAST_FORWARD = "choongyul.android.com.mediaplayerservice.fast.foward";
    public static final String ACTION_NEXT = "choongyul.android.com.mediaplayerservice.next";
    public static final String ACTION_PREVIOUS = "choongyul.android.com.mediaplayerservice.previous";
    public static final String ACTION_STOP = "choongyul.android.com.mediaplayerservice.stop";

    //사용 API 세팅

    private MediaPlayer mMediaPlayer;
    private MediaSessionManager mManager;
    private MediaSession mSession;
    private MediaController mController;

    public MediaPlayerService() {
    }


    // 인텐트 액션에 넘어온 명령어를 분기시키는 함수
    public void handleIntent( Intent intent ) {
        if( intent == null || intent.getAction() == null )
            return;
        String action = intent.getAction();
        if( action.equalsIgnoreCase( ACTION_PLAY ) ) {
            mController.getTransportControls().play();
        } else if ( action.equalsIgnoreCase(ACTION_PAUSE) ) {
            mController.getTransportControls().pause();
        } else if ( action.equalsIgnoreCase(ACTION_FAST_FORWARD) ) {
            mController.getTransportControls().fastForward();
        } else if ( action.equalsIgnoreCase(ACTION_REWIND) ) {
            mController.getTransportControls().rewind();
        } else if ( action.equalsIgnoreCase(ACTION_PREVIOUS) ) {
            mController.getTransportControls().skipToPrevious();
        } else if ( action.equalsIgnoreCase(ACTION_NEXT) ) {
            mController.getTransportControls().skipToNext();
        } else if ( action.equalsIgnoreCase(ACTION_STOP) ) {
            mController.getTransportControls().stop();
        }
    }

    // Noti.Action -> API Level 19
    // Activity에서 클릭 버튼 생성
    private Notification.Action generateAction(int icon, String title, String intentAction ) {
        Intent intent = new Intent( getApplicationContext(), MediaPlayerService.class );
        intent.setAction( intentAction );
        //Pending Intent : 실행 대상이 되는 인텐트를 지연 시키는 역할                 여기 1은 구분자이다. 사용할때 리턴받아서 구분한다.
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
            Icon iconTemp = Icon.createWithResource(getBaseContext(),icon);
            return new Notification.Action.Builder(iconTemp, title, pendingIntent).build();
        }else {
            return new Notification.Action.Builder(icon, title, pendingIntent).build();
        }
    }

    // 노티 바를 생성하는 함수
    private void buildNotification( Notification.Action action, String action_flag ) {
        // 노티 바의 모양 결정 (왜 없어도 된다 하셨을까?)
        Notification.MediaStyle style = new Notification.MediaStyle();

        // 노티 바 전체를 클릭 했을 때 실행되는 메인 인텐트
        Intent intent = new Intent( getApplicationContext(), MediaPlayerService.class );
        intent.setAction( ACTION_STOP );
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);
        // 노티 바 생성
        Notification.Builder builder = new Notification.Builder( this );

        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle( "Title" )
                .setContentText( "Artist" );

        // 현 상태가 퍼즈일 경우만 노티 삭제 가능
        if( action_flag == ACTION_PAUSE ) {
            builder.setDeleteIntent(pendingIntent)
                    .setOngoing(true);
        }
        builder.setStyle(style);

        builder.addAction( generateAction( android.R.drawable.ic_media_previous, "Previous", ACTION_PREVIOUS ) );
        builder.addAction( generateAction( android.R.drawable.ic_media_rew, "Rewind", ACTION_REWIND ) );
        builder.addAction( action );
        builder.addAction( generateAction( android.R.drawable.ic_media_ff, "Fast Foward", ACTION_FAST_FORWARD ) );
        builder.addAction( generateAction( android.R.drawable.ic_media_next, "Next", ACTION_NEXT ) );

        // 안드 버전별로 나올수 있는 버튼(롤리팝 이하는 3개) 갯수가 다르기 때문에 버전별로 서로 다르게 표시되도록 세팅
        // action의 중요도에 따라 꼭 보여져야 되는 action을 앞쪽에 배치한다. 번호는 순서대로 0번
        style.setShowActionsInCompactView(1,2,3,0,4);

        NotificationManager notificationManager = (NotificationManager) getSystemService( Context.NOTIFICATION_SERVICE );
        // 노티바를 화면에 보여준다.
        notificationManager.notify( 1, builder.build() );
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if( mManager == null ) {
            initMediaSessions();
        }

        handleIntent( intent );
        return super.onStartCommand(intent, flags, startId);
    }

    // 21버전 이상 에서 쓸수 있는 미디어 컨트롤 방식.
    private void initMediaSessions() {
        mMediaPlayer = new MediaPlayer();
        Uri musicUri = Uri.parse("음원 Uri");
        try {
            mMediaPlayer.setDataSource(musicUri.getPath());
            mMediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // String 부분은 태그이다.
        mSession = new MediaSession(getApplicationContext(), "Media Player Session");
        // getSesscionToken()을 통해 현재 실행된 미디어 플레이어의 토큰을 가져온다고 하는데 토큰이 뭘까
        mController =new MediaController(getApplicationContext(), mSession.getSessionToken());

        mSession.setCallback(new MediaSession.Callback(){
            @Override
            public void onPlay() {
                super.onPlay();
                Log.e( "MediaPlayerService", "onPlay");
                buildNotification( generateAction( android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE ) , ACTION_PAUSE );
            }

            @Override
            public void onPause() {
                super.onPause();
                Log.e( "MediaPlayerService", "onPause");
                buildNotification(generateAction(android.R.drawable.ic_media_play, "Play", ACTION_PLAY), ACTION_PLAY);
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                Log.e( "MediaPlayerService", "onSkipToNext");
                //Change media here
                buildNotification( generateAction( android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE ) , ACTION_PAUSE );
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                Log.e( "MediaPlayerService", "onSkipToPrevious");
                //Change media here
                buildNotification( generateAction( android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE ) ,ACTION_PAUSE );
            }

            @Override
            public void onFastForward() {
                super.onFastForward();
                Log.e( "MediaPlayerService", "onFastForward");
                //Manipulate current media here
            }

            @Override
            public void onRewind() {
                super.onRewind();
                Log.e( "MediaPlayerService", "onRewind");
                //Manipulate current media here
            }

            @Override
            public void onStop() {
                super.onStop();
                Log.e( "MediaPlayerService", "onStop");
                //Stop media player here
                NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel( 1 );
                Intent intent = new Intent( getApplicationContext(), MediaPlayerService.class );
                stopService( intent );
            }

            @Override
            public void onSeekTo(long pos) {
                super.onSeekTo(pos);
            }

            @Override
            public void onSetRating(Rating rating) {
                super.onSetRating(rating);
            }
        });
    }







    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}

