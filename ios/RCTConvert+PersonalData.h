//
//  RCTConvert+PersonalData.h
//
//  Created by Priska Kohnen on 02.10.24.
//

#import <React/RCTConvert.h>
#import "PersonalData.h"

@interface RCTConvert (PersonalData)

+ (PersonalData *)PersonalData:(id)json;

@end

