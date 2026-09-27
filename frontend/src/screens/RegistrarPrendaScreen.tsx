import React, { useEffect, useState } from "react";
import {
  ActivityIndicator,
  Alert,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from "react-native";
import { apiGet, apiPost } from "../api/client";
import { CatalogoItem, PrendaResponse } from "../types";

export default function RegistrarPrendaScreen() {
  const [nombre, setNombre] = useState("");
  const [categorias, setCategorias] = useState<CatalogoItem[]>([]);
  const [colores, setColores] = useState<CatalogoItem[]>([]);
  const [estilos, setEstilos] = useState<CatalogoItem[]>([]);

  const [idCategoria, setIdCategoria] = useState<number | null>(null);
  const [idColor, setIdColor] = useState<number | null>(null);
  const [idEstilos, setIdEstilos] = useState<number[]>([]);

  const [cargandoCatalogos, setCargandoCatalogos] = useState(true);
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    async function cargarCatalogos() {
      try {
        const [cats, cols, ests] = await Promise.all([
          apiGet<CatalogoItem[]>("/categorias"),
          apiGet<CatalogoItem[]>("/colores"),
          apiGet<CatalogoItem[]>("/estilos"),
        ]);
        setCategorias(cats);
        setColores(cols);
        setEstilos(ests);
      } catch (error) {
        Alert.alert("Error", "No se pudieron cargar los catálogos. ¿Está el backend levantado?");
      } finally {
        setCargandoCatalogos(false);
      }
    }
    cargarCatalogos();
  }, []);

  function toggleEstilo(id: number) {
    setIdEstilos((prev) =>
      prev.includes(id) ? prev.filter((e) => e !== id) : [...prev, id]
    );
  }

  async function handleRegistrar() {
    if (!nombre.trim()) {
      Alert.alert("Falta el nombre", "Escribe un nombre para la prenda.");
      return;
    }
    if (idCategoria === null) {
      Alert.alert("Falta la categoría", "Selecciona una categoría.");
      return;
    }
    if (idColor === null) {
      Alert.alert("Falta el color", "Selecciona un color.");
      return;
    }
    if (idEstilos.length === 0) {
      Alert.alert("Falta el estilo", "Selecciona al menos un estilo.");
      return;
    }

    setEnviando(true);
    try {
      const prenda = await apiPost<PrendaResponse>("/prendas", {
        nombre: nombre.trim(),
        idCategoria,
        idColor,
        idEstilos,
      });
      Alert.alert("Registrada", `"${prenda.nombre}" se guardó en tu armario.`);
      setNombre("");
      setIdCategoria(null);
      setIdColor(null);
      setIdEstilos([]);
    } catch (error) {
      Alert.alert("Error al registrar", (error as Error).message);
    } finally {
      setEnviando(false);
    }
  }

  if (cargandoCatalogos) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" color="#5b8def" />
        <Text style={styles.loadingText}>Cargando catálogos...</Text>
      </View>
    );
  }

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <Text style={styles.titulo}>Registrar prenda</Text>

      <Text style={styles.label}>Nombre</Text>
      <TextInput
        style={styles.input}
        value={nombre}
        onChangeText={setNombre}
        placeholder="Ej: Chaqueta Leon S. Kennedy"
        placeholderTextColor="#888"
      />

      <Text style={styles.label}>Categoría</Text>
      <View style={styles.chipRow}>
        {categorias.map((c) => (
          <TouchableOpacity
            key={c.id}
            style={[styles.chip, idCategoria === c.id && styles.chipSelected]}
            onPress={() => setIdCategoria(c.id)}
          >
            <Text style={[styles.chipText, idCategoria === c.id && styles.chipTextSelected]}>
              {c.nombre}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      <Text style={styles.label}>Color</Text>
      <View style={styles.chipRow}>
        {colores.map((c) => (
          <TouchableOpacity
            key={c.id}
            style={[styles.chip, idColor === c.id && styles.chipSelected]}
            onPress={() => setIdColor(c.id)}
          >
            <Text style={[styles.chipText, idColor === c.id && styles.chipTextSelected]}>
              {c.nombre}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      <Text style={styles.label}>Estilos (puedes elegir varios)</Text>
      <View style={styles.chipRow}>
        {estilos.map((e) => (
          <TouchableOpacity
            key={e.id}
            style={[styles.chip, idEstilos.includes(e.id) && styles.chipSelected]}
            onPress={() => toggleEstilo(e.id)}
          >
            <Text style={[styles.chipText, idEstilos.includes(e.id) && styles.chipTextSelected]}>
              {e.nombre}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      <TouchableOpacity
        style={[styles.boton, enviando && styles.botonDeshabilitado]}
        onPress={handleRegistrar}
        disabled={enviando}
      >
        {enviando ? (
          <ActivityIndicator color="#fff" />
        ) : (
          <Text style={styles.botonTexto}>Registrar prenda</Text>
        )}
      </TouchableOpacity>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    padding: 20,
    paddingTop: 40,
    backgroundColor: "#111111",
    flexGrow: 1,
  },
  centered: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
    backgroundColor: "#111111",
  },
  loadingText: {
    color: "#fff",
    marginTop: 10,
  },
  titulo: {
    fontSize: 24,
    fontWeight: "700",
    color: "#fff",
    marginBottom: 20,
  },
  label: {
    color: "#cccccc",
    fontSize: 14,
    marginTop: 16,
    marginBottom: 8,
  },
  input: {
    backgroundColor: "#1e1e1e",
    color: "#fff",
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 16,
    borderWidth: 1,
    borderColor: "#333333",
  },
  chipRow: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8,
  },
  chip: {
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: "#333333",
    backgroundColor: "#1e1e1e",
  },
  chipSelected: {
    backgroundColor: "#5b8def",
    borderColor: "#5b8def",
  },
  chipText: {
    color: "#cccccc",
    fontSize: 14,
  },
  chipTextSelected: {
    color: "#fff",
    fontWeight: "600",
  },
  boton: {
    marginTop: 32,
    marginBottom: 40,
    backgroundColor: "#5b8def",
    borderRadius: 10,
    paddingVertical: 14,
    alignItems: "center",
  },
  botonDeshabilitado: {
    opacity: 0.6,
  },
  botonTexto: {
    color: "#fff",
    fontSize: 16,
    fontWeight: "700",
  },
});
