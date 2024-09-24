//
//  RNShare.m
//  MyNfcApp2
//
//  Created by Priska Kohnen on 05.09.24.
//

#import <Foundation/Foundation.h>
#import <React/RCTBridgeModule.h>
#import "ReadPersonalDataOptions.h"
#import <React/RCTConvert.h>

@interface RCT_EXTERN_MODULE(RNNFCLoginController, NSObject)

RCT_EXTERN_METHOD(readPersonalData:(ReadPersonalDataOptions *)readPersonalDataOptions
                  resolve:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(getPState:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(getResults)

@end
