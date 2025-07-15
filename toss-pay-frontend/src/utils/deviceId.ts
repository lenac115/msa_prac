import { v4 as uuidv4 } from 'uuid';

export function getOrCreateDeviceId(): string {
  if (typeof window === 'undefined') return uuidv4();

  let deviceId = localStorage.getItem('deviceId');
  if (!deviceId) {
    deviceId = crypto.randomUUID();
    localStorage.setItem('deviceId', deviceId);
  }
  return deviceId;
}
