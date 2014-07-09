package org.apache.cordova.engine.crosswalk;

import java.util.Hashtable;

import org.apache.cordova.AuthenticationToken;
import org.apache.cordova.CordovaUriHelper;
import org.apache.cordova.LOG;
import org.xwalk.core.XWalkClient;
import org.xwalk.core.XWalkHttpAuthHandler;
import org.xwalk.core.XWalkView;

import android.annotation.TargetApi;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.http.SslError;
import android.view.View;
import android.webkit.ValueCallback;

// TODO(yongsheng): remove the dependency of Crosswalk internal class?
public class XWalkCordovaClient extends XWalkClient {
    private final XWalkCordovaWebView appView;
    private CordovaUriHelper helper;

    /** The authorization tokens. */
    private Hashtable<String, AuthenticationToken> authenticationTokens = new Hashtable<String, AuthenticationToken>();

    public XWalkCordovaClient(XWalkCordovaWebView webView) {
       super(webView.getView());
       appView = webView;
       helper = new CordovaUriHelper(appView.cordova, appView);
    }
 
	@Override
    public boolean shouldOverrideUrlLoading(XWalkView view, String url) {
        return helper.shouldOverrideUrlLoading(url);
    }
    
    /**
     * Clear all authentication tokens.
     */
    public void clearAuthenticationTokens() {
        this.authenticationTokens.clear();
    }

    /**
      * Sets the authentication token.
      *
      * @param authenticationToken
      * @param host
      * @param realm
      */
     public void setAuthenticationToken(AuthenticationToken authenticationToken, String host, String realm) {
         if (host == null) {
             host = "";
         }
         if (realm == null) {
             realm = "";
         }
         this.authenticationTokens.put(host.concat(realm), authenticationToken);
     }

     /**
      * Removes the authentication token.
      *
      * @param host
      * @param realm
      *
      * @return the authentication token or null if did not exist
      */
     public AuthenticationToken removeAuthenticationToken(String host, String realm) {
         return this.authenticationTokens.remove(host.concat(realm));
     }

     /**
      * Gets the authentication token.
      *
      * In order it tries:
      * 1- host + realm
      * 2- host
      * 3- realm
      * 4- no host, no realm
      *
      * @param host
      * @param realm
      *
      * @return the authentication token
      */
     public AuthenticationToken getAuthenticationToken(String host, String realm) {
         AuthenticationToken token = null;
         token = this.authenticationTokens.get(host.concat(realm));

         if (token == null) {
             // try with just the host
             token = this.authenticationTokens.get(host);

             // Try the realm
             if (token == null) {
                 token = this.authenticationTokens.get(realm);
             }

             // if no host found, just query for default
             if (token == null) {
                 token = this.authenticationTokens.get("");
             }
         }

         return token;
     }

    /**
     * On received http auth request.
     * The method reacts on all registered authentication tokens. There is one and only one authentication token for any host + realm combination
     *
     * @param view
     * @param handler
     * @param host
     * @param realm
     */
    @Override
    public void onReceivedHttpAuthRequest(XWalkView view, XWalkHttpAuthHandler handler, String host, String realm) {

        // Get the authentication token
        AuthenticationToken token = getAuthenticationToken(host, realm);
        if (token != null) {
            handler.proceed(token.getUserName(), token.getPassword());
        }
        else {
            // Handle 401 like we'd normally do!
            super.onReceivedHttpAuthRequest(view, handler, host, realm);
        }
    }

    /**
     * Notify the host application that a page has started loading.
     * This method is called once for each main frame load so a page with iframes or framesets will call onPageStarted
     * one time for the main frame.
     *
     * In Crosswalk, this method is called for iframe navigations where the scheme is something other than http or https,
     * which includes assets with the Cordova project, so we have to test for that, and not reset plugins in that case.
     *
     * @param view          The webview initiating the callback.
     * @param url           The url of the page.
     */
    @Override
    public void onPageStarted(XWalkView view, String url) {

        // Only proceed if this is a top-level navigation
        if (view.getUrl() != null && view.getUrl().equals(url)) {
            // Flush stale messages.
            appView.onPageReset();
            // Broadcast message that page has loaded
            appView.getPluginManager().postMessage("onPageStarted", url);
        }
    }

    /**
     * Notify the host application that a page has finished loading.
     * This method is called only for main frame. When onPageFinished() is called, the rendering picture may not be updated yet.
     *
     *
     * @param view          The webview initiating the callback.
     * @param url           The url of the page.
     */
    @Override
    public void onPageFinished(XWalkView view, String url) {
        super.onPageFinished(view, url);
        LOG.d(XWalkCordovaResourceClient.TAG, "onPageFinished(" + url + ")");
        // Clear timeout flag
        appView.loadUrlTimeout++;

        // Broadcast message that page has loaded
        appView.getPluginManager().postMessage("onPageFinished", url);

        // Make app visible after 2 sec in case there was a JS error and Cordova JS never initialized correctly
        if (appView.getView().getVisibility() == View.INVISIBLE) {
            Thread t = new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(2000);
                        appView.cordova.getActivity().runOnUiThread(new Runnable() {
                            public void run() {
                                appView.getPluginManager().postMessage("spinner", "stop");
                            }
                        });
                    } catch (InterruptedException e) {
                    }
                }
            });
            t.start();
        }

        // Shutdown if blank loaded
        if (url.equals("about:blank")) {
            appView.getPluginManager().postMessage("exit", null);
        }
    }

    /**
     * Notify the host application that an SSL error occurred while loading a resource.
     * The host application must call either handler.cancel() or handler.proceed().
     * Note that the decision may be retained for use in response to future SSL errors.
     * The default behavior is to cancel the load.
     *
     * @param view          The WebView that is initiating the callback.
     * @param handler       An SslErrorHandler object that will handle the user's response.
     * @param error         The SSL error object.
     */
    @TargetApi(8)
    @Override
    public void onReceivedSslError(XWalkView view, ValueCallback<Boolean> callback, SslError error) {

        final String packageName = appView.cordova.getActivity().getPackageName();
        final PackageManager pm = appView.cordova.getActivity().getPackageManager();

        ApplicationInfo appInfo;
        try {
            appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            if ((appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                // debug = true
                callback.onReceiveValue(true);
                return;
            } else {
                // debug = false
                super.onReceivedSslError(view, callback, error);
            }
        } catch (NameNotFoundException e) {
            // When it doubt, lock it out!
            super.onReceivedSslError(view, callback, error);
        }
    }
}
