const API_BASE_URL = "http://192.168.1.44:8080/api";

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

// Sube una foto como multipart/form-data. En React Native el "archivo" es un
// objeto { uri, name, type } que fetch lee del disco del móvil.
export async function apiUploadImagen<T>(path: string, uri: string, mimeType?: string | null): Promise<T> {
  const nombre = uri.split("/").pop() ?? "foto.jpg";
  const formData = new FormData();
  formData.append("imagen", { uri, name: nombre, type: mimeType ?? "image/jpeg" } as unknown as Blob);

  const response = await fetch(`${API_BASE_URL}${path}`, { method: "POST", body: formData });
  const data = await response.json().catch(() => null);

  if (!response.ok) {
    const message = data?.message ?? `Error ${response.status} al subir la imagen`;
    throw new Error(message);
  }

  return data as T;
}
