package org.godotengine.godot;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.Context;
import android.os.Bundle;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.util.DisplayMetrics;
import android.telephony.TelephonyManager;
import android.view.WindowManager;
import android.view.Display;
import java.math.BigDecimal;
import java.io.IOException;
import java.io.File;
import java.util.Currency;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Locale;
import java.util.Date;
import java.lang.Exception;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.ChildEventListener;

public class GodotFirebaseDatabase extends Godot.SingletonBase {

    private Godot activity = null;
    private FirebaseDatabase database = null;
    private DatabaseReference dbref = null;
    //private ValueEventListener rootValueListener = null;
    private ChildEventListener rootChildListener = null;

    static public Godot.SingletonBase initialize(Activity p_activity) 
    { 
        return new GodotFirebaseDatabase(p_activity); 
    } 

    public GodotFirebaseDatabase(Activity p_activity) 
    {
        registerClass("FirebaseDatabase", new String[]{
                "set_db_root",
                "set_value",
                "push_child",
                "update_children",
                "remove_value",
                "get_value"
            });
        // it will work event without argument types
        addSignal("FirebaseDatabase", "get_value", new String[] {"Ljava/lang/String;", "Ljava/lang/Object;"});
        addSignal("FirebaseDatabase", "child_added", new String[] {"Ljava/lang/String;", "Ljava/lang/Object;"});
        addSignal("FirebaseDatabase", "child_changed", new String[] {"Ljava/lang/String;", "Ljava/lang/Object;"});
        addSignal("FirebaseDatabase", "child_moved", new String[] {"Ljava/lang/String;", "Ljava/lang/Object;"});
        addSignal("FirebaseDatabase", "child_removed", new String[] {"Ljava/lang/String;", "Ljava/lang/Object;"});
        activity = (Godot)p_activity;
        database = FirebaseDatabase.getInstance();
        dbref = database.getReference();
    }

    // Public methods

    public void init(final String key)
    {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    //mAuth = FirebaseAuth.getInstance();
                } catch (Exception e) {
                    Log.e("godot", "Exception: " + e.getMessage());  
                }
            }
        });
    }

    public void set_db_root(final String[] path) {
        if(dbref != null && rootChildListener != null) {
            dbref.removeEventListener(rootChildListener);
        }
        dbref = database.getReference();
        dbref = getReferenceForPath(path);
        if(rootChildListener == null) {
            rootChildListener = new ChildEventListener() {
                    @Override
                    public void onCancelled(DatabaseError error) {
                    }
                    @Override
                    public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                        Object value = snapshot.getValue();
                        String key = snapshot.getKey();
                        emitSignal("FirebaseDatabase", "child_added", new Object[] {key, value});
                    }
                    @Override
                    public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
                        Object value = snapshot.getValue();
                        String key = snapshot.getKey();
                        emitSignal("FirebaseDatabase", "child_changed", new Object[] {key, value});
                    }
                    @Override
                    public void onChildMoved(DataSnapshot snapshot, String previousChildName) {
                        Object value = snapshot.getValue();
                        String key = snapshot.getKey();
                        emitSignal("FirebaseDatabase", "child_moved", new Object[] {key, value});
                    }
                    @Override
                    public void onChildRemoved(DataSnapshot snapshot) {
                        Object value = snapshot.getValue();
                        String key = snapshot.getKey();
                        emitSignal("FirebaseDatabase", "child_removed", new Object[] {key, value});
                    }
                };
        }
        dbref.addChildEventListener(rootChildListener);
    }
    
    private DatabaseReference getReferenceForPath(final String[] path) {
        DatabaseReference ref = dbref;
        for (String p : path) {
            ref = ref.child(p);
        }
        return ref;
    }

    public void set_value(final String[] path, final Object value) {
        DatabaseReference ref = getReferenceForPath(path);
        ref.setValue(value);
    }

    public String push_child(final String[] path) {
        DatabaseReference ref = getReferenceForPath(path);
        ref = ref.push();
        return ref.getKey();
    }

    public void update_children(final String[] paths, final Dictionary params) {
        Dictionary updates = new Dictionary();
        for(String path: paths) {
            updates.put(path, params);
        }
        dbref.updateChildren(updates);
    }

    public void remove_value(final String[] path) {
        DatabaseReference ref = getReferenceForPath(path);
        ref.removeValue();
    }

    public void get_value(final String[] path) {
        DatabaseReference ref = getReferenceForPath(path);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Object value = dataSnapshot.getValue();
                    String key = dataSnapshot.getKey();
                    emitSignal("FirebaseDatabase", "get_value", new Object[] {key, value});
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Getting value failed, log a error message
                    Log.w("godot", "FBDB get_value:onCancelled", databaseError.toException());
                }});
    }

    // Internal methods

    public void callbackSuccess(String ticket, String signature, String sku) {
		//GodotLib.callobject(facebookCallbackId, "purchase_success", new Object[]{ticket, signature, sku});
        //GodotLib.calldeferred(purchaseCallbackId, "consume_fail", new Object[]{});
	}
    @Override protected void onMainActivityResult (int requestCode, int resultCode, Intent data)
    {
    }
}
