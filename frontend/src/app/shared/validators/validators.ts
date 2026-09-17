// Validation simple côté client (confort) : le BACKEND reste l'autorité
// finale. La taille des mots de passe (12 à 72) reprend les règles backend.

type ValidateFn = (value: any, form?: Record<string, any>) => string | null;

/** Règles de base pour les champs courants. */
export const rules = {
  required: (label: string) => (value: any): string | null =>
    value === null || value === undefined || String(value).trim() === ''
      ? `${label} est obligatoire.`
      : null,

  email: (label: string) => (value: any): string | null => {
    if (!value) return null;
    const valid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(String(value).trim());
    return valid ? null : `${label} doit être une adresse e-mail valide.`;
  },

  password: (value: any): string | null => {
    if (!value) return 'Le mot de passe est obligatoire.';
    const length = String(value).length;
    if (length < 12) return 'Le mot de passe doit contenir au moins 12 caractères.';
    if (length > 72) return 'Le mot de passe ne peut pas dépasser 72 caractères.';
    return null;
  },

  matches: (label: string, otherName: string) => (value: any, form: any): string | null =>
    value !== form[otherName] ? `${label} doit être identique.` : null,
};

/**
 * Valide un formulaire : exécute toutes les règles de chaque champ.
 * @returns { errors: { champ: message }, isValid }
 */
export function validateForm(
  formValues: Record<string, any>,
  fieldRules: Record<string, ValidateFn[]>,
): { errors: Record<string, string>; isValid: boolean } {
  const errors: Record<string, string> = {};
  for (const [field, fieldValidators] of Object.entries(fieldRules)) {
    for (const validate of fieldValidators) {
      const message = validate(formValues[field], formValues);
      if (message) {
        errors[field] = message;
        break;
      }
    }
  }
  return { errors, isValid: Object.keys(errors).length === 0 };
}

/**
 * Applique visuellement les erreurs de validation d'un formulaire.
 * Chaque {@code .form-field-error} positionnée après le champ est remplie.
 */
export function renderFormErrors(formElement: HTMLFormElement, errors: Record<string, string>): void {
  formElement.querySelectorAll<HTMLElement>('.form-field-error').forEach((node) => {
    node.textContent = '';
  });
  formElement.querySelectorAll<HTMLElement>('.is-invalid').forEach((node) => {
    node.classList.remove('is-invalid');
  });
  for (const [field, message] of Object.entries(errors)) {
    const control = formElement.elements.namedItem(field) as HTMLElement | null;
    if (!control) continue;
    control.classList.add('is-invalid');
    const errorNode = control.parentElement
      ? control.parentElement.querySelector('.form-field-error')
      : null;
    if (errorNode) errorNode.textContent = message;
  }
}
