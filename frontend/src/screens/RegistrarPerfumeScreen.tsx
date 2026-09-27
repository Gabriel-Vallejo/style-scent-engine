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
import { CatalogoItem, PerfumeItem } from "../types";

export default function RegistrarPerfumeScreen() {
  const [nombre, setNombre] = useState("");
  const [marca, setMarca] = useState("");
  const [familias, setFamilias] = useState<CatalogoItem[]>([]);
  const [notas, setNotas] = useState<CatalogoItem[]>([]);
  const [estados, setEstados] = useState<CatalogoItem[]>([]);

  const [idFamilia, setIdFamilia] = useState<number | null>(null);
  const [idEstado, setIdEstado] = useState<number | null>(null);
  const [idNotas, setIdNotas] = useState<number[]>([]);

  const [cargandoCatalogos, setCargandoCatalogos] = useState(true);
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    async function cargarCatalogos() {
      try {
        const [fams, nots, ests] = await Promise.all([
          apiGet<CatalogoItem[]>("/familias"),
          apiGet<CatalogoItem[]>("/notas"),
          apiGet<CatalogoItem[]>("/estados"),
        ]);
        setFamilias(fams);
        setNotas(nots);
        setEstados(ests);
        // Lo normal es registrar algo que ya tienes: preseleccionamos el primer estado ("En coleccion")
        if (ests.length > 0) setIdEstado(ests[0].id);
      } catch (error) {
        Alert.alert("Error", "No se pudieron cargar los catálogos. ¿Está el backend levantado?");
      } finally {
        setCargandoCatalogos(false);
      }
    }
    cargarCatalogos();
  }, []);

  function toggleNota(id: number) {
    setIdNotas((prev) =>
      prev.includes(id) ? prev.filter((n) => n !== id) : [...prev, id]
    );
  }

  async function handleRegistrar() {
    if (!nombre.trim()) {
      Alert.alert("Falta el nombre", "Escribe el nombre del perfume.");
      return;
    }
    if (idFamilia === null) {
      Alert.alert("Falta la familia", "Selecciona una familia olfativa.");
      return;
    }
    if (idEstado === null) {
      Alert.alert("Falta el estado", "Indica si ya lo tienes, está en camino o es un deseo.");
      return;
    }

    setEnviando(true);
    try {
      const perfume = await apiPost<PerfumeItem>("/perfumes", {
        nombre: nombre.trim(),
        marca: marca.trim() || null,
        idFamilia,
        idEstado,
        idNotas,
      });
      const aviso =
        perfume.estado === "En coleccion"
          ? "ya aparece en Recomendar."
          : `se guardó como "${perfume.estado}" (no se recomendará hasta que esté en tu colección).`;
      Alert.alert("Registrado", `"${perfume.nombre}" ${aviso}`);
      setNombre("");
      setMarca("");
      setIdFamilia(null);
      setIdNotas([]);
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
      <Text style={styles.titulo}>Registrar perfume</Text>

      <Text style={styles.label}>Nombre</Text>
      <TextInput
        style={styles.input}
        value={nombre}
        onChangeText={setNombre}
        placeholder="Ej: Ombré Leather"
        placeholderTextColor="#888"
      />

      <Text style={styles.label}>Marca (opcional)</Text>
      <TextInput
        style={styles.input}
        value={marca}
        onChangeText={setMarca}
        placeholder="Ej: Tom Ford"
        placeholderTextColor="#888"
      />

      <Text style={styles.label}>Estado</Text>
      <View style={styles.chipRow}>
        {estados.map((e) => (
          <TouchableOpacity
            key={e.id}
            style={[styles.chip, idEstado === e.id && styles.chipSelected]}
            onPress={() => setIdEstado(e.id)}
          >
            <Text style={[styles.chipText, idEstado === e.id && styles.chipTextSelected]}>
              {e.nombre}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      <Text style={styles.label}>Familia olfativa</Text>
      <View style={styles.chipRow}>
        {familias.map((f) => (
          <TouchableOpacity
            key={f.id}
            style={[styles.chip, idFamilia === f.id && styles.chipSelected]}
            onPress={() => setIdFamilia(f.id)}
          >
            <Text style={[styles.chipText, idFamilia === f.id && styles.chipTextSelected]}>
              {f.nombre}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      <Text style={styles.label}>Notas (opcional, puedes elegir varias)</Text>
      <View style={styles.chipRow}>
        {notas.map((n) => (
          <TouchableOpacity
            key={n.id}
            style={[styles.chip, idNotas.includes(n.id) && styles.chipSelected]}
            onPress={() => toggleNota(n.id)}
          >
            <Text style={[styles.chipText, idNotas.includes(n.id) && styles.chipTextSelected]}>
              {n.nombre}
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
          <Text style={styles.botonTexto}>Registrar perfume</Text>
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
