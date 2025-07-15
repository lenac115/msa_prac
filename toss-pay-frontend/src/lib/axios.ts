import axios from 'axios';
import {getOrCreateDeviceId} from '@/utils/deviceId';
import {getCookie, setCookie} from 'cookies-next';

const instance = axios.create({
    baseURL: process.env.NEXT_PUBLIC_API_BASE_URL, // 환경변수에서 API 주소 가져오기
    withCredentials: true, // 필요하다면
});

instance.interceptors.request.use(
    (config) => {
        config.headers['X-Device-Id'] = getOrCreateDeviceId();

        if (config.headers['x-ignore-interceptor']) {
            delete config.headers['x-ignore-interceptor'];
            return config;
        }
        const accessToken = getCookie('accessToken');
        if (accessToken) {
            config.headers['Authorization'] = `Bearer ${accessToken}`;
        }

        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

instance.interceptors.response.use(
    (response) => {
        const newAccessToken = response.headers['new-access-token'];
        if (newAccessToken) {
            setCookie('accessToken', newAccessToken, {
                path: '/',
                maxAge: 60 * 60,
            });        
        }
        return response;
    },
    (error) => {
        return Promise.reject(error);
    }
);

export default instance;
