package guzhijistudio.transfile.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

public final class ContentUtil {

    public static String getPath(Context context, Uri uri) {
        if (uri == null) return null;
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = { MediaStore.MediaColumns.DATA };
            Cursor cursor;
            try {
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                if (cursor != null) {
                    try {
                        int column_index = cursor.getColumnIndexOrThrow(projection[0]);
                        if (cursor.moveToFirst()) {
                            return cursor.getString(column_index);
                        }
                    } finally {
                        cursor.close();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

}
