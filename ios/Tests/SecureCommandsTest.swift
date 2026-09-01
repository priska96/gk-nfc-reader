//
//  expoehcnfcexampleTests.swift
//  expoehcnfcexampleTests
//
//  Created by Priska Kohnen on 01.09.25.
//

@testable import HealthCardAccess
@testable import HealthCardControl
@testable import CardReaderProviderApi
@testable import ASN1Kit
import CoreNFC
import XCTest

extension NFCISO7816APDU {
    convenience init(command cmd: CommandType) {
        let realNe: Int
        if let cmdNe = cmd.ne {
            if cmdNe == 0 {
                realNe = 65536
            } else {
                realNe = cmdNe
            }
        } else {
            realNe = -1
        }
        self.init(
            instructionClass: cmd.cla,
            instructionCode: cmd.ins,
            p1Parameter: cmd.p1,
            p2Parameter: cmd.p2,
            data: cmd.data ?? Data(),
            expectedResponseLength: realNe
        )
    }
}


func printLikeExpectedApdu(commandApdu: CommandType) ->String{
    var sw1: UInt8 = 0
    var sw2: UInt8 = 0
    var data = Data()

    let apdu = NFCISO7816APDU(command: commandApdu)
    let sendHeader = Data(
        [apdu.instructionClass] + [apdu.instructionCode] + [apdu.p1Parameter] + [apdu.p2Parameter]
    ).hexString

    let send = "\(sendHeader)\(apdu.data?.hexString ?? "")"
    
    return send
}

final class SecureCommandsTests: XCTestCase {
    
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
    func testBuildSelectDFCommand() throws {
        let dedicatedFile = DedicatedFile(
            aid: EgkFileSystem.DF.HCA.aid,
            fid: EgkFileSystem.EF.hcaPD.fid // holds Personal Data of health card holder
        )
        let selectDF = HealthCardCommand.Select.selectFile(with: dedicatedFile.aid)
        
        let expectedCmd = testData.apdus["selectDf"]?.expectedCmd
        // Print encrypted APDU
        print("Expected APDU: \(expectedCmd)")

        // Print encrypted APDU
        print("Actual APDU: \(selectDF.bytes.hexString)")

        XCTAssertEqual(selectDF.bytes.hexString, expectedCmd)
    }
    
    
    @MainActor
    func testBuildSelectDFCommandEncrypted() throws {
        let dedicatedFile = DedicatedFile(
            aid: EgkFileSystem.DF.HCA.aid,
            fid: EgkFileSystem.EF.hcaPD.fid // holds Personal Data of health card holder
        )
        let selectDF = HealthCardCommand.Select.selectFile(with: dedicatedFile.aid)
        
        // PACE key setup
        let paceKey = AES128PaceKey(
            enc: testData.paceKey.enc.hexData,
            mac: testData.paceKey.mac.hexData
        )

        
        // Encrypt using AES128PaceKey
        let encryptedDf = try paceKey.encrypt(command: selectDF)
        
        let expectedCmd = testData.apdus["selectDf"]?.expectedEncryptedCmd
        // Print encrypted APDU
        print("Expected APDU: \(expectedCmd)")

        // Print encrypted APDU
        print("Actual APDU: \(printLikeExpectedApdu(commandApdu:encryptedDf))")

        XCTAssertEqual(printLikeExpectedApdu(commandApdu:encryptedDf), expectedCmd)
    }
    
    @MainActor
    func testDecryptDFResponse() throws {
        let dedicatedFile = DedicatedFile(
            aid: EgkFileSystem.DF.HCA.aid,
            fid: EgkFileSystem.EF.hcaPD.fid // holds Personal Data of health card holder
        )
        let selectDF = HealthCardCommand.Select.selectFile(with: dedicatedFile.aid)
        
        // PACE key setup
        let paceKey = AES128PaceKey(
            enc: testData.paceKey.enc.hexData,
            mac: testData.paceKey.mac.hexData
        )

        
        // Encrypt using AES128PaceKey
        let encryptedDf = try paceKey.encrypt(command: selectDF)
        
        let encryptedRes = testData.apdus["selectDf"]?.expectedEncryptedRes.hexData
        guard let encryptedRes = encryptedRes, encryptedRes.count >= 2 else {
            fatalError("Response is nil or too short")
        }
        
        // Extract SW1 and SW2
        let sw1 = encryptedRes[encryptedRes.count - 2]
        let sw2 = encryptedRes[encryptedRes.count - 1]

        // Extract data without SW1/SW2
        let dataWithoutSW = encryptedRes.prefix(encryptedRes.count - 2)
        
        let res = RawResponse(data: dataWithoutSW, sw1: sw1, sw2: sw2)
        let decryptedRes = try paceKey.decrypt(response: res)
        
        let expectedRes = testData.apdus["selectDf"]?.expectedRes ?? ""
        // Print encrypted APDU
        print("Expected APDU Res: \(expectedRes)")
        
        let stringyfiedRes = decryptedRes.data?.hexString ?? ""
        XCTAssertEqual(stringyfiedRes, expectedRes)
        
    }
    
