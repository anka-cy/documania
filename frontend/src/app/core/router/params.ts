// Fusionne params de chemin et query d'une route Angular (même comportement
// que le routeur vanilla : { ...matchedParams, ...params }).

import { ActivatedRouteSnapshot } from '@angular/router';

export function pageParams(snapshot: ActivatedRouteSnapshot): Record<string, string> {
  const merged: Record<string, string> = { ...snapshot.params };
  for (const [key, value] of Object.entries(snapshot.queryParams)) {
    if (value !== undefined && value !== null) {
      merged[key] = Array.isArray(value) ? String(value[0]) : String(value);
    }
  }
  return merged;
}
