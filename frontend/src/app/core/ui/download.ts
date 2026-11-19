/** Dispara la descarga de un blob en el navegador sin abrir una pestana nueva. */
export function saveBlob(blob: Blob, filename: string): void {
  if (typeof URL.createObjectURL !== 'function') {
    return;
  }
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  anchor.rel = 'noopener';
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(url);
}
