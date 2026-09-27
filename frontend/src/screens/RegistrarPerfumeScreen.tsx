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
import { apiDelete, apiGet, apiPatch, apiPost } from "../api/client";
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

  const [coleccion, setColeccion] = useState<PerfumeItem[]>([]);
  // Perfume cuyo estado se está cambiando, para bloquear toques repetidos
  const [actualizando, setActualizando] = useState<number | null>(null);

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

  const cargarColeccion = useCallback(async () => {
    try {
      setColeccion(await apiGet<PerfumeItem[]>("/perfumes?todos=true"));
    } catch (error) {
      // Sin backend ya avisa la carga de catálogos; aquí no repetimos el aviso
    }
  }, []);

  // Al volver a la pestaña refrescamos por si algo cambió desde otra pantalla
  useFocusEffect(
    useCallback(() => {
      cargarColeccion();
    }, [cargarColeccion])
  );

  async function cambiarEstado(perfume: PerfumeItem, estado: CatalogoItem) {
    if (perfume.estado === estado.nombre || actualizando !== null) return;
    setActualizando(perfume.idPerfume);
    try {
      const actualizado = await apiPatch<PerfumeItem>(`/perfumes/${perfume.idPerfume}/estado`, {
        idEstado: estado.id,
      });
      setColeccion((prev) =>
        prev.map((p) => (p.idPerfume === actualizado.idPerfume ? actualizado : p))
      );
    } catch (error) {
      Alert.alert("Error al cambiar el estado", (error as Error).message);
    } finally {
      setActualizando(null);
    }
  }

  function confirmarBorrado(perfume: PerfumeItem) {
    Alert.alert("Borrar perfume", `¿Seguro que quieres borrar "${perfume.nombre}"?`, [
      { text: "Cancelar", style: "cancel" },
      {
        text: "Borrar",
        style: "destructive",
        onPress: async () => {
          try {
            await apiDelete(`/perfumes/${perfume.idPerfume}`);
            setColeccion((prev) => prev.filter((p) => p.idPerfume !== perfume.idPerfume));
          } catch (error) {
            Alert.alert("Error al borrar", (error as Error).message);
          }
        },
      },
    ]);
  }

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
      cargarColeccion();
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

      <Text style={styles.seccion}>Tus perfumes ({coleccion.length})</Text>
      {coleccion.length === 0 && (
        <Text style={styles.vacio}>Todavía no has registrado ningún perfume.</Text>
      )}
      {estados.map((estado) => {
        const delEstado = coleccion.filter((p) => p.estado === estado.nombre);
        if (delEstado.length === 0) return null;
        return (
          <View key={estado.id}>
            <Text style={styles.grupo}>
              {estado.nombre} ({delEstado.length})
            </Text>
            {delEstado.map((p) => (
              <View key={p.idPerfume} style={styles.item}>
                <View style={styles.itemCabecera}>
                  <View style={styles.itemTexto}>
                    <Text style={styles.itemNombre}>{p.nombre}</Text>
                    <Text style={styles.itemDetalle}>
                      {[p.marca, p.familia].filter(Boolean).join(" · ")}
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
                <View style={styles.estadoRow}>
                  {estados.map((e) => {
                    const activo = p.estado === e.nombre;
                    return (
                      <TouchableOpacity
                        key={e.id}
                        style={[styles.estadoChip, activo && styles.estadoChipActivo]}
                        onPress={() => cambiarEstado(p, e)}
                        disabled={actualizando !== null}
                      >
                        <Text style={[styles.estadoChipTexto, activo && styles.chipTextSelected]}>
                          {e.nombre}
                        </Text>
                      </TouchableOpacity>
                    );
                  })}
                  {actualizando === p.idPerfume && <ActivityIndicator size="small" color="#5b8def" />}
                </View>
              </View>
            ))}
          </View>
        );
      })}
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
    marginBottom: 4,
  },
  grupo: {
    color: "#888888",
    fontSize: 12,
    fontWeight: "600",
    textTransform: "uppercase",
    letterSpacing: 1,
    marginTop: 16,
    marginBottom: 8,
  },
  vacio: {
    color: "#888888",
    fontSize: 14,
    fontStyle: "italic",
    marginTop: 8,
  },
  item: {
    backgroundColor: "#1e1e1e",
    borderRadius: 10,
    paddingHorizontal: 14,
    paddingVertical: 12,
    marginBottom: 8,
  },
  itemCabecera: {
    flexDirection: "row",
    alignItems: "center",
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
  estadoRow: {
    flexDirection: "row",
    flexWrap: "wrap",
    alignItems: "center",
    gap: 6,
    marginTop: 10,
  },
  estadoChip: {
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: "#333333",
  },
  estadoChipActivo: {
    backgroundColor: "#5b8def",
    borderColor: "#5b8def",
  },
  estadoChipTexto: {
    color: "#aaaaaa",
    fontSize: 12,
  },
});
