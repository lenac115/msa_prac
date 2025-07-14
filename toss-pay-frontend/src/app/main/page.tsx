'use client';

import React, {useEffect, useState, useRef} from "react";
import axios from '@/lib/axios';
import Link from 'next/link';
import {useRouter} from "next/navigation";
import { deleteCookie } from "cookies-next";


interface ProductInfo {
    id: number;
    productName: string;
    price: number;
    stock: number;
    description: string;
}

interface UserInfo {
    name: string;
    auth: string;
}

export default function mainPage() {

    const router = useRouter();
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);
    const [user, setUser] = useState<UserInfo | null>(null);
    const [products, setProducts] = useState<ProductInfo[]>([]);
    const hasFetched = useRef(false);

    useEffect(() => {
        if (hasFetched.current) return;
        hasFetched.current = true;

        const fetchProductAndUser = async () => {

            try {
                const response = await axios.get('http://3.105.113.69:8080/auth/common/get/me');
                setUser(response.data);
            } catch (err: any) {
                console.error(err);
                setError(err.response?.data?.message || '올바르지 않은 유저 정보입니다.');
                alert('올바르지 않은 유저 정보입니다.');
                deleteCookie("accessToken");
                setUser(null);
                try {
                    await router.push('/login');
                } catch (routerError) {
                    console.error('라우팅 실패:', routerError);
                    window.location.href = '/login'; // 최후의 수단으로 강제 이동
                }
                return;
            }
        
            try {
                const response = await axios.get('http://3.105.113.69:8080/product/common/get/list');
                console.log(response);
                const sorted = response.data.sort((a: { id: number; }, b: { id: number; }) => a.id - b.id); // id 기준 정렬
                setProducts(sorted);
            } catch (err: any) {
                console.error(err);
                setError(err.response?.data?.message || '제품 목록을 불러오지 못했습니다.');
                alert('제품 목록을 불러오지 못했습니다.');
                setTimeout(() => {
                    router.refresh();
                }, 2000);
            }
        }

        fetchProductAndUser();
    })

    const handleLogout = () => {
        deleteCookie('accessToken');
        router.push('/login');
    };

    const handleProductRegi = () => {
        router.push('/product/register');
    }
    
    const handleOrderList = () => {
        router.push('/order');
    }

    return (
        <div className="p-6">
            {/* 상단 유저 정보 + 로그아웃 */}
            <div className="flex justify-between items-center mb-6">
                {user ? (
                    <div>
                        <span className="text-lg font-semibold">안녕하세요, {user.name}님</span>
                        <span className="ml-3 text-sm text-gray-600">({user.auth})</span>
                    </div>
                ) : (
                    <div className="text-red-600">유저 로딩중</div>
                )}
                <button
                    onClick={handleLogout}
                    className="bg-red-500 text-white px-4 py-2 rounded hover:bg-red-600"
                >
                    로그아웃
                </button>
            </div>

            {/* 상품 리스트 */}
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4">
                {products.map((product) => (
                    <div key={product.id} className="border rounded-xl p-4 shadow">
                        <Link href={`/product/${product.id}`}>
                            <h2 className="text-xl font-semibold text-blue-600 hover:underline cursor-pointer">
                                {product.productName}
                            </h2>
                        </Link>
                        <p>{product.description}</p>
                        <p className="text-green-600 font-bold mt-2">{product.price.toLocaleString()}원</p>
                        <p>{product.stock}개</p>
                    </div>
                ))}
            </div>

            <footer className="flex justify-between items-center mb-6 margin-top-10px">
                {
                    user?.auth === 'SELLER' &&
                    (<button
                            onClick={handleProductRegi}
                            className="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600">
                            제품 등록
                        </button>
                    )
                }
                <button
                onClick={handleOrderList}
                className="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600">
                    구매 목록
                </button>
            </footer>
        </div>
    );
}