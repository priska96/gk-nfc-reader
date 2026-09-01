package expo.modules.ehcnfc.helpers

import java.security.SecureRandom

//// A_19179
//@Requirement(
//    "O.Rand_1#1",
//    sourceSpecification = "BSI-eRp-ePA",
//    rationale = "Generation of random values by secure random generator specified in FIPS 140-2, " +
//            "Security Requirements for Cryptographic Modules, section 4.9.1."
//)
fun secureRandomInstance(): SecureRandom =
    SecureRandom.getInstanceStrong()
