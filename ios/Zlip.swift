//
//  Zlib.swift
//
//  Created by Priska Kohnen on 01.10.24.
//

import struct Foundation.Data

#if os(Linux)
import zlibLinux
#else
import zlib
#endif

public enum ZlibError: Error {
  case initializationFailed
  case streamError
  case decompressionFailed
  case incompleteDecompression
  
  
  var description: String {
      switch self {
      case .initializationFailed:
          return NSLocalizedString("zlib_initialization_failed", comment: "")
      case .streamError:
        return NSLocalizedString("zlib_stream_error", comment: "")
      case .decompressionFailed:
        return NSLocalizedString("zlib_decompression_failed", comment: "")
      case .incompleteDecompression:
        return NSLocalizedString("zlib_incomplete_decompression", comment: "")
      }
  }
  
}

/// Compression level whose rawValue is based on the zlib's constants.

private enum DataSize {
  
  static let chunk = 1 << 14
  static let stream = MemoryLayout<z_stream>.size
}

func decompressZlib(_ data: Data) throws -> Data {
  let bufferSize = 64 * 1024  // 64KB buffer size
  var decompressedData = Data()
  
  // Create a source buffer from the input data
  try data.withUnsafeBytes { (sourceBuffer: UnsafeRawBufferPointer) in
    guard let sourcePointer = sourceBuffer.baseAddress?.assumingMemoryBound(to: UInt8.self) else {
      throw ZlibError.streamError
    }
    
    // Compression stream setup with all fields initialized
    var stream = z_stream(
      next_in: UnsafeMutablePointer<UInt8>(mutating: sourcePointer),
      avail_in: uInt(data.count),
      total_in: 0,
      next_out: nil,
      avail_out: 0,
      total_out: 0,
      msg: nil,
      state: nil,
      zalloc: nil,
      zfree: nil,
      opaque: nil,
      data_type: 0,
      adler: 0,
      reserved: 0
    )
    
    // Initialize decompression (use 32 for automatic zlib/gzip header detection)
    let initStatus = inflateInit2_(&stream, 32 + MAX_WBITS, ZLIB_VERSION, Int32(DataSize.stream))
    guard initStatus == Z_OK else {
      throw ZlibError.initializationFailed
    }
    
    // Perform the decompression
    var status: Int32 = Z_NULL
    repeat {
      // Output buffer
      var destinationBuffer = [UInt8](repeating: 0, count: bufferSize)
      
      // Use withUnsafeMutableBytes to get a mutable pointer to the destination buffer
      try destinationBuffer.withUnsafeMutableBytes { (destBuffer: UnsafeMutableRawBufferPointer) in
        guard let destinationPointer = destBuffer.baseAddress?.assumingMemoryBound(to: UInt8.self) else {
          throw ZlibError.streamError
        }
        
        stream.next_out = destinationPointer
        stream.avail_out = uInt(bufferSize)
        
        // Decompress
        status = inflate(&stream, Z_NO_FLUSH)
        
        if status == Z_STREAM_ERROR {
          throw ZlibError.decompressionFailed
        }
      }
      
      // Append to decompressed data
      let bytesDecompressed = bufferSize - Int(stream.avail_out)
      decompressedData.append(destinationBuffer, count: bytesDecompressed)
      
    } while status != Z_STREAM_END
    
    // Finalize the decompression
    let endStatus = inflateEnd(&stream)
    guard endStatus == Z_OK else {
      throw ZlibError.incompleteDecompression
    }
  }
  
  return decompressedData
}