    @MainActor
    func testBuildSelectEFByFIDCommand() throws {
        let dedicatedFile = DedicatedFile(
            aid: EgkFileSystem.DF.HCA.aid,
            fid: EgkFileSystem.EF.hcaPD.fid // holds Personal Data of health card holder
        )
        let selectEF = try HealthCardCommand.Select.selectEfRequestingFcp(with: dedicatedFile.fid ?? FileIdentifier("D001"), expectedLength: 256)
        
        
        let expectedCmd = testData.apdus["selectEfByFid"]?.expectedCmd
        // Print encrypted APDU
        print("Expected APDU: \(expectedCmd)")

        // Print encrypted APDU
        print("Actual APDU: \(selectEF.bytes.hexString)")

        XCTAssertEqual(selectEF.bytes.hexString, expectedCmd)
    }
    
    
    
    @MainActor
    func testBuildSelectEFByFIDCommandEncrypted() throws {
        let dedicatedFile = DedicatedFile(
            aid: EgkFileSystem.DF.HCA.aid,
            fid: EgkFileSystem.EF.hcaPD.fid // holds Personal Data of health card holder
        )
        let selectDF = HealthCardCommand.Select.selectFile(with: dedicatedFile.aid)
        
        // PACE key setup
        let paceKey = AES128PaceKey(
            enc: testData.paceKey.enc.hexData,
            mac: testData.paceKey.mac.hexData
        )

        
        // Encrypt DF cmd first iin order to increment ssc twice
        let encryptedDf = try paceKey.encrypt(command: selectDF)
        let selectEF = try HealthCardCommand.Select.selectEfRequestingFcp(with: dedicatedFile.fid ?? FileIdentifier("D001"), expectedLength: 256)

        // Encrypt using AES128PaceKey
        let encryptedEF = try paceKey.encrypt(command: selectEF)
        
        let expectedCmd = testData.apdus["selectEfByFid"]?.expectedEncryptedCmd
        // Print encrypted APDU
        print("Expected APDU: \(expectedCmd)")

        // Print encrypted APDU
        print("Actual APDU: \(printLikeExpectedApdu(commandApdu:encryptedEF))")

        XCTAssertEqual(printLikeExpectedApdu(commandApdu:encryptedEF), expectedCmd)
    }
    
    
  
    
    
