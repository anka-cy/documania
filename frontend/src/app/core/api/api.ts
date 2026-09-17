// Facade fonctionnelle du client API : mêmes exports que la version vanilla,
// délègue à l'ApiService racine.

import { getApi } from '../singletons';
import { ApiFetchOptions } from './api.service';

// Ré-exporté pour les pages qui affichent les erreurs (ex. login : instanceof ApiError).
export { ApiError } from '../interceptors/error.interceptor';

export function apiFetch(path: string, options: ApiFetchOptions = {}): Promise<any> {
  return getApi().apiFetch(path, options);
}

export function download(path: string): Promise<{ blob: Blob; filename: string | null }> {
  return getApi().download(path);
}

export function apiUpload(path: string, file: File): Promise<any> {
  return getApi().apiUpload(path, file);
}

export function get(path: string, options: ApiFetchOptions = {}): Promise<any> {
  return getApi().get(path, options);
}

export { saveBlob } from './api.service';
