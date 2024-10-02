//
//  ReadPersonalDataOptions.h
//
//  Created by Priska Kohnen on 25.09.24.
//

#import <Foundation/Foundation.h>

@interface ReadPersonalDataOptions : NSObject

@property (nonatomic, strong) NSString *can;
@property (nonatomic, strong, nullable) NSString *pin;
@property (nonatomic, assign) BOOL checkBrainpoolAlgorithm;


- (instancetype)initWithDictionary:(NSDictionary *)dictionary;
- (instancetype)initWithCan:(NSString *)can pin:(nullable NSString *)pin checkBrainpoolAlgorithm:(BOOL)checkBrainpoolAlgorithm;
- (NSDictionary *)toDictionary;

@end
