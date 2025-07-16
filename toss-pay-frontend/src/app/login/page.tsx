'use client';

import React, {useState} from 'react';
import axios from '@/lib/axios';
import {useRouter} from 'next/navigation';
import { setCookie, getCookie, deleteCookie, hasCookie } from 'cookies-next';

export default function LoginPage() {
    const router = useRouter();
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState<string | null>(null);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);
        try {
            const response = await axios.post('http://13.209.93.165:8080/auth/public/login', {
                    email,
                    password,
                }, {
                    headers: {
                        'x-ignore-interceptor': 'true',
                    },
                }
            );
            setCookie('accessToken', response.data.accessToken, {
                path: '/',
                maxAge: 60 * 60,
            });   
            console.log('로그인 성공:', response.data);
            router.push('/main'); // 메인 페이지로 이동
        } catch (err: any) {
            console.error(err);
            setError(err.response?.data?.message || '로그인 실패');
            alert('로그인 실패');
        }
    };

    if(hasCookie('accessToken')) {
        router.push('/main');
    }

    const handleGoToSignup = () => {
        router.push('/signup');
    };

    return (
        <form onSubmit={handleSubmit} className="flex flex-col gap-4 max-w-sm mx-auto mt-10">
            <input
                type="email"
                placeholder="이메일"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="border p-2 rounded"
            />
            <input
                type="password"
                placeholder="비밀번호"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="border p-2 rounded"
            />
            {error && <p className="text-red-500">{error}</p>}
            <button type="submit" className="bg-blue-500 text-white p-2 rounded">로그인</button>
            <button
                type="button"
                onClick={handleGoToSignup}
                className="bg-gray-500 text-white p-2 rounded"
            >
                회원가입
            </button>
        </form>
    );
  }