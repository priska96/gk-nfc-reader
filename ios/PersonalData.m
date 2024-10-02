//
//  PersonalData.m
//
//  Created by Priska Kohnen on 02.10.24.
//

#import "PersonalData.h"

@implementation PersonalData

- (instancetype)initWithDictionary:(NSDictionary *)dictionary {
    self = [super init];
    if (self) {
        _lastname = dictionary[@"Nachname"];
      _firstname = dictionary[@"Vorname"];
      _birthday = dictionary[@"Geburtsdatum"];
      _gender = dictionary[@"Geschlecht"];
      _street = dictionary[@"Strasse"];
      _housenumber = dictionary[@"Hausnummer"];
      _zipCode = dictionary[@"Postleitzahl"];
      _city = dictionary[@"Ort"];
      _countryCode = dictionary[@"Wohnsitzlaendercode"];
      _insuranceId = dictionary[@"Versicherten_ID"];
    }
    return self;
}

- (NSDictionary *)toDictionary {
    return @{
        @"lastname": self.lastname ?: @"",
        @"firstname": self.firstname ?: @"",
        @"birthday": self.birthday ?: @"",
        @"gender": self.gender ?: @"",
        @"street": self.street ?: @"",
        @"housenumber": self.housenumber ?: @"",
        @"zipCode": self.zipCode ?: @"",
        @"city": self.city ?: @"",
        @"countryCode": self.countryCode ?: @"",
        @"insuranceId": self.insuranceId ?: @"",
    };
}

// Method to check if all the properties are empty
- (BOOL)isEmpty {
    return (self.lastname.length == 0 &&
            self.firstname.length == 0 &&
            self.birthday.length == 0 &&
            self.gender.length == 0 &&
            self.street.length == 0 &&
            self.housenumber.length == 0 &&
            self.zipCode.length == 0 &&
            self.city.length == 0 &&
            self.countryCode.length == 0 &&
            self.insuranceId.length == 0);
}
@end
