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
import MatchResultCard, { colorPorScore } from "../components/MatchResultCard";
import { MatchResult, PerfumeItem, PrendaResponse, Recomendacion } from "../types";

// Cuántas alternativas se enseñan bajo la recomendación principal
const ALTERNATIVAS_VISIBLES = 4;

export default function RecomendarPerfumeScreen() {
  const [prendas, setPrendas] = useState<PrendaResponse[]>([]);
  const [perfumes, setPerfumes] = useState<PerfumeItem[]>([]);

  const [prendasSeleccionadas, setPrendasSeleccionadas] = useState<number[]>([]);
  const [perfumeSeleccionado, setPerfumeSeleccionado] = useState<number | null>(null);

  const [cargando, setCargando] = useState(true);
  const [calculando, setCalculando] = useState(false);
  // Resultado que se está enseñando en la tarjeta grande
  const [resultado, setResultado] = useState<Recomendacion | null>(null);
  // Solo existe en modo "recomiéndame": ranking completo de mejor a peor
  const [ranking, setRanking] = useState<Recomendacion[] | null>(null);

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
        } catch {
          Alert.alert("Error", "No se pudieron cargar tus prendas y perfumes.");
        } finally {
          setCargando(false);
        }
      }
      cargarDatos();
    }, [])
  );

  function limpiarResultado() {
    setResultado(null);
    setRanking(null);
  }

  function togglePrenda(id: number) {
    limpiarResultado();
    setPrendasSeleccionadas((prev) =>
      prev.includes(id) ? prev.filter((p) => p !== id) : [...prev, id]
    );
  }

  // Volver a pulsar el perfume elegido lo deselecciona (y se vuelve a modo "recomiéndame")
  function togglePerfume(id: number) {
    limpiarResultado();
    setPerfumeSeleccionado((prev) => (prev === id ? null : id));
  }

  async function handleCalcular() {
    if (prendasSeleccionadas.length === 0) {
      Alert.alert("Falta el outfit", "Selecciona al menos una prenda.");
      return;
    }

    setCalculando(true);
    try {
      if (perfumeSeleccionado === null) {
        const lista = await apiPost<Recomendacion[]>("/match/recomendar", {
          prendasIds: prendasSeleccionadas,
        });
        setRanking(lista);
        setResultado(lista[0] ?? null);
      } else {
        const match = await apiPost<MatchResult>("/match", {
          prendasIds: prendasSeleccionadas,
          perfumeId: perfumeSeleccionado,
        });
        const perfume = perfumes.find((p) => p.idPerfume === perfumeSeleccionado)!;
        setRanking(null);
        setResultado({ perfume, resultado: match });
      }
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

  const mejor = ranking?.[0] ?? null;
  const alternativas = ranking ? ranking.slice(1, 1 + ALTERNATIVAS_VISIBLES) : [];

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <Text style={styles.titulo}>Recomendar perfume</Text>

      <Text style={styles.label}>Elige tu outfit (varias prendas)</Text>
      {prendas.length === 0 ? (
        <Text style={styles.vacio}>Aún no tienes prendas. Regístralas en la pestaña Prenda.</Text>
      ) : (
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
      )}

      <Text style={styles.label}>Perfume (opcional: si no eliges, te recomendamos el mejor)</Text>
      {perfumes.length === 0 ? (
        <Text style={styles.vacio}>
          No tienes perfumes en tu colección. Regístralos en la pestaña Perfume.
        </Text>
      ) : (
        <View style={styles.chipRow}>
          {perfumes.map((perfume) => (
            <TouchableOpacity
              key={perfume.idPerfume}
              style={[
                styles.chip,
                perfumeSeleccionado === perfume.idPerfume && styles.chipSelected,
              ]}
              onPress={() => togglePerfume(perfume.idPerfume)}
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
      )}

      <TouchableOpacity
        style={[styles.boton, calculando && styles.botonDeshabilitado]}
        onPress={handleCalcular}
        disabled={calculando}
      >
        {calculando ? (
          <ActivityIndicator color="#fff" />
        ) : (
          <Text style={styles.botonTexto}>
            {perfumeSeleccionado === null ? "Recomiéndame un perfume" : "Calcular Match Score"}
          </Text>
        )}
      </TouchableOpacity>

      {resultado && (
        <View style={styles.resultado}>
          <MatchResultCard
            perfume={resultado.perfume}
            resultado={resultado.resultado}
            destacado={mejor !== null && resultado.perfume.idPerfume === mejor.perfume.idPerfume}
          />

          {ranking && ranking.length > 1 && (
            <>
              <Text style={styles.label}>Otras opciones (toca para ver su desglose)</Text>
              {[mejor!, ...alternativas]
                .filter((r) => r.perfume.idPerfume !== resultado.perfume.idPerfume)
                .map((r) => (
                  <TouchableOpacity
                    key={r.perfume.idPerfume}
                    style={styles.alternativa}
                    onPress={() => setResultado(r)}
                  >
                    <View style={styles.alternativaTexto}>
                      <Text style={styles.alternativaNombre}>{r.perfume.nombre}</Text>
                      <Text style={styles.alternativaFamilia}>{r.perfume.familia}</Text>
                    </View>
                    <Text
                      style={[styles.alternativaScore, { color: colorPorScore(r.resultado.score) }]}
                    >
                      {r.resultado.score}
                    </Text>
                  </TouchableOpacity>
                ))}
            </>
          )}
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
  },
  vacio: {
    color: "#888888",
    fontSize: 14,
    fontStyle: "italic",
  },
  alternativa: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: "#1e1e1e",
    borderRadius: 10,
    paddingHorizontal: 14,
    paddingVertical: 12,
    marginBottom: 8,
  },
  alternativaTexto: {
    flex: 1,
    paddingRight: 12,
  },
  alternativaNombre: {
    color: "#fff",
    fontSize: 15,
    fontWeight: "600",
  },
  alternativaFamilia: {
    color: "#888888",
    fontSize: 13,
    marginTop: 2,
  },
  alternativaScore: {
    fontSize: 20,
    fontWeight: "800",
    fontVariant: ["tabular-nums"],
  },
});
