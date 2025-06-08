import {getOrCreateDeviceId} from '@/utils/deviceId';

export async function authFetch(
    input: RequestInfo,
    init: RequestInit = {}
): Promise<Response> {
    const accessToken = localStorage.getItem("accessToken");
    const deviceId = getOrCreateDeviceId();

    const headers: HeadersInit = {
        'Content-Type': 'application/json',
        'X-Device-Id': deviceId,
        ...(accessToken ? {Authorization: `Bearer ${accessToken}`} : {}),
        ...(init.headers || {}),
    };

    const response = await fetch(input, {
        ...init,
        headers,
        credentials: 'include',
    });

    // 응답 헤더에서 새 토큰이 있으면 갱신
    const newAccessToken = response.headers.get("new-access-token");
    if (newAccessToken) {
        localStorage.setItem("accessToken", newAccessToken);
    }

    return response;
}