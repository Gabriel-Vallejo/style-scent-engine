import React, { useCallback, useEffect, useState } from "react";
import {
  ActivityIndicator,
  Alert,
  Image,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from "react-native";
import { useFocusEffect } from "@react-navigation/native";
import { Ionicons } from "@expo/vector-icons";
import * as ImagePicker from "expo-image-picker";
import { apiDelete, apiGet, apiPost, apiUploadImagen } from "../api/client";
import { AnalisisPrenda, CatalogoItem, PrendaResponse } from "../types";

function porcentaje(confianza: number): string {
  return `${Math.round(confianza * 100)} %`;
}

export default function RegistrarPrendaScreen() {
  const [nombre, setNombre] = useState("");
  const [categorias, setCategorias] = useState<CatalogoItem[]>([]);
  const [colores, setColores] = useState<CatalogoItem[]>([]);
  const [estilos, setEstilos] = useState<CatalogoItem[]>([]);

  const [idCategoria, setIdCategoria] = useState<number | null>(null);
  const [idColor, setIdColor] = useState<number | null>(null);
  const [idEstilos, setIdEstilos] = useState<number[]>([]);

  const [armario, setArmario] = useState<PrendaResponse[]>([]);

  // Fase 2: foto analizada por el servicio de visión para prerrellenar el formulario
  const [fotoUri, setFotoUri] = useState<string | null>(null);
  const [analizando, setAnalizando] = useState(false);
  const [analisis, setAnalisis] = useState<AnalisisPrenda | null>(null);

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
      } catch {
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
    } catch {
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

  async function elegirFoto(origen: "camara" | "galeria") {
    const permiso =
      origen === "camara"
        ? await ImagePicker.requestCameraPermissionsAsync()
        : await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (!permiso.granted) {
      Alert.alert("Sin permiso", "Necesito acceso para poder analizar la foto.");
      return;
    }

    const opciones: ImagePicker.ImagePickerOptions = {
      mediaTypes: ["images"],
      // Recortar a la prenda mejora mucho el análisis (menos fondo que confunda)
      allowsEditing: true,
      quality: 0.7,
    };
    const resultado =
      origen === "camara"
        ? await ImagePicker.launchCameraAsync(opciones)
        : await ImagePicker.launchImageLibraryAsync(opciones);
    if (resultado.canceled) return;

    const foto = resultado.assets[0];
    setFotoUri(foto.uri);
    setAnalisis(null);
    setAnalizando(true);
    try {
      const sugerencias = await apiUploadImagen<AnalisisPrenda>(
        "/prendas/analizar",
        foto.uri,
        foto.mimeType
      );
      setAnalisis(sugerencias);
      // Solo sugerencias: se preseleccionan y el usuario las cambia si no le convencen
      if (sugerencias.categoria) setIdCategoria(sugerencias.categoria.id);
      if (sugerencias.color) setIdColor(sugerencias.color.id);
      if (sugerencias.estilos.length > 0) setIdEstilos(sugerencias.estilos.map((e) => e.id));
    } catch (error) {
      Alert.alert(
        "No se pudo analizar",
        `${(error as Error).message}\n\nPuedes rellenar el formulario a mano.`
      );
    } finally {
      setAnalizando(false);
    }
  }

  function quitarFoto() {
    setFotoUri(null);
    setAnalisis(null);
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
      quitarFoto();
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

      <View style={styles.fotoCaja}>
        {fotoUri ? (
          <View style={styles.fotoFila}>
            <Image source={{ uri: fotoUri }} style={styles.fotoMiniatura} />
            <View style={styles.fotoInfo}>
              {analizando ? (
                <View style={styles.fotoFila}>
                  <ActivityIndicator color="#5b8def" />
                  <Text style={styles.fotoTexto}>  Analizando la prenda...</Text>
                </View>
              ) : analisis ? (
                <>
                  <Text style={styles.fotoTitulo}>Sugerencias de la IA</Text>
                  {analisis.categoria && (
                    <Text style={styles.fotoTexto}>
                      {analisis.categoria.nombre} · {porcentaje(analisis.categoria.confianza)}
                    </Text>
                  )}
                  {analisis.color && (
                    <View style={styles.fotoFila}>
                      <View
                        style={[styles.muestraColor, { backgroundColor: analisis.colorDominanteHex }]}
                      />
                      <Text style={styles.fotoTexto}>
                        {analisis.color.nombre} · {porcentaje(analisis.color.confianza)}
                      </Text>
                    </View>
                  )}
                  {analisis.estilos.length > 0 && (
                    <Text style={styles.fotoTexto}>
                      {analisis.estilos.map((e) => e.nombre).join(", ")}
                    </Text>
                  )}
                  <Text style={styles.fotoAyuda}>Revísalas abajo antes de registrar.</Text>
                </>
              ) : null}
            </View>
            <TouchableOpacity onPress={quitarFoto} hitSlop={10} accessibilityLabel="Quitar foto">
              <Ionicons name="close" size={20} color="#888888" />
            </TouchableOpacity>
          </View>
        ) : (
          <>
            <Text style={styles.fotoTitulo}>Rellenar con una foto</Text>
            <Text style={styles.fotoAyuda}>
              La IA sugiere la categoría, el color y los estilos. Tú confirmas.
            </Text>
            <View style={styles.fotoBotones}>
              <TouchableOpacity style={styles.fotoBoton} onPress={() => elegirFoto("camara")}>
                <Ionicons name="camera-outline" size={18} color="#5b8def" />
                <Text style={styles.fotoBotonTexto}>Hacer foto</Text>
              </TouchableOpacity>
              <TouchableOpacity style={styles.fotoBoton} onPress={() => elegirFoto("galeria")}>
                <Ionicons name="images-outline" size={18} color="#5b8def" />
                <Text style={styles.fotoBotonTexto}>Galería</Text>
              </TouchableOpacity>
            </View>
          </>
        )}
      </View>

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
  fotoCaja: {
    backgroundColor: "#1e1e1e",
    borderRadius: 12,
    borderWidth: 1,
    borderColor: "#333333",
    borderStyle: "dashed",
    padding: 14,
    marginBottom: 8,
  },
  fotoFila: {
    flexDirection: "row",
    alignItems: "center",
  },
  fotoMiniatura: {
    width: 72,
    height: 72,
    borderRadius: 8,
    marginRight: 12,
    backgroundColor: "#333333",
  },
  fotoInfo: {
    flex: 1,
    gap: 2,
  },
  fotoTitulo: {
    color: "#fff",
    fontSize: 15,
    fontWeight: "600",
  },
  fotoTexto: {
    color: "#cccccc",
    fontSize: 13,
  },
  fotoAyuda: {
    color: "#888888",
    fontSize: 12,
    marginTop: 2,
  },
  muestraColor: {
    width: 12,
    height: 12,
    borderRadius: 6,
    marginRight: 6,
    borderWidth: 1,
    borderColor: "#555555",
  },
  fotoBotones: {
    flexDirection: "row",
    gap: 10,
    marginTop: 12,
  },
  fotoBoton: {
    flex: 1,
    flexDirection: "row",
    justifyContent: "center",
    alignItems: "center",
    gap: 6,
    borderWidth: 1,
    borderColor: "#5b8def",
    borderRadius: 8,
    paddingVertical: 10,
  },
  fotoBotonTexto: {
    color: "#5b8def",
    fontSize: 14,
    fontWeight: "600",
  },
});
