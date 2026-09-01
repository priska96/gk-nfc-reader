import * as EhcNfc from 'gk-nfc-reader';
import {
  Button,
  Text,
  SafeAreaView,
  TextInput,
  StyleSheet,
} from 'react-native';
import { useState } from 'react';

const App = () => {
  const [can, setCan] = useState('123123');
  const { nfcReaderState, readPersonalData } = EhcNfc.useReadPersonalData();

  return (
    <SafeAreaView style={styles.container}>
      <TextInput
        style={styles.input}
        placeholder="Type something"
        value={can}
        onChangeText={(newText) => setCan(newText)}
      />
      <Button
        title="Read HCA.PD"
        onPress={() => {
          readPersonalData({ can });
        }}
      />
      <Text style={styles.text}>State: {nfcReaderState.state}</Text>
      <Text style={styles.text}>
        Value:
        {JSON.stringify(nfcReaderState.value, null, 2) ?? ' No Value'}
      </Text>
      <Text style={styles.text}>
        Error:
        {nfcReaderState.error ?? ' No Error'}
      </Text>
    </SafeAreaView>
  );
};

export default App;

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 20,
  },
  input: {
    height: 40,
    borderColor: 'gray',
    borderWidth: 1,
    marginBottom: 20,
    paddingLeft: 10,
  },
  text: {
    paddingTop: 10,
    fontSize: 18,
  },

  box: {
    width: 100,
    height: 80,
    backgroundColor: 'black',
    margin: 30,
  },
});
