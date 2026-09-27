import React, { useCallback, useEffect, useState } from "react";
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
import { useFocusEffect } from "@react-navigation/native";
import { Ionicons } from "@expo/vector-icons";
import { apiDelete, apiGet, apiPost } from "../api/client";
import { CatalogoItem, PrendaResponse } from "../types";

export default function RegistrarPrendaScreen() {
  const [nombre, setNombre] = useState("");
  const [categorias, setCategorias] = useState<CatalogoItem[]>([]);
  const [colores, setColores] = useState<CatalogoItem[]>([]);
  const [estilos, setEstilos] = useState<CatalogoItem[]>([]);

  const [idCategoria, setIdCategoria] = useState<number | null>(null);
  const [idColor, setIdColor] = useState<number | null>(null);
  const [idEstilos, setIdEstilos] = useState<number[]>([]);

  const [armario, setArmario] = useState<PrendaResponse[]>([]);

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

  const cargarArmario = useCallback(async () => {
    try {
      setArmario(await apiGet<PrendaResponse[]>("/prendas"));
    } catch (error) {
      // Sin backend ya avisa la carga de catálogos; aquí no repetimos el aviso
    }
  }, []);

  // Al volver a la pestaña refrescamos por si algo cambió desde otra pantalla
  useFocusEffect(
    useCallback(() => {
      cargarArmario();
    }, [cargarArmario])
  );

  function confirmarBorrado(prenda: PrendaResponse) {
    Alert.alert("Borrar prenda", `¿Seguro que quieres borrar "${prenda.nombre}"?`, [
      { text: "Cancelar", style: "cancel" },
      {
        text: "Borrar",
        style: "destructive",
        onPress: async () => {
          try {
            await apiDelete(`/prendas/${prenda.idPrenda}`);
            setArmario((prev) => prev.filter((p) => p.idPrenda !== prenda.idPrenda));
          } catch (error) {
            Alert.alert("Error al borrar", (error as Error).message);
          }
        },
      },
    ]);
  }

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
      cargarArmario();
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

      <Text style={styles.seccion}>Tu armario ({armario.length})</Text>
      {armario.length === 0 ? (
        <Text style={styles.vacio}>Todavía no has registrado ninguna prenda.</Text>
      ) : (
        armario.map((p) => (
          <View key={p.idPrenda} style={styles.item}>
            <View style={styles.itemTexto}>
              <Text style={styles.itemNombre}>{p.nombre}</Text>
              <Text style={styles.itemDetalle}>
                {[p.categoria, p.color, ...p.estilos].join(" · ")}
              </Text>
            </View>
            <TouchableOpacity
              onPress={() => confirmarBorrado(p)}
              hitSlop={10}
              accessibilityLabel={`Borrar ${p.nombre}`}
            >
              <Ionicons name="trash-outline" size={20} color="#e05757" />
            </TouchableOpacity>
          </View>
        ))
      )}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    padding: 20,
    paddingTop: 40,
    paddingBottom: 40,
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
  seccion: {
    color: "#fff",
    fontSize: 18,
    fontWeight: "700",
    marginTop: 36,
    marginBottom: 12,
  },
  vacio: {
    color: "#888888",
    fontSize: 14,
    fontStyle: "italic",
  },
  item: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: "#1e1e1e",
    borderRadius: 10,
    paddingHorizontal: 14,
    paddingVertical: 12,
    marginBottom: 8,
  },
  itemTexto: {
    flex: 1,
    paddingRight: 12,
  },
  itemNombre: {
    color: "#fff",
    fontSize: 15,
    fontWeight: "600",
  },
  itemDetalle: {
    color: "#888888",
    fontSize: 13,
    marginTop: 2,
  },
});
