# gk-nfc-reader

Reads the Personal Data of German eGK via NFC

# Installation in Expo projects

```
npx expo install @sciendis/gk-nfc-reader
```

Afterwards add the expo-config-plugin to the app.json bzw. app.config.js. Also we need to add the **USE_FRAMEWORK dynamic** flag to the build process and set the required **minSdk** and **kotlin** version. Therefore add the expo-build-properties module to the app and add the following to the app.json.

```
//app.json

...
"plugins": [
    ...
    [
        "expo-build-properties",
        {
          "android": {
            "mindSdkVersion": 30,
            "kotlinVersion": "2.0.21"
          }
          "ios": {
            "useFrameworks": "dynamic"
          }
        }
      ],
      [
        "gk-nfc-reader",
        {
          "nfcReaderUsageDescription": "NFC Test", // you can put any description here
          "deploymentTarget": "14.0", // we need a minimum of 14.0 but maybe it collides with other packages and needs to be even more increased
          "staticLibraryList": ["RNReanimated"] //since we use dynamic_frameworks some libraries crash due to that, so pass a list of libraries that should static
          "languages": ["de", "en"] // list of languages for translations. first one is default language
          "minSdkVersion": 30 // we need at least android sdk 30 otherwise asn1 library does not work
          "kotlinVersion": "2.0.21" // we need at least android sdk 30 otherwise asn1 library does not work
          "development": true // only add this flag when developing and integrating the pkg in the example app
        }
      ]
    ]

```

## Build a new dev client

Afterwards run the command to build a new dev expo client.

# Installation in bare React Native projects

### Add the package to your npm dependencies

```
npm install gk-nfc-reader
```

### Configure for iOS

Run `npx pod-install` after installing the npm package.

### Configure for Android

Let the gradle sync run through.

# API documentation

## Types

The state of the NfcReader. `value` is only defined if reading the data was successful. `value` holds the data of the [**MF / DF.HCA / EF.PD**](https://gemspec.gematik.de/docs/gemSpec/gemSpec_eGK_ObjSys_G2_1/latest/#5.4.4) file, which is the personal data of the card holder.

```
enum State {
  idle = "idle",
  loading = "loading",
  success = "success",
  failure = "failure",
}

export interface PersonalData {
  title: string;
  lastname: string;
  firstname: string;
  nameAddition: string;
  birthday: string;
  gender: string;
  street: string;
  housenumber: string;
  zipCode: string;
  city: string;
  countryCode: string;
  insuranceId: string;
}

export interface NFCReaderState {
  state: State;
  value?: PersonalData;
  error?: string;
}
```

This type defines what kind of information can be sent to the GKNFCReader. For now only the can (Card Access Number) is needed and used to get a hold of the personal data.

```
export interface ReadPersonalDataOptions {
  can: string;
  pin?: string;
  checkBrainpoolAlgorithm?: boolean;
}
```

## useReadPersonalData hook

This hook is the main part of this paackge. It connects to the GKNFCReader via the can, subscribes to the `NFCReaderState`, and updates the results which holds a summary of all commands that were send between the card and the NFC reader device (smartphone). The hook therefore returns the `nfcReaderState` and the `readPersonalData(data: ReadPersonalDataOptions)` function.

For easy integration use provided usage example

```
import {
  Button,
  Text,
  SafeAreaView,
  TextInput,
  StyleSheet,
} from "react-native";
import { useState } from "react";
import { useReadPersonalData } from "gk-nfc-reader";

const App = () => {
  const [can, setCan] = useState("");
  const { nfcReaderState, readPersonalData } = useReadPersonalData();
  return (
    <SafeAreaView style={styles.container}>
      <TextInput
        style={styles.input}
        placeholder="Type something"
        value={can}
        onChangeText={(newText) => setCan(newText)}
      />
      <Button title="Read HCA.PD" onPress={() => readPersonalData({ can })} />
      <Text style={styles.text}>
        {" "}
        State: {state.state} {JSON.stringify(state.value)} {state.error}
      </Text>
      <Text style={styles.text}> Res: {res}</Text>
    </SafeAreaView>
  );
};

export default App;

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  input: {
    height: 40,
    borderColor: "gray",
    borderWidth: 1,
    marginBottom: 20,
    paddingLeft: 10,
  },
  text: {
    fontSize: 18,
  },
});
```

# Contributing & Further Development

## Setup

```
git checkout dev
npm i // this one runs expo prepare script automatically and will also build the expo-config-plugin
npm run build // this keeps your build process in watch mode and will continuesly build the project
```

## ios

In a second terminal run the following to start the app

```
cd example
npm install expo
npx expo install
npx expo prebuild --clean -p ios
npx expo run:ios
```

### Troubleshooting

Make sure the latest xCode and latest simulator ios versions are installed

Now the app should be running on the simulator. However the simulator does not support NFC. Therefore you need to install it on your device via xCode.

### Install via xCode

After the prebuild step run `xed ios` in the example folder to open the example xcode workspace in xCode. Select the target and under Signing&Capabilities Team needs to be set to "sciendis GmbH" and your apple dev account needs to be part of the sciendis team. Change the bundle identifier to "de.sciendis.gk" because only for this bundle the NFC capability is activated. Then with the "+Capabilities" button add the "Near Field Communication Tag Reading" capability. Connect your device via usb and change the target to install on your device.

## Android

In a second terminal run the following to start the app

```
cd example
npm install expo
npx expo install
npx expo prebuild --clean -p android
npx expo run:android
```

### Develop with Android Studio

After the prebuild step run `npm run open:android` in the root directory in order to open the project and set the node path correctly in your env, otherwise it might lead to errors. Let Android Studio finish the gradle sync and now in the hierachy structure you can select `sciendis-gk-nfc-reader -> kotlin+java -> expo.modules.ehcnfc` to see all packages of this module.
Under `run configurations` you can add a new gradle config to run tests and use the debugger
at `:sciendis-gk-nfc-reader:testDebugUnitTest --tests "expo.modules.ehcnfc.SecureCommandsTest.testDecryptReadResponse"` in the **Run** input field and select the `example/android` folder as your **Gradel Project**.
`SecureCommandsTest.testDecryptReadResponse` This defines which test is running so you can configure whatever you want to run here.
