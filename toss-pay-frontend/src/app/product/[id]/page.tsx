'use client';

import {useRouter} from "next/navigation";
import React, {useEffect, useState} from 'react';
import axios from '@/lib/axios';

interface ProductDto {
    id: number;
    productName: string;
    price: number;
    stock: number;
    description: string;
}

export default function ProductDetailPage({params}: { params: Promise<{ id: string }> }) {

    const unwrappedParams = React.use(params); // Promise 해제
    const productId = unwrappedParams.id;
    const [product, setProduct] = useState<ProductDto | null>(null)
    const [error, setError] = useState(null);
    const router = useRouter();

    async function fetchUser() {
         try {
                const response = await axios.get(`http://localhost:8080/product/common/get/${productId}`);
                setProduct(response.data)
            } catch (err: any) {
                console.error(err);
                setError(err.response?.data?.message || '올바르지 않은 상품 정보입니다.');
                setProduct(null);
                setTimeout(() => {
                    router.push('/main'); // 메인 페이지로 이동
                }, 2000); // 2초 후에 메인 페이지로 이동
            }

    }

    fetchUser();

    return (
        <div className="p-6">
            {product ? (<div><h1 className="text-2xl font-bold mb-2">{product.productName}</h1>
                <p className="mb-2">{product.description}</p>
                <p className="text-green-600 font-bold">{product.price.toLocaleString()}원</p>
                <p>{product.stock}개 남음</p></div>) : (<div className="text-red-600">상품 정보 없음</div>)}

            {/* 클라이언트 컴포넌트에 데이터 전달 */}
            <BuyButton id={productId}/>
        </div>
    );
}

function BuyButton({id}: { id: string }) {
    const router = useRouter();

    const handleBuyProduct = () => {
        router.push(`/product/${id}/checkout`);
    };

    return (
        <button onClick={handleBuyProduct} className="bg-blue-500 text-white p-2 rounded mt-4">
            상품 구매
        </button>
    );
}