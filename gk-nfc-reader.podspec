require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
    s.name         = "gk-nfc-reader"
    s.version      = package["version"]
    s.summary      = package["description"]
    s.homepage     = package["homepage"]
    s.license      = package["license"]
    s.authors      = package["author"]
    s.platforms    = { :ios => "14.0" }
    s.source       = { :git => package["repository"]["url"], :tag => "#{s.version}" }
  

    s.platform     = :ios, '14.0'

    s.source_files  = "ios/*.{h,m,swift}"
    s.dependency "React-Core"

    spm_dependency(s,  
     url: "https://github.com/gematik/ref-OpenHealthCardKit", 
     requirement: {kind: "upToNextMajorVersion", minimumVersion: "5.6.0"}, 
     products: ["CardReaderProviderApi", "HealthCardAccess", "HealthCardControl", "Helper", "NFCCardReaderProvider"] 
   ) 
    s.vendored_frameworks = 'ios/*.framework' # include the OpenSSL.framework here. Fix the hardcoded dependency later

    # Specify the resource files, including localization files
    s.resources = [
      'Resources/**/*.{png,jpeg,jpg,json,storyboard,xib}',   # Example for other resources
      'Resources/**/*.lproj/*.strings',                      # Include .strings files for localization
      'Resources/**/*.lproj/*.plist'                         # Include .plist files for localization
    ]
  end
