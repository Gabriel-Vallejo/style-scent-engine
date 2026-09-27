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

export interface MatchResult {
  score: number;
  mensajes: string[];
}
