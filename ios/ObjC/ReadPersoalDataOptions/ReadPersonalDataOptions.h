//
//  ReadPersonalDataOptions.h
//
//  Created by Priska Kohnen on 25.09.24.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface ReadPersonalDataOptions : NSObject

@property (nonatomic, strong) NSString *can;                    // nonnull
@property (nonatomic, strong, nullable) NSString *pin;          // nullable
@property (nonatomic, assign) BOOL checkBrainpoolAlgorithm;

- (instancetype)initWithDictionary:(NSDictionary<NSString *, id> *)dictionary;
- (instancetype)initWithCan:(NSString *)can
                         pin:(nullable NSString *)pin
  checkBrainpoolAlgorithm:(BOOL)checkBrainpoolAlgorithm;

- (NSDictionary<NSString *, id> *)toDictionary;

@end

NS_ASSUME_NONNULL_END
