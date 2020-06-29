package ru.mobilap.firebasedatabase;

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
import android.view.View;
import java.math.BigDecimal;
import java.io.IOException;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Currency;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Locale;
import java.util.Date;
import java.util.Set;
import java.util.HashSet;
import java.lang.Exception;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.ChildEventListener;

import org.godotengine.godot.Godot;
import org.godotengine.godot.Dictionary;
import org.godotengine.godot.plugin.GodotPlugin;
import org.godotengine.godot.plugin.SignalInfo;

public class FirebaseDatabase extends GodotPlugin {

    final private SignalInfo getValueSignal = new SignalInfo("get_value");
    final private SignalInfo childAddedSignal = new SignalInfo("child_added");
    final private SignalInfo childChangedSignal = new SignalInfo("child_changed");
    final private SignalInfo childMovedSignal = new SignalInfo("child_moved");
    final private SignalInfo childRemovedSignal = new SignalInfo("child_removed");


    private Godot activity = null;
    private com.google.firebase.database.FirebaseDatabase database = null;
    private DatabaseReference dbref = null;
    //private ValueEventListener rootValueListener = null;
    private ChildEventListener rootChildListener = null;

    public FirebaseDatabase(Godot godot) 
    {
        super(godot);
        activity = godot;
        database = com.google.firebase.database.FirebaseDatabase.getInstance();
        database.setPersistenceEnabled(true);
        dbref = database.getReference();
    }

    @Override
    public String getPluginName() {
        return "FirebaseDatabase";
    }

    @Override
    public List<String> getPluginMethods() {
        return Arrays.asList(
                             "set_db_root",
                             "set_value",
                             "push_child",
                             "update_children",
                             "remove_value",
                             "get_value");
    }

    @Override
    public Set<SignalInfo> getPluginSignals() {
        return new HashSet<SignalInfo>(Arrays.asList(getValueSignal, childAddedSignal, childChangedSignal, childMovedSignal, childRemovedSignal));
    }

    @Override
    public View onMainCreate(Activity activity) {
        return null;
    }

    // Public methods

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
                        emitSignal(childAddedSignal.getName(), new Object[] {key, value});
                    }
                    @Override
                    public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
                        Object value = snapshot.getValue();
                        String key = snapshot.getKey();
                        emitSignal(childChangedSignal.getName(), new Object[] {key, value});
                    }
                    @Override
                    public void onChildMoved(DataSnapshot snapshot, String previousChildName) {
                        Object value = snapshot.getValue();
                        String key = snapshot.getKey();
                        emitSignal(childMovedSignal.getName(), new Object[] {key, value});
                    }
                    @Override
                    public void onChildRemoved(DataSnapshot snapshot) {
                        Object value = snapshot.getValue();
                        String key = snapshot.getKey();
                        emitSignal(childRemovedSignal.getName(), new Object[] {key, value});
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
                    emitSignal(getValueSignal.getName(), new Object[] {key, value});
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Getting value failed, log a error message
                    Log.w("godot", "FBDB get_value:onCancelled", databaseError.toException());
                }});
    }
}
