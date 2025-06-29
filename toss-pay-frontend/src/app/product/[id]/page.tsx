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

type BuyButtonProps = {
    id: number;
    quantity: number;
    price: number;
};

export default function ProductDetailPage({params}: { params: Promise<{ id: number }> }) {

    const unwrappedParams = React.use(params); // Promise 해제
    const productId = unwrappedParams.id;
    const [product, setProduct] = useState<ProductDto | null>(null)
    const [error, setError] = useState(null);
    const router = useRouter();
    const [quantity, setQuantity] = useState<number>(0);

    useEffect(() => {
        async function fetchUser() {
            try {
                const response = await axios.get(`http://localhost:8080/product/common/get/${productId}`);
                setProduct(response.data);
            } catch (err: any) {
                console.error(err);
                setError(err.response?.data?.message || '올바르지 않은 상품 정보입니다.');
                alert('올바르지 않은 상품 정보입니다.');
                setProduct(null);
                setTimeout(() => {
                    router.push('/main');
                }, 2000);
            }
        }

        fetchUser();
    }, [productId]);

    return (
        <div className="p-6 flex flex-col gap-4 max-w-sm mx-auto mt-10">
            {product ? (<div><h1 className="text-2xl font-bold mb-2">{product.productName}</h1>
                <p className="mb-2">{product.description}</p>
                <p className="text-green-600 font-bold">{product.price.toLocaleString()}원</p>
                <p>{product.stock}개 남음</p></div>) : (<div className="text-red-600">상품 정보 없음</div>)
                }

            <form className="flex flex-col gap-4 max-w-sm mx-auto mt-10">
                <input
                    type="number"
                    placeholder="수량"
                    value={quantity}
                    onChange={(e) => setQuantity(Number(e.target.value))}
                    className="border p-2 rounded"
                    />
            </form>
            {/* 클라이언트 컴포넌트에 데이터 전달 */}
            <BuyButton id={product?.id ?? 0} price={product?.price ?? 0} quantity={quantity ?? 0}/>
        </div>
    );
}

function BuyButton({ id, price, quantity }: BuyButtonProps) {
        console.log(quantity);
        const router = useRouter();
        if(quantity < 0) {
            alert("수량은 비어선 안됩니다.");
            return;
        }

        const handleBuyProduct = async () => {
            try {
                const newOrder = [{
                    quantity: quantity,
                    amount: price,
                    productId: id
                }];
                
                const response = await axios.post(
                    `http://localhost:8080/order/common/create`, 
                    newOrder
                );
                
                //router.push(`/product/${id}/checkout?id=${response.data.id}`);
                router.push(`/product/ready?productId=${id}&orderId=${response.data.id}&orderEventId=${response.data.orderEventId}`)
            } catch (err) {
                console.error("주문 생성 오류:", err);
                setTimeout(() => router.push('/main'), 2000);
            }
        };

        return (
            <button onClick={handleBuyProduct} className="bg-blue-500 text-white p-2 rounded mt-4">
                상품 구매
            </button>
        );
}