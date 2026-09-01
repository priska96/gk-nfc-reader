//
//  expoehcnfcexampleTests.swift
//  expoehcnfcexampleTests
//
//  Created by Priska Kohnen on 01.09.25.
//

@testable import HealthCardAccess
@testable import HealthCardControl
@testable import CardReaderProviderApi
import CoreNFC
@testable import ASN1Kit

import XCTest

extension Data {
    var hexString: String {
        self.map { String(format: "%02X", $0) }.joined()
    }
    func addPadding(_ delimiter: UInt8 = AES128PaceKey.paddingDelimiter,
                                blockSize: Int = AES128PaceKey.blockSize) -> Data {
        var padded = self + Data(repeating: 0x0, count: blockSize - count % blockSize)
        assert(padded.count % blockSize == 0)
        assert(padded.count >= count)
        padded[count] = delimiter
        return padded
    }
}


func calculateMac(key: Data, ssc: Data, macIn: Data) throws -> Data {
    let sscNormalized = ssc.normalize(to: AES128PaceKey.blockSize, paddingIndicator: 0x0)
    let macInPadded = macIn.addPadding()
    let cmac = try AES.CMAC(key: key, data: sscNormalized + macInPadded)
    return cmac.prefix(8)
}



// Build a minimal CommandType implementation inline
struct RawCommand: CommandType {
    let data: Data?
    let ne: Int?
    let nc: Int
    let cla: UInt8
    let ins: UInt8
    let p1: UInt8
    let p2: UInt8

    var bytes: Data {
        var b = Data([cla, ins, p1, p2])
        if let data = data { b.append(data) }
        return b
    }
}

struct RawResponse: ResponseType {
    let data: Data?
    let sw1: UInt8
    let sw2: UInt8

    var nr: Int { data?.count ?? 0 }

    var sw: UInt16 {
        // combine sw1 and sw2 into a UInt16
        return (UInt16(sw1) << 8) | UInt16(sw2)
    }
}

final class ExpoEhcNfcExampleTests: XCTestCase {
    
    var testData: TestData!

      override func setUpWithError() throws {
          try super.setUpWithError()
          testData = try loadTestData()
      }

      override func tearDownWithError() throws {
          testData = nil
          try super.tearDownWithError()
      }
    
    func loadTestData() throws -> TestData  {
        let bundle = Bundle(for: type(of: self)) // self = XCTestCase subclass
        guard let url = bundle.url(forResource: "test_data", withExtension: "json") else {
            fatalError("test_data.json not found in bundle")
        }
        let data = try! Data(contentsOf: url)
        return try! JSONDecoder().decode(TestData.self, from: data)
    }
    

    // PACE key setup
    let paceKey = AES128PaceKey(
        enc: "83D8079E433618FC90AFB79624112671".hexData,
        mac: "100DD487AA008B5DB00103025F20525F".hexData
    )

    
    
