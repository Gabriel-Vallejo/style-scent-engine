import React from "react";
import { StyleSheet, Text, View } from "react-native";
import { DetalleMatch, MatchResult, PerfumeItem, TipoDetalle } from "../types";

interface Props {
  perfume: PerfumeItem;
  resultado: MatchResult;
  destacado?: boolean;
}

const ETIQUETA_TIPO: Record<TipoDetalle, string> = {
  BASE: "Base",
  COLOR: "Color",
  ESTILO: "Estilo",
  EXCLUSION: "Nota vetada",
};

export function colorPorScore(score: number): string {
  if (score >= 75) return "#4caf7d";
  if (score >= 50) return "#e0a93b";
  return "#e05757";
}

function veredicto(score: number): string {
  if (score >= 85) return "Combinación excelente";
  if (score >= 65) return "Buena combinación";
  if (score >= 45) return "Combinación aceptable";
  return "Mejor prueba con otro";
}

function colorPuntos(detalle: DetalleMatch): string {
  if (detalle.tipo === "BASE") return "#888888";
  return detalle.puntos >= 0 ? "#4caf7d" : "#e05757";
}

export default function MatchResultCard({ perfume, resultado, destacado }: Props) {
  const color = colorPorScore(resultado.score);
  const acotado = resultado.score !== resultado.scoreSinAcotar;

  return (
    <View style={[styles.card, destacado && { borderColor: color }]}>
      {destacado && <Text style={styles.eyebrow}>Te recomendamos</Text>}
      <Text style={styles.perfume}>{perfume.nombre}</Text>
      <Text style={styles.subtitulo}>
        {[perfume.marca, perfume.familia].filter(Boolean).join(" · ")}
      </Text>

      <View style={styles.scoreRow}>
        <Text style={[styles.score, { color }]}>{resultado.score}</Text>
        <Text style={styles.scoreMax}>/ 100</Text>
      </View>
      <View style={styles.barra}>
        <View style={[styles.barraRelleno, { width: `${resultado.score}%`, backgroundColor: color }]} />
      </View>
      <Text style={[styles.veredicto, { color }]}>{veredicto(resultado.score)}</Text>

      <Text style={styles.seccion}>Desglose</Text>
      {resultado.detalles.map((d, i) => (
        <View key={i} style={styles.detalle}>
          <View style={styles.detalleTexto}>
            <Text style={styles.detalleTipo}>{ETIQUETA_TIPO[d.tipo]}</Text>
            <Text style={styles.detalleDescripcion}>{d.descripcion}</Text>
          </View>
          <Text style={[styles.detallePuntos, { color: colorPuntos(d) }]}>
            {d.tipo === "BASE" ? d.puntos : `${d.puntos > 0 ? "+" : ""}${d.puntos}`}
          </Text>
        </View>
      ))}
      {resultado.detalles.length === 1 && (
        <Text style={styles.nota}>
          Ninguna prenda tiene sinergias con la familia {perfume.familia}.
        </Text>
      )}
      {acotado && (
        <Text style={styles.nota}>
          Suma real: {resultado.scoreSinAcotar} puntos (el score se limita a 0-100).
        </Text>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: "#1e1e1e",
    borderRadius: 12,
    padding: 16,
    borderWidth: 1,
    borderColor: "#333333",
  },
  eyebrow: {
    color: "#888888",
    fontSize: 12,
    fontWeight: "600",
    textTransform: "uppercase",
    letterSpacing: 1,
    marginBottom: 4,
  },
  perfume: {
    color: "#fff",
    fontSize: 20,
    fontWeight: "700",
  },
  subtitulo: {
    color: "#aaaaaa",
    fontSize: 14,
    marginTop: 2,
  },
  scoreRow: {
    flexDirection: "row",
    alignItems: "baseline",
    marginTop: 16,
  },
  score: {
    fontSize: 44,
    fontWeight: "800",
  },
  scoreMax: {
    color: "#888888",
    fontSize: 18,
    marginLeft: 6,
  },
  barra: {
    height: 8,
    borderRadius: 4,
    backgroundColor: "#333333",
    overflow: "hidden",
    marginTop: 8,
  },
  barraRelleno: {
    height: "100%",
    borderRadius: 4,
  },
  veredicto: {
    fontSize: 15,
    fontWeight: "600",
    marginTop: 8,
  },
  seccion: {
    color: "#cccccc",
    fontSize: 14,
    fontWeight: "600",
    marginTop: 20,
    marginBottom: 8,
  },
  detalle: {
    flexDirection: "row",
    alignItems: "center",
    paddingVertical: 8,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: "#333333",
  },
  detalleTexto: {
    flex: 1,
    paddingRight: 12,
  },
  detalleTipo: {
    color: "#888888",
    fontSize: 11,
    textTransform: "uppercase",
    letterSpacing: 0.5,
  },
  detalleDescripcion: {
    color: "#dddddd",
    fontSize: 14,
    marginTop: 2,
  },
  detallePuntos: {
    fontSize: 16,
    fontWeight: "700",
    fontVariant: ["tabular-nums"],
  },
  nota: {
    color: "#888888",
    fontSize: 13,
    marginTop: 12,
    fontStyle: "italic",
  },
});
