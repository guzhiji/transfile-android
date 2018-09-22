package guzhijistudio.transfile.utils;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;

public final class PermUtil {

    public static boolean checkPermissions(Activity ctx, String[] permissions, boolean doRequest, int requestCode) {
        ArrayList<String> toRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(ctx, permission) != PackageManager.PERMISSION_GRANTED)
                toRequest.add(permission);
        }
        if (toRequest.isEmpty()) return true;
        if (doRequest)
            ActivityCompat.requestPermissions(ctx, toRequest.toArray(new String[0]), requestCode);
        return false;
    }

}
