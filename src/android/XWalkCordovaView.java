package org.crosswalk.engine;

import org.xwalk.core.XWalkPreferences;
import org.xwalk.core.XWalkResourceClient;
import org.xwalk.core.XWalkUIClient;
import org.xwalk.core.XWalkView;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class XWalkCordovaView extends XWalkView {
    protected XWalkCordovaResourceClient resourceClient;
    protected XWalkCordovaUiClient uiClient;

    protected XWalkCordovaWebView cordovaWebView;
    private long lastMenuEventTime = 0;

    private static boolean hasSetStaticPref;
    // This needs to run before the super's constructor.
    private static Context setGlobalPrefs(Context context) {
        if (!hasSetStaticPref) {
            hasSetStaticPref = true;
            ApplicationInfo ai = null;
            try {
                ai = context.getPackageManager().getApplicationInfo(context.getApplicationContext().getPackageName(), PackageManager.GET_META_DATA);
            } catch (PackageManager.NameNotFoundException e) {
                throw new RuntimeException(e);
            }
            if (ai.metaData.getBoolean("CrosswalkAnimatable")) {
                // Slows it down a bit, but allows for it to be animated by Android View properties.
                XWalkPreferences.setValue(XWalkPreferences.ANIMATABLE_XWALK_VIEW, true);
            }
            if ((ai.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                XWalkPreferences.setValue(XWalkPreferences.REMOTE_DEBUGGING, true);
            }
            XWalkPreferences.setValue(XWalkPreferences.JAVASCRIPT_CAN_OPEN_WINDOW, true);
            XWalkPreferences.setValue(XWalkPreferences.ALLOW_UNIVERSAL_ACCESS_FROM_FILE, true);
        }
        return context;
    }

    public XWalkCordovaView(Context context, AttributeSet attrs) {
        super(setGlobalPrefs(context), attrs);
    }

    void init(XWalkCordovaWebView webView) {
        cordovaWebView = webView;
        if (resourceClient == null) {
            setResourceClient(new XWalkCordovaResourceClient(webView));
        }
        if (uiClient == null) {
            setUIClient(new XWalkCordovaUiClient(webView));
        }
    }

    @Override
    public void setResourceClient(XWalkResourceClient client) {
        // XWalk calls this method from its constructor.
        if (client instanceof XWalkCordovaResourceClient) {
            this.resourceClient = (XWalkCordovaResourceClient)client;
        }
        super.setResourceClient(client);
    }

    @Override
    public void setUIClient(XWalkUIClient client) {
        // XWalk calls this method from its constructor.
        if (client instanceof XWalkCordovaUiClient) {
            this.uiClient = (XWalkCordovaUiClient)client;
        }
        super.setUIClient(client);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if(cordovaWebView.boundKeyCodes.contains(keyCode))
            {
                if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                        evaluateJavascript("cordova.fireDocumentEvent('volumedownbutton');", null);
                        return true;
                }
                // If volumeup key
                else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                        evaluateJavascript("cordova.fireDocumentEvent('volumeupbutton');", null);
                        return true;
                }
            }
            else if(keyCode == KeyEvent.KEYCODE_BACK)
            {
                return !(cordovaWebView.startOfHistory()) || cordovaWebView.isButtonPlumbedToJs(KeyEvent.KEYCODE_BACK);
            }
            else if(keyCode == KeyEvent.KEYCODE_MENU)
            {
                //How did we get here?  Is there a childView?
                View childView = (View) this.getFocusedChild();
                if(childView != null)
                {
                    //Make sure we close the keyboard if it's present
                    InputMethodManager imm = (InputMethodManager) cordovaWebView.cordova.getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(childView.getWindowToken(), 0);
                    cordovaWebView.cordova.getActivity().openOptionsMenu();
                    return true;
                }
            }
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                // A custom view is currently displayed  (e.g. playing a video)
                if (hasEnteredFullscreen()) {
                    leaveFullscreen();
                    return true;
                } else if (cordovaWebView.isCustomViewShowing()) {
                    cordovaWebView.hideCustomView();
                    return true;
                } else {
                    // The webview is currently displayed
                    // If back key is bound, then send event to JavaScript
                    if (cordovaWebView.isButtonPlumbedToJs(KeyEvent.KEYCODE_BACK)) {
                        evaluateJavascript("cordova.fireDocumentEvent('backbutton');", null);
                        return true;
                    } else {
                        // If not bound
                        // Go to previous page in webview if it is possible to go back
                        if (cordovaWebView.backHistory()) {
                            return true;
                        }
                    }
                }
            }
            // Legacy
            else if (keyCode == KeyEvent.KEYCODE_MENU) {
                if (lastMenuEventTime < event.getEventTime()) {
                    evaluateJavascript("cordova.fireDocumentEvent('menubutton');", null);
                }
                lastMenuEventTime = event.getEventTime();
            }
            // If search key
            else if (keyCode == KeyEvent.KEYCODE_SEARCH) {
                evaluateJavascript("cordova.fireDocumentEvent('searchbutton');", null);
                return true;
            }
        }

        return super.dispatchKeyEvent(event);
    }

    @Override
    public void pauseTimers() {
        // This is called by XWalkViewInternal.onActivityStateChange().
        // We don't want them paused by default though.
    }

    public void pauseTimersForReal() {
        super.pauseTimers();
    }
}
