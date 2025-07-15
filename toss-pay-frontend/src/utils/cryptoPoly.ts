// src/utils/crypto-polyfill.ts
import { v4 as uuidv4 } from 'uuid';

// 1. 정확한 UUID 타입 정의
type UUID = `${string}-${string}-${string}-${string}-${string}`;

// 2. 전역 Crypto 인터페이스 확장
declare global {
    interface Window {
        crypto: {
            randomUUID?: () => UUID;
            getRandomValues?: <T extends ArrayBufferView | null>(array: T) => T;
        };
    }
}

// 3. 폴리필 구현
if (typeof window !== 'undefined') {
    if (!window.crypto) {
        // 읽기 전용 속성에 대한 안전한 할당
        Object.defineProperty(window, 'crypto', {
            value: {},
            writable: false,
            configurable: true,
            enumerable: true
        });
    }

    if (!window.crypto.randomUUID) {
        // 타입 안전한 구현
        window.crypto.randomUUID = (() => {
            return uuidv4() as UUID;
        }) as () => UUID;
    }
}