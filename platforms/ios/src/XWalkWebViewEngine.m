// Copyright (c) 2015 Intel Corporation. All rights reserved.
// Use of this source code is governed by a Apache V2.0 license that can be
// found in the LICENSE file.

#import "XWalkWebViewEngine.h"

#import <Cordova/NSDictionary+CordovaPreferences.h>
#import <XWalkView/XWalkView.h>
#import "XWalkNavigationDelegate.h"
#import "XWalkUIDelegate.h"

@interface XWalkWebViewEngine ()

@property (nonatomic, strong, readwrite) XWalkView* engineWebView;
@property (nonatomic, strong, readwrite) id <WKUIDelegate> uiDelegate;
@property (nonatomic, strong, readwrite) id <WKNavigationDelegate> navigationDelegate;

@end

@implementation XWalkWebViewEngine

@synthesize engineWebView;

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super init]) {
        self.engineWebView = [[XWalkView alloc] initWithFrame:frame];
        NSLog(@"Using XWalkView");
    }
    return self;
}

- (void)pluginInitialize {
    self.uiDelegate = [[XWalkUIDelegate alloc] initWithEngine:self];
    [self.engineWebView setUIDelegate:self.uiDelegate];
    self.navigationDelegate = [[XWalkNavigationDelegate alloc] initWithWebViewEngine:self];
    [self.engineWebView setNavigationDelegate:self.navigationDelegate];
    [self updateSettings:self.commandDelegate.settings];
}

- (void)updateSettings:(NSDictionary*)settings {
    BOOL bounceAllowed = !([settings cordovaBoolSettingForKey:@"DisallowOverscroll" defaultValue:NO]);
    if (!bounceAllowed) {
        self.engineWebView.scrollView.bounces = NO;
    }

    NSString* decelerationSetting = [settings cordovaSettingForKey:@"UIWebViewDecelerationSpeed"];
    if (![@"fast" isEqualToString:decelerationSetting]) {
        [self.engineWebView.scrollView setDecelerationRate:UIScrollViewDecelerationRateNormal];
    }
}

- (id)loadRequest:(NSURLRequest*)request {
    return [self.engineWebView loadRequest:request];
}

- (id)loadHTMLString:(NSString*)string baseURL:(NSURL*)baseURL {
    return [self.engineWebView loadHTMLString:string baseURL:baseURL];
}

- (void)evaluateJavaScript:(NSString*)javaScriptString completionHandler:(void (^)(id, NSError*))completionHandler {
    [self.engineWebView evaluateJavaScript:javaScriptString completionHandler:completionHandler];
}

- (NSURL*)URL {
    return self.engineWebView.URL;
}

- (BOOL)canLoadRequest:(NSURLRequest*)request {
    return request != nil;
}

- (void)updateWithInfo:(NSDictionary*)info {
    NSDictionary* settings = [info objectForKey:kCDVWebViewEngineWebViewPreferences];
    if (settings && [settings isKindOfClass:[NSDictionary class]]) {
        [self updateSettings:settings];
    }
}

@end
