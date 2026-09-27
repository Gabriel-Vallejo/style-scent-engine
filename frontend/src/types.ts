export interface CatalogoItem {
  id: number;
  nombre: string;
}

export interface PrendaResponse {
  idPrenda: number;
  nombre: string;
  categoria: string;
  color: string;
  estilos: string[];
  fechaRegistro: string;
}

export interface PerfumeItem {
  idPerfume: number;
  nombre: string;
  marca: string;
  familia: string;
  notas: string[];
  estado: string;
}

export type TipoDetalle = "BASE" | "COLOR" | "ESTILO" | "EXCLUSION";

export interface DetalleMatch {
  tipo: TipoDetalle;
  descripcion: string;
  puntos: number;
}

export interface MatchResult {
  score: number;
  scoreSinAcotar: number;
  detalles: DetalleMatch[];
}

export interface Recomendacion {
  perfume: PerfumeItem;
  resultado: MatchResult;
}

export interface Sugerencia {
  id: number;
  nombre: string;
  confianza: number;
}

export interface AnalisisPrenda {
  categoria: Sugerencia | null;
  color: Sugerencia | null;
  alternativasColor: Sugerencia[];
  estilos: Sugerencia[];
  colorDominanteHex: string;
}
