
package com.amine;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import java.io.File;
import android.util.Log;
import com.android.vending.util.*;
import android.content.Intent;
import android.net.Uri;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.ScaleGestureDetector;

public class JNIActivity extends Activity 
{
    JNIView mView;
	IabHelper mIAB;
	private static String TAG = "VLAD";
	public boolean m_HasFPSMode = false;
	public boolean m_PreviousHasFPSMode = false;
	public String m_IABError = "";
	private ScaleGestureDetector SGD;

    @Override protected void onCreate(Bundle icicle)
	{
        super.onCreate(icicle);
			
		String HeartOfTarrasque = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjZ0i5gnjEyQsY10DN5Dyn+ioH86974QWwD+6GHd/y6grsN0GKoPUF/1PoIpBxNk616JCh/7Ua4R04wEof+33nrrc2N7r1qtYNeGC3fCqUQ7vIq6TPE0t3wD9PDfBt/aVImyDEn9g6mXbx/Ipo8aJDjmgdm8AJrpHUqOLPNIuj5jJrGMxY05cI7ZdNfl30LYekh0P5H/xuZ23KrTdaK/ToeOLRp8rUBdHshC6yCWcvJhTXlB/uUzAK2gTJ3YTG/bfhCAMcUQWKWRuxEjgsQdrEyYTdaZeevMs5E4d9Ytq3rcNxR9VApPJ5XF9kSkTrO1/NnEg86hdPt+g2+Fae9VutwIDAQAB";		
		mIAB = new IabHelper(this, HeartOfTarrasque);
		//mIAB.enableDebugLogging(true);
		mIAB.startSetup(new IabHelper.OnIabSetupFinishedListener()
		{
			public void onIabSetupFinished(IabResult result)
			{
				if(!result.isSuccess())
				{
					Log.e("VLAD", "Problem setting up In-app Billing: " + result);
					if(mIAB != null)
					{
						mIAB.dispose();
						mIAB = null;
					}
					return;
				}
				
				if(mIAB == null)
				{
					Log.e("VLAD", "In-app Billing helper was disposed of unexpectedly");
				}
				else
				{
					Log.d("VLAD", "In-app Billing Initialized");
					mIAB.queryInventoryAsync(mGotInventoryListener);
				}
			}
		});
		
        mView = new JNIView(getApplication());
		mView.sActivity = this;
		setContentView(mView);
		String internaPath = getFilesDir().getAbsolutePath();
		File externalDir = getExternalFilesDir(null);
		String externalPath = (externalDir != null) ? externalDir.getAbsolutePath() : new String("");		
		JNILib.init(this, getAssets(), internaPath, externalPath);
				
		SGD = new ScaleGestureDetector(this, new ScaleListener());
    }
	
	// Listener that's called when we finish querying the items and subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener()
	{
        public void onQueryInventoryFinished(IabResult result, Inventory inventory)
		{
            Log.d("VLAD", "Query inventory finished.");            
            if (mIAB == null)
			{
				Log.e("VLAD", "In-app Billing helper was disposed of unexpectedly");
				return;
			}
            
            if (result.isFailure())
			{
                Log.e("VLAD", "Inventory query failed: " + result);
				if(mIAB != null)
				{
					mIAB.dispose();
					mIAB = null;
				}
                return;
            }

            Log.d("VLAD", "Query inventory was successful.");
            Purchase fpsMode = inventory.getPurchase("com.frantic.starports.extramode1");
            m_HasFPSMode = (fpsMode != null);
            Log.d("VLAD", "m_HasFPSMode: " + m_HasFPSMode);	
        }
    };
	