    @MainActor
    func testGetDataSelectForDFMACObject() throws {
        
        XCTAssertNotNil(testData)
        // PACE key setup
        let paceKey = AES128PaceKey(
            enc: testData.paceKey.enc.hexData,
            mac: testData.paceKey.mac.hexData
        )
        
        var ssc = Data(count: 16)
        ssc = Data(repeating: 0x0, count: ssc.count)
        ssc = AES128PaceKey.incrementSsc(ssc: ssc)
        
        
        
        let dedicatedFile = DedicatedFile(
            aid: EgkFileSystem.DF.HCA.aid,
            fid: EgkFileSystem.EF.hcaPD.fid // holds Personal Data of health card holder
        )
        let command = HealthCardCommand.Select.selectFile(with: dedicatedFile.aid)
        
        var header1 = Data([command.cla, command.ins, command.p1, command.p2])

        let dataObject: Data
        if let data = command.data {
            // gemSpec_COS#N032.300
            // use CBC mode here instead of specified ECB mode, since only one block has to be encrypted
            let initVector = try AES.CBC128.encrypt(data: ssc, key: "CE366D71D740E8CCBDA976B9A3B21CC9".hexData)
            let paddedData = data.addPadding()
            let paddedDataEncrypted = try AES.CBC128.encrypt(data: paddedData, key: "CE366D71D740E8CCBDA976B9A3B21CC9".hexData, initVector: initVector)
            dataObject = try create(tag: .taggedTag(7), data: .primitive([0x1] + paddedDataEncrypted)).serialize()
        } else {
            dataObject = Data()
        }
        
        let lengthObject: Data
        if let le = command.ne { // swiftlint:disable:this identifier_name
            let leValue: Data
            if le == 0x100 {
                leValue = Data([0x0])
            } else if le == 0x10000 {
                leValue = Data([0x0, 0x0])
            } else if le > 0x100 {
                leValue = Data([UInt8(le >> 8) & 0xFF, UInt8(le & 0xFF)])
            } else {
                leValue = Data([UInt8(le & 0xFF)])
            }
            lengthObject = try create(tag: .taggedTag(0x17), data: .primitive(leValue)).serialize()
        } else {
            lengthObject = Data()
        }
        // [REQ:gemSpec_COS:N032.500] Indicate Secure Messaging (Caution: we assume CLA in [0,3]!)
        header1[0] |= 0x0C

        let tmpData1 = dataObject + lengthObject
        
        // Print encrypted APDU
        print("Actual header1: \(header1.hexString)")
        print("Actual dataObject: \(dataObject.hexString)")
        print("Actual lengthObject: \(lengthObject.hexString)")
        print("Actual tmpData1: \(tmpData1.hexString)")
        
        
        // [REQ:gemSpec_COS:N032.800] Calculate MAC
        let calculatedMac: Data
        let header = "00A4040C".hexData
        let tmpData = "8711014B68176BBDD160A97D976F257834F3AB".hexData
        if tmpData.isEmpty {
            calculatedMac = try calculateMac(key: paceKey.mac, ssc: ssc, macIn: header)
        } else {
            let padHeader = header.addPadding()
            calculatedMac = try calculateMac(key: paceKey.mac, ssc: ssc, macIn: padHeader + tmpData)
        }
        
        let mDo = try create(tag: .taggedTag(0xE), data: .primitive(calculatedMac)).serialize()

        
        // [REQ:gemSpec_COS:N033.000]
        let newD = tmpData + mDo
        
        let setLe: Int
        // [REQ:gemSpec_COS:N033.100,N033.200,N033.300,N033.400]
        switch (command.data, command.ne) {
        case (nil, nil): setLe = APDU.expectedLengthWildcardShort
        case (nil, _): setLe = APDU.expectedLengthWildcardExtended
        case (_, nil):
            if newD.count <= 255 {
                setLe = APDU.expectedLengthWildcardShort
            } else {
                setLe = APDU.expectedLengthWildcardExtended
            }
        case (_, _): setLe = APDU.expectedLengthWildcardExtended
        }

       var encryptedAdpu = try APDU.Command(cla: header[0], ins: header[1], p1: header[2], p2: header[3], data: newD, ne: setLe)
        
        // Print encrypted APDU
        print("Expected MacData: \(mDo.hexString)")
        print("Expected encryptedAdpu Data: \(encryptedAdpu.bytes.hexString)")
        
        let myMacData = "8E08D1A170BD3B4315C9".hexData
        print("Actual MacData: \(myMacData.hexString)")
        
        XCTAssertEqual(myMacData, mDo)
    }
    
    
    @MainActor
    func testEncryptSelectCommandWithHealthCardCommand() throws {
        // PACE key setup
        let paceKey = AES128PaceKey(
            enc: "CE366D71D740E8CCBDA976B9A3B21CC9".hexData,
            mac: "3D3BB10D2CCAABA2C4125CD932582ADC".hexData
        )

        
        let dedicatedFile = DedicatedFile(
            aid: EgkFileSystem.DF.HCA.aid,
            fid: EgkFileSystem.EF.hcaPD.fid // holds Personal Data of health card holder
        )
        let healthCardCommand = HealthCardCommand.Select.selectFile(with: dedicatedFile.aid)

        // Encrypt using AES128PaceKey
        let encrypted = try paceKey.encrypt(command: healthCardCommand)

        // Print encrypted APDU
        print("Encrypted APDU: \(encrypted.bytes.hexString)")

        // Example assertion (replace with your expected value)
        let expectedHex = "0CA4040C8711014B68176BBDD160A97D976F257834F3AB8E0822E1433056DBF06C"
        XCTAssertEqual(encrypted.bytes.hexString.uppercased(), expectedHex)
        }
 
    @MainActor
    func testEncryptDecryptSelectCommandWithHealthCardCommand() throws {

        // Raw APDU command bytes
        let apduData = "D27600000102".hexData


        let rawCommand = RawCommand(
            data: apduData,
            ne: nil,
            nc: 6,
            cla: 0x00,
            ins: 0xA4,
            p1: 0x04,
            p2: 0x0C
        )

        // Wrap it in HealthCardCommand
        let healthCommand = HealthCardCommand(
            apduCommand: rawCommand,
            responseStatuses: [0x9000: .success]
        )


            // Encrypt using AES128PaceKey
            let encrypted = try paceKey.encrypt(command: healthCommand)

            // Print encrypted APDU
            print("Encrypted APDU: \(encrypted.bytes.hexString)")

            let response = RawResponse(
                data: "990290008E08D0E7169F63A6A429".hexData,
                sw1: 0x90,
                sw2: 0x00
            )
        
        
            // Encrypt using AES128PaceKey
            let decrypted = try paceKey.decrypt(response: response)
        
        
        // Print encrypted APDU
        print("Decrypted APDU response: \(decrypted.data?.hexString ?? "null")")

        // Example assertion (replace with your expected value)
        let expectedHex = "9000"
        
        let expectedResponse = RawResponse(
            data:nil,
            sw1: 0x90,
            sw2: 0x00
        )
        XCTAssertEqual(decrypted.data, expectedResponse.data)
        XCTAssertEqual(decrypted.sw, expectedResponse.sw)
        XCTAssertEqual(decrypted.sw, expectedResponse.sw)
        
        }
    
    
    @MainActor
    func testEncryptSelectEFByFIDCommandWithHealthCardCommand() throws {

            // Raw APDU command bytes
            let apduData = "D00100".hexData


            let rawCommand = RawCommand(
                data: apduData,
                ne: 256,
                nc: 2,
                cla: 0x00,
                ins: 0xA4,
                p1: 0x02,
                p2: 0x04
            )

            // Wrap it in HealthCardCommand
            let healthCommand = HealthCardCommand(
                apduCommand: rawCommand,
                responseStatuses: [0x9000: .success]
            )

            // Encrypt using AES128PaceKey
            let encrypted = try paceKey.encrypt(command: healthCommand)

            // Print encrypted APDU
            print("Encrypted APDU: \(encrypted.bytes.hexString)")

            // Example assertion (replace with your expected value)
            let expectedHex = "0CA40204000020871101C890340620AA084307EF4006EBD5843A9701008E088A37696412C8E62E0000"
            XCTAssertEqual(encrypted.bytes.hexString.uppercased(), expectedHex)
        }
    
