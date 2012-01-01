package com.mash.masciicam;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import com.mash.tools.FpsMeter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PreviewCallback;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

public class MASCIICamView 	extends SurfaceView 
implements SurfaceHolder.Callback, Runnable 
{
	private static final String TAG = "mASCIIcam";
    public static boolean DEBUG = true;
    
    private int w=80;
    private int h;
    private int textSize=14;
    private long mFrameCounter=0;
    
    private static Camera		mCamera;
    private SurfaceHolder       mHolder;
    private static int         	mFrameWidth;
    private static int          mFrameHeight;
    private int         		mCanvasWidth;
    private int          		mCanvasHeight;
    
    private byte[]              mFrame;

    private FpsMeter            mFps;
    private int            		mNumberOfCameras=1;
    private int            		mDefaultCameraId;
    private static int          mCameraId;
    private int            		mFrontCameraId;
    private int            		mBackCameraId;
    private int					mDrawRotation=0;
    
    private List<String>		mFocusModes;
    private List<Camera.Size> 	mResolutions;
    
    private Mat mYuv;
    private Mat mGraySubmat;
    private Mat mGray;

    private Paint textPaint;
    private int backGroundColor = Color.BLACK;

    private String[] vlines;
    //private String symbolstr = " .,_:;onmwYOR%$#";
    private char[] symbolarr = {' ', '.', ',',':','+','=','Y','o','n','m','w','%','Q','R','$','#'};
    // All ASCII characters, sorted according to their visual density
    // from processing
    /*private String letterOrder =
      " .`-_':,;^=+/\"|)\\<>)iv%xclrs{*}I?!][1taeo7zjLu" +
      "nT#JCwfy325Fp6mqSghVd4EgXPGZbYkOA&8U$@KHDBWNMR0Q";
    */
    protected static boolean mResolutionChanged=false;
    private boolean mHasMultipleCameras = false;
    private boolean showFPS = false;    
    private boolean	mInvert = false;
	private boolean mFlashIsOn = false;    
    private boolean mThreadRun;
    private boolean flipH = false;
    private boolean flipV = false;

    
    public MASCIICamView(Context context) 
    {
        super(context);
        
        mHolder = getHolder();
        mHolder.addCallback(this);
        mFps = new FpsMeter();        
        
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTypeface(Typeface.MONOSPACE);
        textPaint.setTextSize(textSize);
        
        if (DEBUG)  {
   			Log.i(TAG, "Instantiated new  " + this.getClass());
   			Log.i(TAG, "Android version  " + Build.VERSION.SDK_INT);
   		}
        
        /*
        if (Build.VERSION.SDK_INT>=9) {
    	  initSensor();
    	}  
    	*/      
    }

    
    public void surfaceChanged(SurfaceHolder _holder, int format, int width, int height) 
    {
    	if (DEBUG) Log.i(TAG, "surfaceChanged");
   		
        if (mCamera != null) {
            mFrameWidth = width;
            mFrameHeight = height;
            mCanvasWidth = width;
            mCanvasHeight = height;

            //choose resolution that fits screen
            setMinimalResolution();
            
            if (DEBUG) {}
          
            //mResolutionChanged = true;
            synchronized(this) {
                // initialize Mats before usage
                mYuv = new Mat(getFrameHeight() + getFrameHeight() / 2, getFrameWidth(), CvType.CV_8UC1);
                mGraySubmat = new Mat();
                mGray = new Mat();   
                           
                // get orientation
            	Display display  = ((WindowManager) this.getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            	int d_rotation = display.getRotation();
            	int degrees = 0;
            	switch (d_rotation) {
            		case Surface.ROTATION_0: degrees = 0; break;
            		case Surface.ROTATION_90: degrees = 270; break;
            		case Surface.ROTATION_180: degrees = 180; break;
            		case Surface.ROTATION_270: degrees = 90; break;
            	}
            	Log.i(TAG,"surface rotation = " + degrees);
            	int orientation = degrees;
            	//Log.i(TAG,"surface orientation = " + mOrientation);       
        		int rotation = 0;
        		
                if (Build.VERSION.SDK_INT >= 9) {
                	
                	Camera.CameraInfo info = new Camera.CameraInfo();
    	      	    Camera.getCameraInfo(mCameraId, info);
    	      	    orientation = (orientation + 45) / 90 * 90;	      	    
    	 	    	if (DEBUG) Log.i(TAG,"camera rotation is " + info.orientation);                   	     
    	      	    if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
    	      	        rotation = (info.orientation - orientation + 360) % 360;
    	      	    } else {  // back-facing camera
    	      	        rotation = (info.orientation + orientation) % 360;
    	      	    }
    	      	    if (DEBUG) Log.i(TAG,"rotation should be: " + rotation);      	             
                } else {
                	if (mCanvasWidth > mCanvasHeight) 
                		rotation =  0;
                	else
                		rotation = 90;
                }
                
                if (mCanvasWidth > mCanvasHeight) {
                	if (DEBUG) Log.i(TAG,"orientation is landscape");
                	w = getCanvasWidth() / 8;
                	h = (int)( ((float)w / (float)getFrameWidth()) * (float)getFrameHeight() * 4 / 7);
                	vlines = new String[h];
                } else {
                	if (DEBUG) Log.i(TAG,"orientation is portrait");
                	w = getCanvasHeight() / 14;
                	h = (int)( ((float)w / (float)getFrameWidth()) * (float)getFrameHeight() * 7 / 4);
                	vlines = new String[w];            
                }
                
                mDrawRotation = rotation;
                
                if (DEBUG) Log.i(TAG,"surface draw rotation = " + mDrawRotation);
                
               // mResolutionChanged = false;
            }                         
          
            try {
				mCamera.setPreviewDisplay(null);
			} catch (IOException e) {
				Log.e(TAG, "mCamera.setPreviewDisplay fails: " + e);
			}
            mCamera.startPreview();
        }
        
    }

    public void surfaceCreated(SurfaceHolder holder) 
    {   
    	initCamera();
    	
    	if (DEBUG) Log.i(TAG, "surfaceCreated");
                
        (new Thread(this)).start();
    }

    public void surfaceDestroyed(SurfaceHolder holder) 
    {
    	if (DEBUG) Log.i(TAG, "surfaceDestroyed");
        
   		mThreadRun = false;
        if (mCamera != null) {
            synchronized (this) {
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);
                mCamera.release();
                mCamera = null;
            }
        }
    }

    public String[] processFrame(byte[] data) 
    {   	
    	if (vlines != null) {
	        mYuv.put(0, 0, data);
	    	
	        Imgproc.resize(mYuv, mGraySubmat, new Size(w,h+h/2));
	    	mGray = mGraySubmat.submat(0, h, 0, w);
	    	if (flipV && flipH)
	    		Core.flip(mGray.clone(), mGray, -1);
	    	else if (flipV)
	    		Core.flip(mGray.clone(), mGray, 1);
	    	else if (flipH)
	    		Core.flip(mGray.clone(), mGray, 0);
	        
	    	int v = 0;
	    	//float d = (float)letterOrder.length()/(float)256;

	    	//if (DEBUG) Log.i(TAG,"size:" + mGray.rows() + "x" + mGray.cols());
	    	//if (DEBUG) Log.i(TAG,"size:" + mCanvasWidth + "x" + mCanvasHeight);
	    	//if (mCanvasWidth > mCanvasHeight) {
	    	if (mDrawRotation == 0 ) {
	            for(int row=0; row < mGray.rows(); row++) {
	          //  	if (DEBUG) Log.i(TAG,"row:" + row);
	            	vlines[row] = "";
	            	for(int col=0; col < mGray.cols(); col++) {        		
	            		
	            		if (isFrontCamera())
	            			v = (int)mGray.get(row, mGray.cols() - col - 1)[0];
	            		else
	            			v = (int)mGray.get(row, col)[0];    		
	            		
	            		if (mInvert)
	            			v = 255 - v;    
	            		
	            		vlines[row] = vlines[row] + symbolarr[v/16];
	            		//vlines[row] = vlines[row] + letterOrder.charAt((int)(d*v));
	                }
	            	
	            }
	            
	        } else if (mDrawRotation == 180 ) {
	            for(int row=0; row < mGray.rows(); row++) {
	            	//if (DEBUG) Log.i(TAG,"row:" + row);
	            	vlines[row] = "";
	            	for(int col=0; col < mGray.cols(); col++) {        		
	            		if (isFrontCamera())
	            			v = (int)mGray.get(mGray.rows() - row - 1, col)[0];
	            		else
	            			v = (int)mGray.get(mGray.rows() - row - 1, mGray.cols() - col - 1)[0];
	            		if (mInvert)
	            			v = 255 - v;  
	            		vlines[row] = vlines[row] + symbolarr[v/16];
	            		//vlines[row] = vlines[row] + letterOrder.charAt((int)(d*v));	            		
	                }
	            }
	            
	        } else if (mDrawRotation == 90) {        
	            for(int col=0; col < mGray.cols(); col++) {
	            	//if (DEBUG) Log.i(TAG,"row:" + col);
	            	vlines[col] = "";
	            	for(int row=0; row < mGray.rows(); row++) {        		
	            		if (isFrontCamera())
	            			v = (int)mGray.get(mGray.rows() - row - 2, mGray.cols() - col - 1)[0];
	            		else
	            			v = (int)mGray.get(mGray.rows() - row - 2, col)[0];
	            		if (mInvert)
	            			v = 255 - v;  
	            		vlines[col] = vlines[col] + symbolarr[v/16];
	            		//vlines[row] = vlines[row] + letterOrder.charAt((int)(d*v));	            		
	            	}
	            }
	        } else if (mDrawRotation == 270) {        
	            for(int col=0; col < mGray.cols(); col++) {
	            	//if (DEBUG) Log.i(TAG,"row:" + col);
	            	vlines[col] = "";
	            	for(int row=0; row < mGray.rows(); row++) {        		
	            		if (isFrontCamera())
	            			v = (int)mGray.get(mGray.rows() - row - 2, mGray.cols() - col - 1)[0];
	            		else
	            			v = (int)mGray.get(row, col)[0];
	            		if (mInvert)
	            			v = 255 - v;  
	            		vlines[col] = vlines[col] + symbolarr[v/16];
	            		//vlines[row] = vlines[row] + letterOrder.charAt((int)(d*v));	            		
	            	}
	            }
	        } else {
	        	return null;
	        }
	        mFrameCounter++;
    	}
		return vlines;
    }

    
    public void run() 
    {
        mThreadRun = true;
        
        if (DEBUG) Log.i(TAG, "Starting processing thread");
        
        mFps.init();

        String[] str= null;
        
        while (mThreadRun) {                       
            synchronized (this) {
                try {
                    this.wait();
                    str = processFrame(mFrame);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }       

            mFps.measure();
            
            if (str != null) {
                Canvas canvas = mHolder.lockCanvas();
                if (canvas != null) {
                	canvas.drawColor(backGroundColor);                	
                	                	
                	int offs = (getCanvasHeight() - (str.length * textSize))/2;
                	for(int i = 0; i < str.length; i++) {
                		canvas.drawText(str[i], 0, textSize + offs + i * textSize, textPaint);
                	}                     
  
                	if (showFPS)
                		mFps.draw(canvas, canvas.getWidth()/2+20, canvas.getHeight()-25);
                	
                	mHolder.unlockCanvasAndPost(canvas); 
                }
                str = null;
            }            
                       
        }
        synchronized (this) {
            // Explicitly deallocate Mats
            if (mYuv != null)
                mYuv.release(); 
            if (mGraySubmat != null)
            	mGraySubmat.release(); 
            if (mGray != null)
            	mGray.release();            
            mYuv = null;
            mGraySubmat = null; 
            mGray = null; 
        }              
    }

    
    
    public void initCamera() 
    {
    	if (Build.VERSION.SDK_INT>=9) {
    		mDefaultCameraId = 0;    		
        	mNumberOfCameras = Camera.getNumberOfCameras();
        	
        	if (DEBUG)  { 
       			Log.i(TAG, "available cameras:");
       			Log.i(TAG, "Found " + mNumberOfCameras + " cameras.");
       		}
       		
        	if (mNumberOfCameras > 1) {
        		mHasMultipleCameras = true;
        		// Find the ID of the default camera
        		CameraInfo cameraInfo = new CameraInfo();
        		for (int i = 0; i < mNumberOfCameras; i++) {
        			Camera.getCameraInfo(i, cameraInfo);
        			if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
        				mDefaultCameraId = i;
        				mBackCameraId = i;
        				mCameraId = i;
        			} else if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
        				mFrontCameraId = i;
        			}
        		}        		
        	}        	
        	mCamera = Camera.open(mDefaultCameraId); 
        } else {
        	mCamera = Camera.open();
        }
    	
    	mFocusModes = mCamera.getParameters().getSupportedFocusModes();
    	
    	if (DEBUG)   {
        	Log.i(TAG, "focus modes:" + mCamera.getParameters().getFocusMode());
            Log.i(TAG, "available focus modes: " + mFocusModes); 
            if (Build.VERSION.SDK_INT>=9) {
            	Log.i(TAG,"supported preview formats: " + mCamera.getParameters().getSupportedPreviewFormats().toString());    	
            	Log.i(TAG,"supported preview fps range:" + mCamera.getParameters().getSupportedPreviewFpsRange().toString());
            	Log.i(TAG,"supported preview Framerates: " + mCamera.getParameters().getSupportedPreviewFrameRates());
            }
        	Log.i(TAG,"current preview format:" + mCamera.getParameters().getPreviewFormat());    	        
        	Log.i(TAG,"current preview frame rate: " + mCamera.getParameters().getPreviewFrameRate());
        	Log.i(TAG,"zoom supported?: " + mCamera.getParameters().isZoomSupported());
        	if (mCamera.getParameters().isZoomSupported()) {
        		Log.i(TAG,"current zoom: " + mCamera.getParameters().getZoom());
        		Log.i(TAG,"max zoom: " + mCamera.getParameters().getMaxZoom());
        		Log.i(TAG,"zoom ratios: " + mCamera.getParameters().getZoomRatios());
        	}
   		}    	

        mCamera.setPreviewCallback(new PreviewCallback() {
            public void onPreviewFrame(byte[] data, Camera camera) {
                synchronized (MASCIICamView.this) {
                    mFrame = data;
                    MASCIICamView.this.notify();
                }
            }
        });
    }
    
    public List<String> getFocusModes() 
    {
        return mCamera.getParameters().getSupportedFocusModes();
    }    

    public String getFocusMode() 
    {
        return mCamera.getParameters().getFocusMode();
    }
    
    public boolean setFocusMode(String mode) 
    {
    	Camera.Parameters params = mCamera.getParameters();
    	if (mFocusModes.contains(mode)) {
    		params.setFocusMode(mode);
    		mCamera.setParameters(params);
    		if (DEBUG) Log.i(TAG, "set Focus Mode to " + mode);
    		return true;
    	} else {
    		if (DEBUG) Log.i(TAG, "FAILED in setting Focus Mode to " + mode);
    		return false;
    	}    	
    }
    
	public void AutofocusNow() 
	{
		if (getFocusMode() != Camera.Parameters.FOCUS_MODE_AUTO) 
			setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO); 
		mCamera.autoFocus(null);
	} 
	
    public List<String> getFlashModes() 
    {
    	if (mCamera.getParameters().getSupportedFlashModes()!=null) {
    		if (DEBUG) Log.i(TAG,mCamera.getParameters().getSupportedFlashModes().toString());
    		return mCamera.getParameters().getSupportedFlashModes();
    	} else {
    		return null;
    	}
    }    	
    
    public List<Camera.Size> getResolutions() 
    {
        return mResolutions;
    }        
 
    protected boolean isFrontCamera() 
    { 
    	if (Build.VERSION.SDK_INT < 9)
    		return false;
    	else
    		return mCameraId == mFrontCameraId;
    }    
    
    public void setCamera(String type) 
    {
    	if (Build.VERSION.SDK_INT>=9) {
        	mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            
        	if (type == "front") {
        		if (DEBUG) Log.i(TAG, "open front facing camera:");        		
        		mCamera = Camera.open(mFrontCameraId);
        		Camera.Parameters params = mCamera.getParameters();
        		mCamera.setParameters(params);
        		mCameraId = mFrontCameraId;
        	} else  {
        		if (DEBUG) Log.i(TAG, "open back facing camera:");
        		mCamera = Camera.open(mBackCameraId);
        		Camera.Parameters params = mCamera.getParameters();
        		mCamera.setParameters(params);
        		mCameraId = mBackCameraId;
        	}
        	
        	mFocusModes = mCamera.getParameters().getSupportedFocusModes();
        	if (DEBUG)   {
	            Log.i(TAG, "available focus modes: " + mFocusModes); 
	        	Log.i(TAG,"supported preview formats: " + mCamera.getParameters().getSupportedPreviewFormats().toString());    	
	        	Log.i(TAG,"current:" + mCamera.getParameters().getPreviewFormat());    	
	        	Log.i(TAG,"supported preview fps range:" + mCamera.getParameters().getSupportedPreviewFpsRange().toString());
	        	Log.i(TAG,"supported preview Framerates: " + mCamera.getParameters().getSupportedPreviewFrameRates());
	        	Log.i(TAG,"current: " + mCamera.getParameters().getPreviewFrameRate());           	
       		}
       		
        	setMinimalResolution();
    		mResolutionChanged = true;
    	
    		mCamera.setPreviewCallback(new PreviewCallback() {
    			public void onPreviewFrame(byte[] data, Camera camera) {
    				synchronized (MASCIICamView.this) {
    					mFrame = data;
    					MASCIICamView.this.notify();
    				}
    			}
    		});    	
    		mCamera.startPreview();
    	}
    }    
    
    public void toggleFPSDisplay() 
    {
    	if (showFPS) 
    		showFPS = false;
    	else
    		showFPS = true;
    }   
    
    public boolean showsFPS() 
    {
    	return showFPS;
    } 
    
    public void setMinimalResolution() 
    {	
    	Camera.Parameters params = mCamera.getParameters();
        mResolutions = params.getSupportedPreviewSizes();
                           
        if (DEBUG) Log.i(TAG, "available resolutions:");
        // selecting optimal camera preview size
        {
            double minDiff = Double.MAX_VALUE;
            for (Camera.Size size : mResolutions) {
                if (Math.abs(size.width) < minDiff) {
                    mFrameWidth = size.width;
                    mFrameHeight = size.height;
                    minDiff = size.width;
                    if (DEBUG) Log.i(TAG, size.width + "x" + size.height);
                }                    
            }
        }
        
        params.setPreviewSize(getFrameWidth(), getFrameHeight()); 
        mCamera.setParameters(params);
        if (DEBUG) Log.i(TAG, "setting resolution: " + getFrameWidth() + "x" + getFrameHeight());  	
    }
    
    public boolean hasMultipleCameras() 
    {
    	return mHasMultipleCameras;
    }

    public int getFrameWidth() 
    {
        return mFrameWidth;
    }

    public int getFrameHeight() 
    {
        return mFrameHeight;
    }
    
    public int getCanvasWidth() 
    {
        return mCanvasWidth;
    }

    public int getCanvasHeight() 
    {
        return mCanvasHeight;
    }        
    
    public void toggleInvert() 
    {
    	if (!mInvert) {
    		mInvert = true;
            textPaint.setColor(Color.BLACK);
            textPaint.setTypeface(Typeface.MONOSPACE);
            textPaint.setTextSize(textSize); 
            backGroundColor = Color.WHITE;    		
    	} else {
            mInvert = false;    		
        	textPaint.setColor(Color.WHITE);
            textPaint.setTypeface(Typeface.MONOSPACE);
            textPaint.setTextSize(textSize);
            backGroundColor = Color.BLACK;    		
    	}
    }
    
    public boolean getInvert() 
    {
        return mInvert;
    }

    public String saveFile() 
    {
    	String filename;
	   	Date date = new Date();
	   	SimpleDateFormat sdf = new SimpleDateFormat ("yyyyMMddHHmmss");
	   	filename =  sdf.format(date);
	    String state = Environment.getExternalStorageState();
        
	   	if (Environment.MEDIA_MOUNTED.equals(state)) 
        {
		   	try{	
			   		String path = Environment.getExternalStorageDirectory().toString();
			   		BufferedWriter fOut = null;
			   		
			   		File fpath=new File(path,"mASCIIcam");
			   		
			   		if (!fpath.exists()) fpath.mkdirs();
			   		File file = new File(fpath, filename+".txt");
			   		fOut = new BufferedWriter(new FileWriter(file));
			   		if (DEBUG) 
			   			Log.i(TAG, "saving file to" + file.toString());	  	
   	
			   		synchronized(this) {
			   			for(int i=0; i<vlines.length;i++)
			   				fOut.write(vlines[i]+"\n");			   			
			   		}

			   		fOut.write(" (shot with mASCIIcam on Android) \n");			   		
				    fOut.flush();
			   		fOut.close();	

			   		file = new File(fpath, filename+".html");
			   		fOut = new BufferedWriter(new FileWriter(file));
			   		
			   		if (DEBUG) 
			   			Log.i(TAG, "saving file to" + file.toString());

			   		fOut.write("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">");
			   		fOut.write("<html>\n<head\n>");
			   		fOut.write("<title>mASCIICam - Snapshot - " + filename + "</title>\n");
			   		fOut.write("</head>\n");
			   		if(mInvert)
			   			fOut.write("<body style=\"background-color:#fff;color:#000;font-size:10px\">\n");
			   		else
			   			fOut.write("<body style=\"background-color:#000;color:#fff;font-size:10px\">\n");
			   		fOut.write("<pre>\n");
			   		for(int i=0; i<vlines.length;i++)
			   			fOut.write("<b>" + vlines[i] + "</b>\n");
			   		
			   		fOut.write("  (shot with <a href=\"http://market.android.com/details?id=com.mash.masciicam\">mASCIIcam</a> on Android)\n");
			   		fOut.write("</pre>\n");			   		
			   		fOut.write("</body>\n");
			   		fOut.write("</html>\n");
			   		
				    fOut.flush();
				    fOut.close();
				    
			   		return "Saved image as HTML and TXT to:\n " + fpath.toString();				

		   	} catch (Exception e) {
		   		e.printStackTrace();
		   		return "saving image failed.";
		   	}		   	
        } else {
        	if (Log.isLoggable(TAG, Log.WARN))
        		Log.w(TAG, "saving image failed. no external media found");
        	return "Saving image failed. No sdcard found!";
        }
    }

    public boolean isFlashOn() 
    {
    	return mFlashIsOn;
    }

	public void toggleFlash() 
	{
		Camera.Parameters params = mCamera.getParameters();
    	if (mFlashIsOn) {
    		mFlashIsOn = false;
    		params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
    		if (DEBUG) Log.i(TAG, "turning flash off");
    	} else {
    		mFlashIsOn = true;
    		params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
    		if (DEBUG) Log.i(TAG, "turning flash on");    		
    	}  
    	mCamera.setParameters(params);
	}


	public void toggleFlipV() {
    	if (flipV) 
    		flipV = false;
    	else
    		flipV = true;	
	}
	
	public void toggleFlipH() {
    	if (flipH) 
    		flipH = false;
    	else
    		flipH = true;			
	}	

	public boolean isFlipV() {
		return flipV;		
	}

	public boolean isFlipH() {
		return flipH;		
	}
	
/*
	private void initSensor()
	{
        OrientationEventListener orientationListener = 
        	new OrientationEventListener(this.getContext()) {
            
            @Override
            public void onOrientationChanged(int orientation) {
                synchronized (this) {
                   	     if (orientation == ORIENTATION_UNKNOWN) {
                   	    	 mOrientation = -1;
                   	    	 return;
                   	     } else {
                   	    	 mOrientation = orientation;
                   	     }
                    }
            	}
        };
        orientationListener.enable();
    }*/
}