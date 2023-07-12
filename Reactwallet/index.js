/**
 * @format
 */

import {AppRegistry, View, ScrollView, StyleSheet} from 'react-native';
import App from './App';
import {name as appName} from './app.json';

const AppContainer = () => {
    return (
      <View style={styles.container}>
        <PopoverDialog />
      </View>
    );
  };

const PopoverDialog = () => {
    return (
        <View style={styles.popover}>
            <ScrollView contentContainerStyle={styles.contentContainer}>
                <App />
            </ScrollView>            
        </View>
    );
};

AppRegistry.registerComponent(appName, () => App);

const styles = StyleSheet.create({
    popover: {
      borderRadius: 8,
      paddingTop: 16,
      width: '100%',
      shadowColor: 'black',
      shadowOpacity: 0.4,
      shadowOffset: { width: 0, height: 2 },
      shadowRadius: 4,
      elevation: 5,
      position: 'absolute',
      backgroundColor: 'white',
      top: '10%',
      borderColor: 'red',
      left: 0,
    },
    container: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: 'rgba(255, 255, 255, 0.3)',
      },
      contentContainer: {
        flexGrow: 1,
        paddingHorizontal: 16,
        paddingBottom: 16,
      },
  });