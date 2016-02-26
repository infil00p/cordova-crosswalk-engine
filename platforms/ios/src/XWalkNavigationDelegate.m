// Copyright (c) 2015 Intel Corporation. All rights reserved.
// Use of this source code is governed by a Apache V2.0 license that can be
// found in the LICENSE file.

#import "XWalkNavigationDelegate.h"

#import <Cordova/CDVViewController.h>
#import <Cordova/CDVUserAgentUtil.h>
#import <objc/message.h>
#import "XWalkWebViewEngine.h"

@interface XWalkNavigationDelegate ()

@property(nonatomic, weak) XWalkWebViewEngine* engine;

@end

@implementation XWalkNavigationDelegate

- (id)initWithWebViewEngine:(XWalkWebViewEngine*)engine {
    if (self = [super init]) {
        _engine = engine;
    }
    return self;
}

- (void)webView:(WKWebView *)webView didStartProvisionalNavigation:(WKNavigation *)navigation {
    NSLog(@"Resetting plugins due to page load.");
    CDVViewController* controller = (CDVViewController*)self.engine.viewController;

    [controller.commandQueue resetRequestId];
    [[NSNotificationCenter defaultCenter] postNotification:[NSNotification notificationWithName:CDVPluginResetNotification object:webView]];
}

- (void)webView:(WKWebView *)webView didFinishNavigation:(WKNavigation *)navigation {
    NSLog(@"Finished load of: %@", webView.URL);
    CDVViewController* controller = (CDVViewController*)self.engine.viewController;

    // It's safe to release the lock even if this is just a sub-frame that's finished loading.
    [CDVUserAgentUtil releaseLock:controller.userAgentLockToken];

    /*
     * Hide the Top Activity THROBBER in the Battery Bar
     */
    [[UIApplication sharedApplication] setNetworkActivityIndicatorVisible:NO];

    [[NSNotificationCenter defaultCenter] postNotification:[NSNotification notificationWithName:CDVPageDidLoadNotification object:webView]];
}

- (void)webView:(WKWebView *)webView didFailProvisionalNavigation:(WKNavigation *)navigation withError:(NSError *)error {
    [self webView:webView failedNavigation:navigation withError:error];
}

- (void)webView:(WKWebView *)webView didFailNavigation:(WKNavigation *)navigation withError:(NSError *)error {
    [self webView:webView failedNavigation:navigation withError:error];
}

- (void)webView:(WKWebView *)webView failedNavigation:(WKNavigation *)navigation withError:(NSError *)error {
    CDVViewController* controller = (CDVViewController*)self.engine.viewController;

    [CDVUserAgentUtil releaseLock:controller.userAgentLockToken];

    NSString* message = [NSString stringWithFormat:@"Failed to load webpage with error: %@", [error localizedDescription]];
    NSLog(@"%@", message);

    NSURL* errorUrl = controller.errorURL;
    if (errorUrl) {
        errorUrl = [NSURL URLWithString:[NSString stringWithFormat:@"?error=%@", [message stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding]] relativeToURL:errorUrl];
        NSLog(@"%@", [errorUrl absoluteString]);
        [webView loadRequest:[NSURLRequest requestWithURL:errorUrl]];
    }
}

- (void)webView:(WKWebView *)webView decidePolicyForNavigationAction:(WKNavigationAction *)navigationAction decisionHandler:(void (^)(WKNavigationActionPolicy))decisionHandler {
    NSURL* url = navigationAction.request.URL;
    CDVViewController* controller = (CDVViewController*)self.engine.viewController;

    if ([url.scheme isEqualToString:@"gap"]) {
        [controller.commandQueue fetchCommandsFromJs];
        [controller.commandQueue executePending];
        decisionHandler(WKNavigationActionPolicyCancel);
        return;
    }

    /*
     * Give plugins the chance to handle the url
     */
    BOOL anyPluginsResponded = NO;
    BOOL shouldAllowRequest = NO;

    for (NSString* pluginName in controller.pluginObjects) {
        CDVPlugin* plugin = [controller.pluginObjects objectForKey:pluginName];
        SEL selector = NSSelectorFromString(@"shouldOverrideLoadWithRequest:navigationType:");
        if ([plugin respondsToSelector:selector]) {
            anyPluginsResponded = YES;
            shouldAllowRequest = (((BOOL (*)(id, SEL, id, int))objc_msgSend)(plugin, selector, navigationAction.request, navigationAction.navigationType));
            if (!shouldAllowRequest) {
                break;
            }
        }
    }

    if (anyPluginsResponded) {
        decisionHandler(shouldAllowRequest ? WKNavigationActionPolicyAllow : WKNavigationActionPolicyCancel);
        return;
    }

    /*
     * Handle all other types of urls (tel:, sms:), and requests to load a url in the main webview.
     */
    BOOL shouldAllowNavigation = [self defaultResourcePolicyForURL:url];
    if (shouldAllowNavigation) {
        decisionHandler(WKNavigationActionPolicyAllow);
        return;
    }
    [[NSNotificationCenter defaultCenter] postNotification:[NSNotification notificationWithName:CDVPluginHandleOpenURLNotification object:url]];
    decisionHandler(WKNavigationActionPolicyCancel);
}

- (BOOL)defaultResourcePolicyForURL:(NSURL*)url
{
    /*
     * If a URL is being loaded that's a file url, just load it internally
     */
    if ([url isFileURL]) {
        return YES;
    }

    return NO;
}

@end
