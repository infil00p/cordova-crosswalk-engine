// Copyright (c) 2015 Intel Corporation. All rights reserved.
// Use of this source code is governed by a Apache V2.0 license that can be
// found in the LICENSE file.

#import <Cordova/CDVPlugin.h>
#import <Cordova/CDVWebViewEngineProtocol.h>
#import <WebKit/WebKit.h>

@interface XWalkWebViewEngine : CDVPlugin <CDVWebViewEngineProtocol>

@property (nonatomic, strong, readonly) id <WKUIDelegate> uiDelegate;
@property (nonatomic, strong, readonly) id <WKNavigationDelegate> navigationDelegate;

@end