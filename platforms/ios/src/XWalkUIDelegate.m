// Copyright (c) 2015 Intel Corporation. All rights reserved.
// Use of this source code is governed by a Apache V2.0 license that can be
// found in the LICENSE file.

#import "XWalkUIDelegate.h"

#import "XWalkWebViewEngine.h"

@interface XWalkUIDelegate ()

@property(nonatomic, weak) XWalkWebViewEngine* engine;

@end

@implementation XWalkUIDelegate

- (id)initWithEngine:(XWalkWebViewEngine*)engine {
    if (self = [super init]) {
        _engine = engine;
    }
    return self;
}

@end
