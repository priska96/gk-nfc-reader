//
//  RCTConvert+PersonalData.m
//
//  Created by Priska Kohnen on 02.10.24.
//

#import "RCTConvert+PersonalData.h"

@implementation RCTConvert (PersonalData)

+ (PersonalData *)PersonalData:(id)json {
    if (![json isKindOfClass:[NSDictionary class]]) {
        return nil;
    }
    return [[PersonalData alloc] initWithDictionary:json];
}

@end