    @MainActor
    func testDecryptEFByFIDResponse() throws {
        let dedicatedFile = DedicatedFile(
            aid: EgkFileSystem.DF.HCA.aid,
            fid: EgkFileSystem.EF.hcaPD.fid // holds Personal Data of health card holder
        )
        let selectDF = HealthCardCommand.Select.selectFile(with: dedicatedFile.aid)
        
        // PACE key setup
        let paceKey = AES128PaceKey(
            enc: testData.paceKey.enc.hexData,
            mac: testData.paceKey.mac.hexData
        )

        
        // Encrypt DF cmd first iin order to increment ssc twice
        let encryptedDf = try paceKey.encrypt(command: selectDF)
        let selectEF = try HealthCardCommand.Select.selectEfRequestingFcp(with: dedicatedFile.fid ?? FileIdentifier("D001"), expectedLength: 256)

        // Encrypt using AES128PaceKey
        let encryptedEF = try paceKey.encrypt(command: selectEF)
        
        let encryptedRes = testData.apdus["selectEfByFid"]?.expectedEncryptedRes.hexData
        
        guard let encryptedRes = encryptedRes, encryptedRes.count >= 2 else {
            fatalError("Response is nil or too short")
        }
        
        // Extract SW1 and SW2
        let sw1 = encryptedRes[encryptedRes.count - 2]
        let sw2 = encryptedRes[encryptedRes.count - 1]

        // Extract data without SW1/SW2
        let dataWithoutSW = encryptedRes.prefix(encryptedRes.count - 2)
        
        let res = RawResponse(data: dataWithoutSW, sw1: sw1, sw2: sw2)
        let decryptedRes = try paceKey.decrypt(response: res)
        
        let expectedRes = testData.apdus["selectEfByFid"]?.expectedRes ?? ""
        // Print encrypted APDU
        print("Expected APDU Res: \(expectedRes)")
        
        let stringyfiedRes = decryptedRes.data?.hexString ?? ""
        XCTAssertEqual(stringyfiedRes, expectedRes)
        
    }
    @MainActor
    func testCreateFCPFromEF() throws {
        let dedicatedFile = DedicatedFile(
            aid: EgkFileSystem.DF.HCA.aid,
            fid: EgkFileSystem.EF.hcaPD.fid // holds Personal Data of health card holder
        )
        let selectDF = HealthCardCommand.Select.selectFile(with: dedicatedFile.aid)
        
        // PACE key setup
        let paceKey = AES128PaceKey(
            enc: testData.paceKey.enc.hexData,
            mac: testData.paceKey.mac.hexData
        )

        
        // Encrypt DF cmd first iin order to increment ssc twice
        let encryptedDf = try paceKey.encrypt(command: selectDF)
        let selectEF = try HealthCardCommand.Select.selectEfRequestingFcp(with: dedicatedFile.fid ?? FileIdentifier("D001"), expectedLength: 256)

        // Encrypt using AES128PaceKey
        let encryptedEF = try paceKey.encrypt(command: selectEF)
        
        let encryptedRes = testData.apdus["selectEfByFid"]?.expectedEncryptedRes.hexData
        
        guard let encryptedRes = encryptedRes, encryptedRes.count >= 2 else {
            fatalError("Response is nil or too short")
        }
        
        // Extract SW1 and SW2
        let sw1 = encryptedRes[encryptedRes.count - 2]
        let sw2 = encryptedRes[encryptedRes.count - 1]

        // Extract data without SW1/SW2
        let dataWithoutSW = encryptedRes.prefix(encryptedRes.count - 2)
        
        let res = RawResponse(data: dataWithoutSW, sw1: sw1, sw2: sw2)
        let decryptedRes = try paceKey.decrypt(response: res)
        guard let fcpData = decryptedRes.data else {
            fatalError("Response no success")
        }
        let fcp = try FileControlParameter.parse(data: fcpData)

    }
    
    
    
    @MainActor
    func testDecryptReadResponse() throws {
        let dedicatedFile = DedicatedFile(
            aid: EgkFileSystem.DF.HCA.aid,
            fid: EgkFileSystem.EF.hcaPD.fid // holds Personal Data of health card holder
        )
        let selectDF = HealthCardCommand.Select.selectFile(with: dedicatedFile.aid)
        
        // PACE key setup
        let paceKey = AES128PaceKey(
            enc: testData.paceKey.enc.hexData,
            mac: testData.paceKey.mac.hexData
        )

        // Encrypt DF cmd first in order to increment ssc twice
        let encryptedDf = try paceKey.encrypt(command: selectDF)
        
        let selectEF = try HealthCardCommand.Select.selectEfRequestingFcp(with: dedicatedFile.fid ?? FileIdentifier("D001"), expectedLength: 256)
        // Encrypt EF cmd first in order to increment ssc another time
        let encryptedEF = try paceKey.encrypt(command: selectEF)
        
        
        
        let readFileCommand = try HealthCardCommand.Read.readFileCommand(ne: testData.fcp.readSize, offset: 0)
        let encryptedReadFileCommand = try paceKey.encrypt(command: readFileCommand)
        
        let encryptedRes = testData.apdus["readSelectedFile"]?.expectedEncryptedRes.hexData
        guard let encryptedRes = encryptedRes, encryptedRes.count >= 2 else {
            fatalError("Response is nil or too short")
        }
        
        // Extract SW1 and SW2
        let sw1 = encryptedRes[encryptedRes.count - 2]
        let sw2 = encryptedRes[encryptedRes.count - 1]

        // Extract data without SW1/SW2
        let dataWithoutSW = encryptedRes.prefix(encryptedRes.count - 2)
        
        let res = RawResponse(data: dataWithoutSW, sw1: sw1, sw2: sw2)
        let decryptedRes = try paceKey.decrypt(response: res)
        
        let expectedRes = testData.apdus["readSelectedFile"]?.expectedRes ?? ""
        // Print encrypted APDU
        print("Expected APDU Res: \(expectedRes)")
        
        let stringyfiedRes = decryptedRes.data?.hexString ?? ""
        print("Actual APDU Res: \(stringyfiedRes)")
        XCTAssertEqual(stringyfiedRes, expectedRes)
        
    }
}
