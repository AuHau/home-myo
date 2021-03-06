package cz.cvut.uhlirad1.homemyo.localization;

import android.content.Context;
import cz.cvut.uhlirad1.homemyo.localization.cat.CatTracker;
import cz.cvut.uhlirad1.homemyo.localization.cat.CatTracker_;

/**
 * Author: Adam Uhlíř <uhlir.a@gmail.com>
 * Date: 3.12.14
 */
public final class TrackerFactory {

    public static final int WAITING_TIME = 1000;

    public static final int WAITING_ITERATION = 4;

    private TrackerFactory() {
    }

    public static ITracker createTracker(Context context){
        return CatTracker_.getInstance_(context);
    }
}
