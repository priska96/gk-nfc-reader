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
    s.source       = { :git => "https://github.com/priska96/gk-nfc-reader.git", :tag => "#{s.version}" }
  

    s.platform     = :ios, '14.0'

    s.source_files  = "ios/*.{h,m,swift}"
    s.dependency "React-Core"

    s.vendored_frameworks = 'ios/*.framework'

  end
  