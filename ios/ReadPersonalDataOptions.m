//
//  ReadPersonalDataOptions.m
//
//  Created by Priska Kohnen on 25.09.24.
//

#import "ReadPersonalDataOptions.h"

@implementation ReadPersonalDataOptions

- (instancetype)initWithDictionary:(NSDictionary *)dictionary {
    self = [super init];
    if (self) {
        _can = dictionary[@"can"];
        _pin = dictionary[@"pin"];
        _checkBrainpoolAlgorithm = [dictionary[@"checkBrainpoolAlgorithm"] boolValue];
    }
    return self;
}

- (instancetype)initWithCan:(NSString *)can pin:(nullable NSString *)pin checkBrainpoolAlgorithm:(BOOL)checkBrainpoolAlgorithm {
    self = [super init];
    if (self) {
        _can = can;
        _pin = pin;
        _checkBrainpoolAlgorithm = checkBrainpoolAlgorithm;
    }
    return self;
}

- (NSDictionary *)toDictionary {
    return @{
        @"can": self.can ?: @"",
        @"pin": self.pin ?: @"",
        @"checkBrainpoolAlgorithm": @(self.checkBrainpoolAlgorithm)
    };
}

@end
