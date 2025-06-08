'use client';

import React, {useState} from 'react';
import {useRouter} from 'next/navigation';
import axios from '@/lib/axios';

type AuthType = "BUYER" | "SELLER";
const AUTH_MAP = {
    "구매자": "BUYER",
    "판매자": "SELLER",
} as const;

export default function SignupPage() {

    const router = useRouter();
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [name, setName] = useState('');
    const [phone, setPhone] = useState('');
    const [address, setAddress] = useState('');
    const [birthDay, setBirthDay] = useState('');
    const [auth, setAuth] = useState('');
    const authType = ["구매자", "판매자"]
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);
        setSuccess(null);

        if (!email) {
            setError('이메일을 입력하세요.');
            return;
        }
        if (!password) {
            setError('비밀번호를 입력하세요.');
            return;
        }
        if (!confirmPassword) {
            setError('비밀번호 확인을 입력하세요.');
            return;
        }
        if (!name) {
            setError('이름을 입력하세요.');
            return;
        }
        if (!phone) {
            setError('전화번호를 입력하세요.');
            return;
        }
        if (!address) {
            setError('주소를 입력하세요.');
            return;
        }
        if (!birthDay) {
            setError('생년월일을 입력하세요.');
            return;
        }
        if (!auth) {
            setError('권한을 선택하세요.');
            return;
        }


        if (password !== confirmPassword) {
            setError('비밀번호가 일치하지 않습니다.');
        }

        if (auth.includes('구매자')) {
            setAuth('SELLER');
        } else if (auth.includes('판매자')) {
            setAuth('BUYER');
        }
        console.log(auth)
        try {
            const response = await axios.post('http://localhost:8080/auth/public/register', {
                email,
                password,
                name,
                phone,
                address,
                birthDay,
                auth
            });
            console.log('회원가입 성공:', response.data);
            setSuccess('회원가입 성공! 로그인 페이지로 이동합니다.');
            setTimeout(() => {
                router.push('/login'); // 로그인 페이지로 이동
            }, 2000); // 2초 후에 로그인 페이지로 이동
        } catch (err: any) {
            console.error(err);
            setError(err.response?.data?.message || '회원가입 실패');
        }
    };


    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const value = e.target.value;
        const mappedValue = AUTH_MAP[value as keyof typeof AUTH_MAP] || value;
        setAuth(mappedValue);
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
            <input
                type="password"
                placeholder="비밀번호 확인"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                className="border p-2 rounded"
            />
            <input
                type="text"
                placeholder="이름"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="border p-2 rounded"
            />
            <input
                type="text"
                placeholder="전화번호"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                className="border p-2 rounded"
            />
            <input
                type="text"
                placeholder="주소"
                value={address}
                onChange={(e) => setAddress(e.target.value)}
                className="border p-2 rounded"
            />
            <input
                type="date"
                placeholder="생년월일"
                value={birthDay}
                onChange={(e) => setBirthDay(e.target.value)}
                className="border p-2 rounded"
            />
            <label>권한
                {authType.map((type) => (
                    <div key={type}>
                        <input
                            type="checkbox"
                            value={type}
                            checked={auth === (AUTH_MAP[type as keyof typeof AUTH_MAP] || type)}
                            onChange={handleChange}
                        />
                        {type}
                    </div>
                ))}
            </label>
            {error && <p className="text-red-500">{error}</p>}
            <button type="submit" className="bg-blue-500 text-white p-2 rounded">회원가입</button>
        </form>
    )
}