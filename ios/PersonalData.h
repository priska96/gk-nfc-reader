//
//  PersonalData.h
//
//  Created by Priska Kohnen on 02.10.24.
//

#import <Foundation/Foundation.h>

@interface PersonalData : NSObject

@property (nonatomic, strong) NSString *lastname;
@property (nonatomic, strong) NSString *firstname;
@property (nonatomic, strong) NSString *birthday;
@property (nonatomic, strong) NSString *gender;
@property (nonatomic, strong) NSString *street;
@property (nonatomic, strong) NSString *housenumber;
@property (nonatomic, strong) NSString *zipCode;
@property (nonatomic, strong) NSString *city;
@property (nonatomic, strong) NSString *countryCode;
@property (nonatomic, strong) NSString *insuranceId;


- (instancetype)initWithDictionary:(NSDictionary *)dictionary;
- (NSDictionary *)toDictionary;
- (BOOL)isEmpty;

@end
