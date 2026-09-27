import { File } from "expo-file-system";

// En el APK la URL se fija al compilar (EXPO_PUBLIC_API_URL en eas.json); en
// desarrollo con Expo Go se usa la IP del servidor en la red local.
const API_BASE_URL = process.env.EXPO_PUBLIC_API_URL ?? "http://192.168.1.44:8080/api";

export async function apiGet<T>(path: string): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`);
  if (!response.ok) {
    throw new Error(`Error ${response.status} al consultar ${path}`);
  }
  return response.json();
}

export async function apiPost<T>(path: string, body: unknown): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });

  const data = await response.json().catch(() => null);

  if (!response.ok) {
    const message = data?.message ?? `Error ${response.status} al enviar a ${path}`;
    throw new Error(message);
  }

  return data as T;
}

export async function apiPatch<T>(path: string, body: unknown): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });

  const data = await response.json().catch(() => null);

  if (!response.ok) {
    const message = data?.message ?? `Error ${response.status} al actualizar ${path}`;
    throw new Error(message);
  }

  return data as T;
}

export async function apiDelete(path: string): Promise<void> {
  const response = await fetch(`${API_BASE_URL}${path}`, { method: "DELETE" });

  if (!response.ok) {
    const data = await response.json().catch(() => null);
    const message = data?.message ?? `Error ${response.status} al borrar ${path}`;
    throw new Error(message);
  }
}

// Sube una foto como multipart/form-data. Desde el SDK 57 el fetch global es
// expo/fetch, que no acepta el clásico { uri, name, type } de React Native
// ("Unsupported FormDataPart implementation"): el archivo tiene que ser un File
// de expo-file-system, que implementa Blob y lleva su nombre y tipo.
export async function apiUploadImagen<T>(path: string, uri: string): Promise<T> {
  const formData = new FormData();
  formData.append("imagen", new File(uri));

  const response = await fetch(`${API_BASE_URL}${path}`, { method: "POST", body: formData });
  const data = await response.json().catch(() => null);

  if (!response.ok) {
    const message = data?.message ?? `Error ${response.status} al subir la imagen`;
    throw new Error(message);
  }

  return data as T;
}
