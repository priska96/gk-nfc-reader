Pod::Spec.new do |s|
    s.name         = "gk-nfc-reader"
    s.version      = "1.0.5"
    s.summary      = "Reads the Personal Data of eGK viw NFC"
    s.homepage     = "https://github.com/priska96/gk-nfc-reader"
    s.license      = "ISC"
    s.authors      = { "Priska Kohnen" => "kohnen@sciendis.de" }
    s.platforms    = { :ios => "14.0" }
    s.source       = { :git => "https://github.com/priska96/gk-nfc-reader.git", :tag => "#{s.version}" }
  

    s.platform     = :ios, '14.0'

    s.source_files  = "ios/**/*.{h,m,swift}"
    s.dependency "React-Core"

    s.vendored_frameworks = 'ios/CardReaderAccess.framework', 'ios/CardReaderProvider.framework', 'ios/HealthCardAccess.framework', 'ios/HealthCardControl.framework', 'ios/Helper.framework', 'ios/NFCCardReaderProvider.framework', 'ios/Util.framework'

  end
  