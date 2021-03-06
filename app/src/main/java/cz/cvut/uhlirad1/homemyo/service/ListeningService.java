package cz.cvut.uhlirad1.homemyo.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.*;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;
import com.thalmic.myo.*;
import cz.cvut.uhlirad1.homemyo.MainActivity;
import cz.cvut.uhlirad1.homemyo.R;
import cz.cvut.uhlirad1.homemyo.knx.cat.Location_;
import cz.cvut.uhlirad1.homemyo.settings.AppPreferences_;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EService;
import org.androidannotations.annotations.sharedpreferences.Pref;

/**
 * Service which is running in separate thread and is listening for Myo gestures,
 * detect Combos and send commands to KNX network.
 *
 * @author: Adam Uhlíř <uhlir.a@gmail.com>
 */
@EService
public class ListeningService extends Service {

    private static final String TAG = "ListeningService";
    public static int LOCATION_UPDATE = 1;
    private Toast toast;
    final Messenger messenger = new Messenger(new LocationMessenger());


    @Bean
    protected MyoListener listener;

    @Pref
    protected AppPreferences_ preferences;

    @Pref
    protected Location_ location;

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    /**
     * Method will create new Thread for the Myo listener.
     */
    @Override
    public void onCreate() {
        super.onCreate();

        HandlerThread thread = new HandlerThread("MyoListener",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper, this);
    }

    /**
     * Method will start created Thread and show notification.
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        showToast("Watching for Myo gestures");

        makeNotification();

        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        mServiceHandler.sendMessage(msg);

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

    /**
     * Method will terminate Myo listener and Hub
     */
    @Override
    public void onDestroy() {
        showToast("Stop watching for Myo gestures");

        Hub.getInstance().removeListener(listener);
        Hub.getInstance().shutdown();

        destroyNotification();
    }

    /**
     * Method will create persist notification
     */
    private void makeNotification(){
        Intent notifyIntent = new Intent(this, MainActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, notifyIntent, 0);

        Notification n  = new Notification.Builder(this)
                .setContentTitle("Home Myo is running")
                .setContentText("We are watching for your gestures!")
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setAutoCancel(false)
                .setContentIntent(pIntent)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_LOW)
                .build();

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(0, n);
    }

    /**
     * Method will destroy persist notification
     */
    private void destroyNotification(){
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(0);
    }

    /**
     * Method will show Toast
     * @param text text showed in Toast
     */
    private void showToast(String text) {
        Log.w(TAG, text);
        if (toast == null) {
            toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        } else {
            toast.setText(text);
        }
        toast.show();
    }

    /**
     * Class which is used by RTLS localization system for passing
     * detected user locality.
     */
    public final class LocationMessenger extends Handler {

        @Override
        public void handleMessage(Message msg) {
            Log.i("ListenerService", "Location recieved - " + msg.arg1);
                location.location().put(msg.arg1);
            super.handleMessage(msg);
        }
    }

    /**
     * Class which handle Myo initialization and starting new Thread.
     */
    private final class ServiceHandler extends Handler {

        private Context context;

        public ServiceHandler(Looper looper, Context context) {
            super(looper);
            this.context = context;
        }

        @Override
        public void handleMessage(Message msg) {

            Hub hub = Hub.getInstance();
            if (!hub.init(context, getPackageName())) {
                showToast("Couldn't initialize Hub");
                stopSelf();
                return;
            }

            hub.setLockingPolicy(Hub.LockingPolicy.NONE);

            listener = MyoListener_.getInstance_(context);
            hub.addListener(listener);

            if (hub.getConnectedDevices().isEmpty() && !preferences.myoMac().get().isEmpty()) {
                    hub.attachByMacAddress(preferences.myoMac().get());
            }
        }
    }
}