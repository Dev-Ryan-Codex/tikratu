package dev.ryan.tikratu;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import dev.ryan.tikratu.utils.AppPrefs;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/* JADX INFO: loaded from: classes6.dex */
public class PrefsProvider extends ContentProvider {
    public static final String AUTHORITY = "dev.ryan.tikratu.prefs";
    public static final Uri URI_ALL = Uri.parse("content://dev.ryan.tikratu.prefs/all");

    @Override // android.content.ContentProvider
    public boolean onCreate() {
        return true;
    }

    @Override // android.content.ContentProvider
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        MatrixCursor cursor = new MatrixCursor(new String[]{"key", "value"});
        Properties props = new Properties();
        try {
            FileInputStream in = new FileInputStream(AppPrefs.file(getContext()));
            try {
                props.load(in);
                in.close();
            } catch (Throwable th) {
                try {
                    in.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        } catch (IOException e) {
        }
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            cursor.addRow(new Object[]{entry.getKey().toString(), entry.getValue().toString()});
        }
        return cursor;
    }

    @Override // android.content.ContentProvider
    public String getType(Uri uri) {
        return "vnd.android.cursor.dir/vnd.dev.ryan.tikratu.prefs";
    }

    @Override // android.content.ContentProvider
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Solo lectura");
    }

    @Override // android.content.ContentProvider
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Solo lectura");
    }

    @Override // android.content.ContentProvider
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Solo lectura");
    }
}