    @MainActor
    func testBuildSelectEFByFIDCommandWithHealthCardCommand() throws {
        let dedicatedFile = DedicatedFile(
            aid: EgkFileSystem.DF.HCA.aid,
            fid: EgkFileSystem.EF.hcaPD.fid // holds Personal Data of health card holder
        )
        let expectedSelectEFViaFIDCommand = try HealthCardCommand.Select.selectEfRequestingFcp(with: dedicatedFile.fid ?? FileIdentifier("D001"), expectedLength: 256)
        
        // Raw APDU command bytes
        let apduData = "D00100".hexData


        let rawCommand = RawCommand(
            data: apduData,
            ne: 256,
            nc: 2,
            cla: 0x00,
            ins: 0xA4,
            p1: 0x02,
            p2: 0x04
        )

        // Wrap it in HealthCardCommand
        let healthCommand = HealthCardCommand(
            apduCommand: rawCommand,
            responseStatuses: [0x9000: .success]
        )
        
        // Print encrypted APDU
        print("Expected APDU: \(expectedSelectEFViaFIDCommand.bytes.hexString)")

        // Print encrypted APDU
        print("Actual APDU: \(healthCommand.bytes.hexString)")

        XCTAssertEqual(healthCommand.bytes.hexString, expectedSelectEFViaFIDCommand.bytes.hexString)
    }
    
    @MainActor
    func testGetDataForMACObject() throws {
        var ssc = Data(count: 16)
        ssc = Data(repeating: 0x0, count: ssc.count)
        ssc = AES128PaceKey.incrementSsc(ssc: ssc)
        ssc = AES128PaceKey.incrementSsc(ssc: ssc)
        ssc = AES128PaceKey.incrementSsc(ssc: ssc)
        let mac = "871762158302D0018A010582014188010880020352C50201A199029000".hexData
        let macInPadded = mac.addPadding()
        let sscNormalized = ssc.normalize(to: AES128PaceKey.blockSize, paddingIndicator: 0x0)
        let cmac = try AES.CMAC(key: paceKey.mac, data: sscNormalized + macInPadded)
        let expectedMacData = cmac.prefix(8)
        
        // Print encrypted APDU
        print("Expected MacData: \(expectedMacData.hexString)")
        
        let myMacData = "E49E8710899B1DA7".hexData
        print("Actual MacData: \(myMacData.hexString)")
        
        XCTAssertEqual(myMacData, expectedMacData)
    }
    
    @MainActor
    func testEncryptDecryptSelectEFByFIDCommandWithHealthCardCommand() throws {

            // Raw APDU command bytes
            let apduData = "D00100".hexData


            let rawCommand = RawCommand(
                data: apduData,
                ne: 256,
                nc: 2,
                cla: 0x00,
                ins: 0xA4,
                p1: 0x02,
                p2: 0x04
            )

            // Wrap it in HealthCardCommand
            let healthCommand = HealthCardCommand(
                apduCommand: rawCommand,
                responseStatuses: [0x9000: .success]
            )

            // Encrypt using AES128PaceKey
            let encrypted = try paceKey.encrypt(command: healthCommand)

            // Print encrypted APDU
            print("Encrypted APDU: \(encrypted.bytes.hexString)")
        
        let response = RawResponse(
            data: "811762158302D0018A010582014188010880020352C50201A1990290008E08232F59F58BAFF22F".hexData,
            sw1: 0x90,
            sw2: 0x00
        )
    
    
        // Encrypt using AES128PaceKey
        let decrypted = try paceKey.decrypt(response: response)
    
    
    // Print encrypted APDU
    print("Decrypted APDU response: \(decrypted.data?.hexString)")

    // Example assertion (replace with your expected value)
    let expectedHex = "9000"
    XCTAssertEqual(decrypted.data?.hexString.uppercased(), expectedHex)
        }

}
