import React from "react";
import { StatusBar, StyleSheet } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { NavigationContainer } from "@react-navigation/native";
import { createBottomTabNavigator } from "@react-navigation/bottom-tabs";
import { Ionicons } from "@expo/vector-icons";
import RegistrarPrendaScreen from "./src/screens/RegistrarPrendaScreen";
import RegistrarPerfumeScreen from "./src/screens/RegistrarPerfumeScreen";
import RecomendarPerfumeScreen from "./src/screens/RecomendarPerfumeScreen";

const Tab = createBottomTabNavigator();

type NombreIcono = keyof typeof Ionicons.glyphMap;

// Icono relleno cuando la pestaña está activa, contorno cuando no
const ICONOS: Record<string, [NombreIcono, NombreIcono]> = {
  Prenda: ["shirt", "shirt-outline"],
  Perfume: ["flask", "flask-outline"],
  Recomendar: ["sparkles", "sparkles-outline"],
};

export default function App() {
  return (
    <SafeAreaView style={styles.safeArea}>
      <StatusBar barStyle="light-content" />
      <NavigationContainer>
        <Tab.Navigator
          screenOptions={({ route }) => ({
            headerShown: false,
            tabBarStyle: { backgroundColor: "#1e1e1e", borderTopColor: "#333333" },
            tabBarActiveTintColor: "#5b8def",
            tabBarInactiveTintColor: "#888888",
            tabBarIcon: ({ focused, color, size }) => {
              const [activo, inactivo] = ICONOS[route.name];
              return <Ionicons name={focused ? activo : inactivo} size={size} color={color} />;
            },
          })}
        >
          <Tab.Screen name="Prenda" component={RegistrarPrendaScreen} />
          <Tab.Screen name="Perfume" component={RegistrarPerfumeScreen} />
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
