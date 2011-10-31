package com.mash.masciicam;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.mash.masciicam.MASCIICamView;


public class MASCIICamActivity extends Activity 
{
    private static final String TAG = "mASCIIcam"; 
    public static boolean DEBUG = false;
    public static boolean DEMO_MODE = false;
    
    private MASCIICamView		mView;    
    private MenuItem            mItemSave;
    private MenuItem			mItemFocusAuto;
    private MenuItem			mItemFocusMacro;
    private MenuItem			mItemFocusInfinity;
    private MenuItem			mItemFocusFixed;
    private MenuItem			mItemFocusEdof;
    private MenuItem	 		mItemFocusContinuous;   
    private MenuItem			mItemInfo;    
    private MenuItem	 		mItemFrontCamera;
    private MenuItem	 		mItemBackCamera;
    private MenuItem	 		mItemSettings;
    private MenuItem	 		mItemFPS;
    private MenuItem	 		mItemInvert;
    private MenuItem	 		mItemFlash;  
    
    
    private PowerManager.WakeLock mWakeLock;        
    
    public static final int 	DIALOG_INFO=0;
          
    public MASCIICamActivity() 
    {
   		if (Log.isLoggable(TAG, Log.INFO)) 
   			Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
   		if (DEBUG) Log.i(TAG, "onCreate");
        
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                WindowManager.LayoutParams.FLAG_FULLSCREEN);            
        mView = new MASCIICamView(this);
        setContentView(mView);
        registerForContextMenu(mView);
        
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "DoNotDim");
        mWakeLock.acquire();        
    }
    
    @Override
    protected void onPause() 
    {
    	super.onPause();
    	mWakeLock.release();
    }
    
    @Override
    protected void onResume() 
    {
    	super.onResume();
    	mWakeLock.acquire();
    }    

    @Override
    protected void onDestroy() 
    {
    	mWakeLock.release();
    	super.onResume();    	
    }    

    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
    	if (DEBUG) Log.i(TAG, "onCreateOptionsMenu");
    	
        if (!DEMO_MODE) {
        	mItemSave = menu.add("Save snapshot");
        	mItemSave.setIcon(android.R.drawable.ic_menu_save);
    	}
        mItemSettings = menu.add("Settings");
        mItemSettings.setIcon(android.R.drawable.ic_menu_preferences);
        mItemInfo = menu.add("Info");   
        mItemInfo.setIcon(android.R.drawable.ic_menu_info_details); 
      
        return true;
    }
 
    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	if (DEBUG) Log.i(TAG, "Menu Item selected " + item);
   		
        if (item == mItemSave) {
        	Toast.makeText(this,mView.saveFile(), Toast.LENGTH_SHORT).show();
        } else if (item == mItemInfo) {
    		showDialog(DIALOG_INFO);        
        } else if (item == mItemSettings) {
        	openContextMenu(mView);
        }        
        return true;
    }

    @Override
    public void onCreateContextMenu(
    		ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) 
    {
    	menu.setHeaderTitle("Settings");
        if (mView.getFocusModes() != null) {
        	Menu focusMenu = menu.addSubMenu("Focus modes");
	        if (mView.getFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO))
	        	mItemFocusAuto = focusMenu.add("(Auto)Focus Now!"); 
	        if (mView.getFocusModes().contains(Camera.Parameters.FOCUS_MODE_MACRO))
	        	mItemFocusMacro = focusMenu.add("Macro");
	        if (mView.getFocusModes().contains(Camera.Parameters.FOCUS_MODE_FIXED))
	        	mItemFocusFixed = focusMenu.add("Fixed");
	        if (mView.getFocusModes().contains(Camera.Parameters.FOCUS_MODE_INFINITY))
	        	mItemFocusInfinity = focusMenu.add("Infinity");
	        if (mView.getFocusModes().contains(Camera.Parameters.FOCUS_MODE_EDOF))
	        		mItemFocusEdof= focusMenu.add("EDOF");
	        // new in android 2.3
	        if (Build.VERSION.SDK_INT>=9) { 
	        	if (mView.getFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
	        		mItemFocusContinuous = focusMenu.add("continuous");
	        }      
        }        

        // new in android 2.3
        if (Build.VERSION.SDK_INT>=9) { 
        	if (mView.hasMultipleCameras()) {
        		Menu cameraMenu = menu.addSubMenu("Select camera");
        		mItemFrontCamera =  cameraMenu.add("Front facing camera");
        		mItemBackCamera =  cameraMenu.add("Back facing camera");
        	}
        }    
        if (mView.getInvert())
        	mItemInvert = menu.add("Inverse off");
        else
        	mItemInvert = menu.add("Inverse"); 
        
        if (mView.getFlashModes() != null) {
	        if (mView.isFlashOn())
	        	mItemFlash = menu.add("Turn flashlight off");
	        else
	        	mItemFlash = menu.add("Turn flashlight on");        
	        }
        /*                
        if (mView.showsFPS())
        	mItemFPS = menu.add("Hide FPS");
        else
        	mItemFPS = menu.add("Show FPS");        
        */    		
    }
 
    @Override
    public Dialog onCreateDialog(int id) 
    {
        switch(id) {
        case DIALOG_INFO:
        	AlertDialog.Builder builder = new AlertDialog.Builder(this);
        	
        	if (DEMO_MODE) {
        	builder.setMessage(
        			"A simple and puristic image-as-ASCII-art camera viewer.\n" +
        			"\n" +
        			"If you have fun and like it, consider spending a buck on buying " +
        			"a full version (so you get rid of ads and can save snapshots too!)"         			
        			)
            	   .setTitle("mASCIIcam - Free Demo")
        	       .setCancelable(true)
        	       .setPositiveButton("Buy", new DialogInterface.OnClickListener() {
        	           public void onClick(DialogInterface dialog, int id) {
        	        	   Intent intent = new Intent(Intent.ACTION_VIEW);
        	        	   intent.setData(Uri.parse("market://details?id=com.mash.masciicam"));
        	        	   startActivity(intent);
        	           }
        	       })
        	       .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
        	           public void onClick(DialogInterface dialog, int id) {
        	                dialog.cancel();
        	           }
        	       });
        	} else {
            	builder.setMessage(
            			"A simple and puristic image-as-ASCII-art camera viewer.\n" +
            			"\n" +
            			"written by Michael Aschauer (http://m.ash.to)\n"        			
            			)
                	   .setTitle("mASCIIcam")
            	       .setCancelable(true)
            	       .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
            	           public void onClick(DialogInterface dialog, int id) {
            	                dialog.cancel();
            	           }
            	       });    		
        	}
        	return builder.create();

        default:
        	return null;
        }
        
    }    
    
    @Override
    public boolean onContextItemSelected(MenuItem item) 
    {
    	if (DEBUG) Log.i(TAG, "Menu Item selected " + item);
   		
        if (item == mItemSave) {
        	Toast.makeText(this,mView.saveFile(), Toast.LENGTH_SHORT).show();
        } 
        else if (item == mItemFPS)
        	mView.toggleFPSDisplay();
        else if (item == mItemInvert)
        	mView.toggleInvert();   
        else if (item == mItemFlash)
        	mView.toggleFlash();
        else if (item == mItemFocusAuto)
        	mView.AutofocusNow();          
        else if (item == mItemFocusMacro)
        	mView.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO); 
        else if (item == mItemFocusInfinity)
        	mView.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);         
        else if (item == mItemFocusFixed)
        	mView.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED); 
        else if (item == mItemFocusEdof)
        	mView.setFocusMode(Camera.Parameters.FOCUS_MODE_EDOF);
        
        // new in android 2.3
        if (Build.VERSION.SDK_INT>=9) { 
        	if (item == mItemFocusContinuous)
        		mView.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        	else if (item == mItemFrontCamera)
        		mView.setCamera("front");
        	else if (item == mItemBackCamera)
        		mView.setCamera("back");        	
        }                
        return true;
    }
    
    public void alert() {
    	showDialog(DIALOG_INFO);   
    }
}
