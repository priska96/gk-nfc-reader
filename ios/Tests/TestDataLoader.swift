//
//  TestDataLoader.swift
//  Pods
//
//  Created by Priska Kohnen on 03.09.25.
//
import Foundation

struct TestData: Decodable {
    struct PaceKey: Decodable {
        let enc: String
        let mac: String
    }
    struct Apdu: Decodable {
        let expectedCmd: String
        let expectedEncryptedCmd: String
        let expectedRes: String
        let expectedEncryptedRes: String
    }
    struct PersonalData: Decodable {
        let compressed: String
        let decompressed: String
    }
    struct FCP: Decodable {
        let numberOfOctets: Int
        let fileDescriptor: String
        let fileIdentifier: String
        let applicationIdentifier: String?
        let shortFileIdentifier: String?
        let lifeCycleStatus: String
        let readSize: Int
    }
    let paceKey: PaceKey
    let apdus: [String: Apdu]
    let macs: [String: String]
    let personalData: PersonalData
    let fcp: FCP
}

extension String {
    var hexData: Data {
        var data = Data()
        var hex = self
        while hex.count > 0 {
            let c = String(hex.prefix(2))
            hex = String(hex.dropFirst(2))
            if let byte = UInt8(c, radix: 16) {
                data.append(byte)
            }
        }
        return data
    }
}
