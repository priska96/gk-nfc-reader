# gk-nfc-reader

Reads the Personal Data of German eGK via NFC

# API documentation

## Types

The state of the GKNFCReader. `value` is only defined if reading the data was successful. `value` holds the data of the [**MF / DF.HCA / EF.PD**](https://gemspec.gematik.de/docs/gemSpec/gemSpec_eGK_ObjSys_G2_1/latest/#5.4.4) file, which is the personal data of the card holder.

```
enum State {
  idle = "idle",
  loading = "loading",
  success = "success",
  error = "error",
}

export interface PersonalData {
  lastname: string;
  firstname: string;
  birthday: string;
  gender: string;
  street: string;
  housenumber: string;
  zipCode: string;
  city: string;
  counrtyCode: string;
  insuranceId: string;
}

export interface NFCReaderState {
  state: State;
  value?: PersonalData;
  error?: string;
}
```

This type defines what kind of information can be sent to the GKNFCReader. For now Only the can (Card Acess Number) is needed and used to get a hold of the personal data.

```
export interface ReadPersonalDataOptions {
  can: string;
  pin?: string;
  checkBrainpoolAlgorithm?: boolean;
}
```

## useReadPersonalData hook

This hook is the main part of this paackge. It connectes to the GKNFCReader via the can, subscribes to the `NFCReaderState`, and updates the results which holds a summary of all commands that were send between the card and th NFC redaer device (smartphone). The hook therefore returns the `state`, the `result`and the `readPersonalData(data: ReadPersonalDataOptions)` function.

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
  const { result, state, readPersonalData } = useReadPersonalData();
  return (
    <SafeAreaView style={styles.container}>
      <TextInput
        style={styles.input}
        placeholder="Type something"
        value={can}
        onChangeText={(newText) => setCan(newText)} // Update state on text change
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

# Installation in managed Expo projects

```
npx expo install gk-nfc-reader
```

Afterwards add the expo-config-plugin to the app.json

```
//app.json

...
"plugins": [
    ...
      [
        "../app.plugin.js",
        {
          "nfcReaderUsageDescription": "NFC Test", // you can put any description here
          "deploymentTarget": "14.0" // we need a minimum of 14.0
        }
      ]
    ]

```

# Installation in bare React Native projects

### Add the package to your npm dependencies

```
npm install gk-nfc-reader
```

### Configure for iOS

Run `npx pod-install` after installing the npm package.

### Configure for Android

Not supported

# Contributing

Contributions are very welcome! Please refer to guidelines described in the [contributing guide](https://github.com/expo/expo#contributing).
