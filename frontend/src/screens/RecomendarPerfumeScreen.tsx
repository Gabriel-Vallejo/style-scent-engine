import React, { useCallback, useState } from "react";
import {
  ActivityIndicator,
  Alert,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";
import { useFocusEffect } from "@react-navigation/native";
import { apiGet, apiPost } from "../api/client";
import { MatchResult, PerfumeItem, PrendaResponse } from "../types";

export default function RecomendarPerfumeScreen() {
  const [prendas, setPrendas] = useState<PrendaResponse[]>([]);
  const [perfumes, setPerfumes] = useState<PerfumeItem[]>([]);

  const [prendasSeleccionadas, setPrendasSeleccionadas] = useState<number[]>([]);
  const [perfumeSeleccionado, setPerfumeSeleccionado] = useState<number | null>(null);

  const [cargando, setCargando] = useState(true);
  const [calculando, setCalculando] = useState(false);
  const [resultado, setResultado] = useState<MatchResult | null>(null);

  // Recargamos cada vez que se entra en la pestaña: las pestañas no se desmontan,
  // así que sin esto no aparecerían las prendas/perfumes recién registrados.
  useFocusEffect(
    useCallback(() => {
      async function cargarDatos() {
        try {
          const [listaPrendas, listaPerfumes] = await Promise.all([
            apiGet<PrendaResponse[]>("/prendas"),
            apiGet<PerfumeItem[]>("/perfumes"),
          ]);
          setPrendas(listaPrendas);
          setPerfumes(listaPerfumes);
        } catch (error) {
          Alert.alert("Error", "No se pudieron cargar tus prendas y perfumes.");
        } finally {
          setCargando(false);
        }
      }
      cargarDatos();
    }, [])
  );

  function togglePrenda(id: number) {
    setResultado(null);
    setPrendasSeleccionadas((prev) =>
      prev.includes(id) ? prev.filter((p) => p !== id) : [...prev, id]
    );
  }

  function seleccionarPerfume(id: number) {
    setResultado(null);
    setPerfumeSeleccionado(id);
  }

  async function handleCalcular() {
    if (prendasSeleccionadas.length === 0) {
      Alert.alert("Falta el outfit", "Selecciona al menos una prenda.");
      return;
    }
    if (perfumeSeleccionado === null) {
      Alert.alert("Falta el perfume", "Selecciona un perfume.");
      return;
    }

    setCalculando(true);
    try {
      const match = await apiPost<MatchResult>("/match", {
        prendasIds: prendasSeleccionadas,
        perfumeId: perfumeSeleccionado,
      });
      setResultado(match);
    } catch (error) {
      Alert.alert("Error al calcular", (error as Error).message);
    } finally {
      setCalculando(false);
    }
  }

  if (cargando) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" color="#5b8def" />
        <Text style={styles.loadingText}>Cargando tu armario...</Text>
      </View>
    );
  }

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <Text style={styles.titulo}>Recomendar perfume</Text>

      <Text style={styles.label}>Elige tu outfit (varias prendas)</Text>
      <View style={styles.chipRow}>
        {prendas.map((p) => (
          <TouchableOpacity
            key={p.idPrenda}
            style={[styles.chip, prendasSeleccionadas.includes(p.idPrenda) && styles.chipSelected]}
            onPress={() => togglePrenda(p.idPrenda)}
          >
            <Text
              style={[
                styles.chipText,
                prendasSeleccionadas.includes(p.idPrenda) && styles.chipTextSelected,
              ]}
            >
              {p.nombre}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      <Text style={styles.label}>Elige un perfume</Text>
      <View style={styles.chipRow}>
        {perfumes.map((perfume) => (
          <TouchableOpacity
            key={perfume.idPerfume}
            style={[
              styles.chip,
              perfumeSeleccionado === perfume.idPerfume && styles.chipSelected,
            ]}
            onPress={() => seleccionarPerfume(perfume.idPerfume)}
          >
            <Text
              style={[
                styles.chipText,
                perfumeSeleccionado === perfume.idPerfume && styles.chipTextSelected,
              ]}
            >
              {perfume.nombre}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      <TouchableOpacity
        style={[styles.boton, calculando && styles.botonDeshabilitado]}
        onPress={handleCalcular}
        disabled={calculando}
      >
        {calculando ? (
          <ActivityIndicator color="#fff" />
        ) : (
          <Text style={styles.botonTexto}>Calcular Match Score</Text>
        )}
      </TouchableOpacity>

      {resultado && (
        <View style={styles.resultado}>
          <Text style={styles.score}>{resultado.score} / 100</Text>
          {resultado.mensajes.map((m, i) => (
            <Text key={i} style={styles.mensaje}>
              • {m}
            </Text>
          ))}
        </View>
      )}
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
  resultado: {
    marginTop: 24,
    marginBottom: 40,
    backgroundColor: "#1e1e1e",
    borderRadius: 12,
    padding: 16,
  },
  score: {
    fontSize: 32,
    fontWeight: "800",
    color: "#5b8def",
    textAlign: "center",
    marginBottom: 12,
  },
  mensaje: {
    color: "#cccccc",
    fontSize: 14,
    marginBottom: 4,
  },
});
