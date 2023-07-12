/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 * @flow strict-local
 */

import React, {useState, useEffect} from 'react';
import {Node} from 'react';
import {EventEmitter2} from 'eventemitter2';
import {
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  useColorScheme,
  View,
  Button,
} from 'react-native';

import {
  Colors,
  DebugInstructions,
  Header,
  LearnMoreLinks,
} from 'react-native/Libraries/NewAppScreen';

import {
  NativeModules,
  DeviceEventEmitter,
  NativeEventEmitter,
} from 'react-native';

import AndroidSDKEventHandler from './AndroidSDKEventHandler';

const Section = ({children, title}): Node => {
  const isDarkMode = useColorScheme() === 'dark';
  return (
    <View style={styles.sectionContainer}>
      <Text
        style={[
          styles.sectionTitle,
          {
            color: isDarkMode ? Colors.white : Colors.black,
          },
        ]}>
        {title}
      </Text>
      <Text
        style={[
          styles.sectionDescription,
          {
            color: isDarkMode ? Colors.light : Colors.dark,
          },
        ]}>
        {children}
      </Text>
    </View>
  );
};

const CommunicationClient = NativeModules.CommunicationClient;
//const emitter = new EventEmitter2();
//const nativeEventEmitter = new NativeEventEmitter(DeviceEventEmitter);

const App: () => Node = () => {
  const isDarkMode = useColorScheme() === 'dark';

  const backgroundStyle = {
    backgroundColor: isDarkMode ? Colors.darker : Colors.lighter,
  };

  const [result, setResult] = useState('');

 // useEffect(() => {
    // const handleTerminate = message => {
    //   // Handle the received event data
    //   console.log(`Got terminate message: ${message}`);
    // };

    // const clientsConnectedListener = AndroidSDKEventHandler.onClientsConnected(
    //   id => {
    //     console.log(`YAY Received clients_connected: ${id}`);
    //   },
    // );

    // const clientsDisconnectedListener =
    //   AndroidSDKEventHandler.onClientsDisconnected(id => {
    //     console.log(`YAY Received clients_disconnected: ${id}`);
    //   });

    // const messageListener = AndroidSDKEventHandler.onMessageReceived(
    //   message => {
    //     console.log(`YAY Received message: ${message}`);
    //     const msg = {
    //       message: {
    //         type: 'wallet_info',
    //         data: {
    //           name: 'MetaMask',
    //           version: '1.1.2',
    //         },
    //       },
    //     };
    //     CommunicationClient.sendMessage(JSON.stringify(msg));
    //   },
    // );

    // nativeEventEmitter.addListener('message', handleMessage);
    // nativeEventEmitter.addListener('terminate', handleTerminate);
    // nativeEventEmitter.addListener('key_info', handleKeyInfo);
    // nativeEventEmitter.addListener('clients_ready', handleClientsReady);
    // nativeEventEmitter.addListener('keys_exchanged', handleKeysExchanged);
    // nativeEventEmitter.addListener('clients_waiting', handleClientsWaiting);
    // nativeEventEmitter.addListener('clients_connected', handleClientsConnected);
    // nativeEventEmitter.addListener(
    //   'clients_disconnected',
    //   handleClientsDisconnected,
    // );

  //   return () => {
  //     // emitter.off('message', handleMessage);
  //     nativeEventEmitter.removeAllListeners('message');
  //   };
  // });

  const bindService = async () => {
    await CommunicationClient.bindService()
      .then(response => {
        setResult(response);
        console.log("Binding serving now now!");
        console.log(response);
      })
      .catch(error => {
        console.error(error);
      });
  };

  return (
    <SafeAreaView style={backgroundStyle}>
      <StatusBar barStyle={isDarkMode ? 'light-content' : 'dark-content'} />
      <ScrollView
        contentInsetAdjustmentBehavior="automatic"
        style={backgroundStyle}>
        <Header />
        <View
          style={{
            backgroundColor: isDarkMode ? Colors.black : Colors.white,
          }}>
          <Section title="App Name">React Wallet!</Section>
          <Section title="Step One">
            Edit <Text style={styles.highlight}>App.js</Text> to change this
            screen and then come back to see your edits.
          </Section>
          <Section title="Native Module">
            <View>
              <Button title="Bind service" onPress={bindService} />
            </View>
          </Section>
          <Section title="Debug">
            <DebugInstructions />
          </Section>
          <Section title="Learn More">
            Read the docs to discover what to do next:
          </Section>
          <LearnMoreLinks />
        </View>
      </ScrollView>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  sectionContainer: {
    marginTop: 32,
    paddingHorizontal: 24,
  },
  sectionTitle: {
    fontSize: 24,
    fontWeight: '600',
  },
  sectionDescription: {
    marginTop: 8,
    fontSize: 18,
    fontWeight: '400',
  },
  highlight: {
    fontWeight: '700',
  },
});

export default App;
