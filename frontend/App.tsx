import React from "react";
import { StatusBar, StyleSheet } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { NavigationContainer } from "@react-navigation/native";
import { createBottomTabNavigator } from "@react-navigation/bottom-tabs";
import RegistrarPrendaScreen from "./src/screens/RegistrarPrendaScreen";
import RecomendarPerfumeScreen from "./src/screens/RecomendarPerfumeScreen";

const Tab = createBottomTabNavigator();

export default function App() {
  return (
    <SafeAreaView style={styles.safeArea}>
      <StatusBar barStyle="light-content" />
      <NavigationContainer>
        <Tab.Navigator
          screenOptions={{
            headerShown: false,
            tabBarStyle: { backgroundColor: "#1e1e1e" },
            tabBarActiveTintColor: "#5b8def",
            tabBarInactiveTintColor: "#888888",
          }}
        >
          <Tab.Screen name="Registrar" component={RegistrarPrendaScreen} />
          <Tab.Screen name="Recomendar" component={RecomendarPerfumeScreen} />
        </Tab.Navigator>
      </NavigationContainer>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: "#111111",
  },
});
