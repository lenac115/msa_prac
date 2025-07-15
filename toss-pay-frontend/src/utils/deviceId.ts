import { v4 as uuidv4 } from 'uuid';

export function getOrCreateDeviceId(): string {
  if (typeof window === 'undefined') {
    // SSR 환경일 경우 로컬 스토리지 접근 불가
    return uuidv4(); // 임시 UUID 반환
  }

  let deviceId = localStorage.getItem('deviceId');
  if (!deviceId) {
    deviceId = uuidv4();
    localStorage.setItem('deviceId', deviceId);
  }
  return deviceId;
}
