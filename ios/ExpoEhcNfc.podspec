require "json"

package = JSON.parse(File.read(File.join(__dir__, "..",  "package.json")))

Pod::Spec.new do |s|
    s.name         = "ExpoEhcNfc"
    s.version      = package["version"]
    s.summary      = package["description"]
    s.homepage     = package["homepage"]
    s.license      = package["license"]
    s.authors      = package["author"]
    s.platforms    = { :ios => "14.0" }
    s.source       = { :git => package["repository"]["url"], :tag => "#{s.version}" }
  

    s.platform     = :ios, '14.0'
    
    # Uncomment once ExpoModules API works with dynamic frameworks
    # s.static_framework = false
    # s.dependency  'ExpoModulesCore'
    # # Swift/Objective-C compatibility
    # s.pod_target_xcconfig = {
    #   'DEFINES_MODULE' => 'YES',
    # }

    s.dependency "React-Core"

    s.source_files  = "**/*.{h,m,swift}"
    s.exclude_files = "Tests/**/*", "ExpoModule/**/*", "OpenSSL.framework/**/*"
    
    spm_dependency(s,  
      url: "https://github.com/gematik/ref-OpenHealthCardKit", 
      requirement: {kind: "upToNextMajorVersion", minimumVersion: "5.6.0"}, 
      products: ["CardReaderProviderApi", "HealthCardAccess", "HealthCardControl", "Helper", "NFCCardReaderProvider"] 
    ) 
  #    spm_dependency(s,  
  #    url: "https://github.com/gematik/OpenSSL-Swift", 
  #    requirement: {kind: "upToNextMajorVersion", minimumVersion: "4.2.0"}, 
  #    products: ["OpenSSL-Swift"] 
  #  ) 
    s.vendored_frameworks = '*.framework' # include the OpenSSL.framework here. Fix the hardcoded dependency later

    # Specify the resource files, including localization files
    s.resources = [
      'Resources/**/*.{png,jpeg,jpg,json,storyboard,xib}',   # Example for other resources
      'Resources/**/*.lproj/*.strings',                      # Include .strings files for localization
      'Resources/**/*.lproj/*.plist'                         # Include .plist files for localization
    ]

    s.test_spec 'Tests' do |test_spec|
      test_spec.source_files = 'Tests/**/*.{swift}'
      test_spec.resources = [
      '../TestResources/test_data.json',
    ]
    end
end
  