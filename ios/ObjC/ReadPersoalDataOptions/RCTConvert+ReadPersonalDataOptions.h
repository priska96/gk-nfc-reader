//
//  RCTConvert+ReadPersonalDataOptions.h
//
//  Created by Priska Kohnen on 25.09.24.
//

#import <React/RCTConvert.h>
#import "ReadPersonalDataOptions.h"

@interface RCTConvert (ReadPersonalDataOptions)

+ (ReadPersonalDataOptions *)ReadPersonalDataOptions:(id)json;

@end
