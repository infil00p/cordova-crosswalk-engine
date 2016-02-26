// Copyright (c) 2015 Intel Corporation. All rights reserved.
// Use of this source code is governed by a Apache V2.0 license that can be
// found in the LICENSE file.

#import <WebKit/WebKit.h>

@class XWalkWebViewEngine;

@interface XWalkNavigationDelegate : NSObject <WKNavigationDelegate>

- (id)initWithWebViewEngine:(XWalkWebViewEngine*)engine;

@end
