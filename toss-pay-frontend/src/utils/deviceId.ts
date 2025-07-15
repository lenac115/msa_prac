import { v4 as uuidv4 } from 'uuid';

export function getOrCreateDeviceId(): string {
  if (typeof window === 'undefined') return uuidv4();

  // 브라우저에 crypto.randomUUID 없으면 polyfill
  if (typeof crypto.randomUUID !== 'function') {
    (crypto as any).randomUUID = () =>
        ([1e7]+-1e3+-4e3+-8e3+-1e11).replace(/[018]/g, c =>
            (parseInt(c) ^ crypto.getRandomValues(new Uint8Array(1))[0] & 15 >> parseInt(c) / 4).toString(16)
        );
  }

  let deviceId = localStorage.getItem('deviceId');
  if (!deviceId) {
    deviceId = crypto.randomUUID();
    localStorage.setItem('deviceId', deviceId);
  }
  return deviceId;
}
