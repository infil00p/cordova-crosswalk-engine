package org.apache.cordova.engine.crosswalk;

import org.apache.cordova.CordovaWebView;
import org.xwalk.core.XWalkWebChromeClient;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

public class XWalkCordovaWebChromeClient extends XWalkWebChromeClient {
    private View mVideoProgressView;
    private CordovaWebView appView;
    
    public XWalkCordovaWebChromeClient(XWalkCordovaWebView view) {
        super(view.getView());
        appView = view;
    }
    
    /**
     * Ask the host application for a custom progress view to show while
     * a <video> is loading.
     * @return View The progress view.
     */
    @Override
    public View getVideoLoadingProgressView() {
    
        if (mVideoProgressView == null) {	        
        	// Create a new Loading view programmatically.
        	
        	// create the linear layout
        	LinearLayout layout = new LinearLayout(appView.getView().getContext());
            layout.setOrientation(LinearLayout.VERTICAL);
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
            layout.setLayoutParams(layoutParams);
            // the proress bar
            ProgressBar bar = new ProgressBar(appView.getView().getContext());
            LinearLayout.LayoutParams barLayoutParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            barLayoutParams.gravity = Gravity.CENTER;
            bar.setLayoutParams(barLayoutParams);   
            layout.addView(bar);
            
            mVideoProgressView = layout;
        }
        return mVideoProgressView; 
    }
}