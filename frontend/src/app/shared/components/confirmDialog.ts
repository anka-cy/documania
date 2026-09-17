// Boîte de confirmation avant action destructive ou sensible, basée sur la
// modale Bootstrap. Renvoie true (confirmé) ou false (annulé).

let dialogId = 0;

interface ConfirmOptions {
  title?: string;
  okText?: string;
  cancelText?: string;
  /** 'primary' (défaut) ou 'danger' pour les suppressions. */
  okVariant?: string;
}

/**
 * Affiche une boîte de confirmation.
 * @param message Question posée à l'utilisateur. C'est un gabarit HTML
 *                (supporte <strong>, <br>, <span>…) : toute donnée
 *                utilisateur doit y être passée via escapeHtml() avant l'appel.
 * @param options { title, okText, cancelText, okVariant }
 * @returns résolue avec true si l'utilisateur confirme.
 */
export function confirmAction(message: string, options: ConfirmOptions = {}): Promise<boolean> {
  const title = options.title || 'Confirmation';
  const okText = options.okText || 'Confirmer';
  const cancelText = options.cancelText || 'Annuler';
  const okVariant = options.okVariant || 'primary'; // 'danger' pour les suppressions

  return new Promise((resolve) => {
    const id = `documania-confirm-${++dialogId}`;
    const backdrop = document.createElement('div');
    backdrop.innerHTML = `
      <div class="modal fade" id="${id}" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered modal-fullscreen-sm-down">
          <div class="modal-content">
            <div class="modal-header">
              <h5 class="modal-title">${title}</h5>
              <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Fermer"></button>
            </div>
            <div class="modal-body">${message}</div>
            <div class="modal-footer">
              <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">${cancelText}</button>
              <button type="button" class="btn btn-${okVariant}" id="${id}-ok">${okText}</button>
            </div>
          </div>
        </div>
      </div>
    `;
    document.body.appendChild(backdrop);

    const modalElement = backdrop.querySelector('.modal') as HTMLElement;
    const modal = bootstrap.Modal.getOrCreateInstance(modalElement);
    let settled = false;

    const finish = (result: boolean) => {
      if (settled) return;
      settled = true;
      modal.hide();
      resolve(result);
    };

    // Bootstrap gère déjà Échap, le clic extérieur, « Annuler » et la croix :
    // à la fermeture sans confirmation explicite, on résout « false ».
    modalElement.addEventListener('hidden.bs.modal', () => {
      backdrop.remove();
      if (!settled) {
        settled = true;
        resolve(false);
      }
    });
    backdrop.querySelector(`#${id}-ok`)!.addEventListener('click', () => finish(true));
    modal.show();
  });
}