	// Callback for when a purchase is finished
	IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener()
	{
		public void onIabPurchaseFinished(IabResult result, Purchase purchase)
		{
			Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);
				
			if(mIAB == null)
			{
				Log.e("VLAD", "In-app Billing helper was disposed of unexpectedly");
				return;
			}
	
			if(result.isFailure())
			{
				m_IABError = "" + result;
				Log.e("VLAD", m_IABError);
				
				// no need to show a user dialog, google play already displays its own dialog
				//JNIActivity.this.runOnUiThread(new Runnable()
				//{
					//public void run() 
					//{
						//AlertDialog.Builder builder1 = new AlertDialog.Builder(JNIActivity.this);
						//builder1.setMessage(m_IABError);
						//builder1.setCancelable(true);
						//builder1.setPositiveButton("OK", 
						//new DialogInterface.OnClickListener() { public void onClick(DialogInterface dialog, int id) { dialog.cancel(); } });
						//AlertDialog alert11 = builder1.create();
						//alert11.show();
					//}
				//});
				
				return;
			}	
	
			Log.d(TAG, "Purchase successful.");
			boolean bFound = purchase.getSku().equals(mProductToPurchase);
			Log.d(TAG, "mProductToPurchase found: " + bFound);
			m_HasFPSMode = true;
		}
	};
	
	String mProductToPurchase = "";
	public void purchase(String productID)
	{
		if(mIAB != null)
		{
			mProductToPurchase = productID;
			mIAB.flagEndAsync();
			mIAB.launchPurchaseFlow(this, productID, 10001, mPurchaseFinishedListener, "");
		}
		else
		{
			JNIActivity.this.runOnUiThread(new Runnable()
				{
					public void run() 
					{
						AlertDialog.Builder builder1 = new AlertDialog.Builder(JNIActivity.this);
						builder1.setMessage("Couldn't contact server.");
						builder1.setCancelable(true);
						builder1.setPositiveButton("OK", 
						new DialogInterface.OnClickListener() { public void onClick(DialogInterface dialog, int id) { dialog.cancel(); } });
						AlertDialog alert11 = builder1.create();
						alert11.show();
					}
				});
		}		
	}
	
	@Override protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);

		if(mIAB != null && mIAB.handleActivityResult(requestCode, resultCode, data))
		{
			Log.d(TAG, "onActivityResult handled by IabHelper.");
			return;
		}
		
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	@Override protected void onStop()
	{
		JNILib.stop();
        super.onStop();
    }
	
	@Override protected void onDestroy()
	{
		if(mIAB != null)
		{
			mIAB.dispose();
			mIAB = null;
		}
		JNILib.destroy();
		super.onDestroy();
	}
	
    @Override protected void onPause()
	{
		JNILib.pause();
        super.onPause();
        mView.onPause();
    }

    @Override protected void onResume()
	{
		JNILib.resume();
        super.onResume();
        mView.onResume();
    }
	
	public boolean m_Pinch = false;
	public boolean m_ZoomIn = false;	
	public long m_TouchTime;
	private boolean m_TouchTimeInitialized = false;
	public boolean onTouchEvent(MotionEvent event)
	{		
		SGD.onTouchEvent(event);
		if(m_Pinch)
		{
			return super.onTouchEvent(event);
		}
		
		int action = event.getAction();
		int pointerIndex = (action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
		if(pointerIndex == 0)
		{			
			int actionType = action & MotionEvent.ACTION_MASK;
			boolean bPressed = (actionType == MotionEvent.ACTION_DOWN || actionType == MotionEvent.ACTION_MOVE);
			float x = event.getX(pointerIndex);
			float y = event.getY(pointerIndex);
			JNILib.setTouchState(x, y, bPressed);
			return true;
		}
		return super.onTouchEvent(event);
	}

	private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener 
	{		
		@Override public boolean onScale(ScaleGestureDetector detector) 
		{
			float scale = detector.getScaleFactor();
			m_ZoomIn = scale < 1.0f;
			m_Pinch = true;
			m_TouchTime = System.currentTimeMillis();
			return true;
		}
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if(event.getKeyCode() == KeyEvent.KEYCODE_BACK)
		{
			JNILib.setBackPressed(true);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		if(event.getKeyCode() == KeyEvent.KEYCODE_BACK)
		{
			JNILib.setBackPressed(false);
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}
}
