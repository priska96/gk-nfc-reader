//
//  RCTConvert+ReadPersonalDataOptions.m
//
//  Created by Priska Kohnen on 25.09.24.
//

#import "RCTConvert+ReadPersonalDataOptions.h"

@implementation RCTConvert (ReadPersonalDataOptions)

+ (ReadPersonalDataOptions *)ReadPersonalDataOptions:(id)json {
    if (![json isKindOfClass:[NSDictionary class]]) {
        return nil;
    }
    return [[ReadPersonalDataOptions alloc] initWithDictionary:json];
}

@end
