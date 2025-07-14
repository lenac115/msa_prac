'use client';

import React, { FormEvent, useState } from 'react';
import { useRouter } from 'next/navigation';
import axios from '@/lib/axios';

export default function ProductRegiPage() {

    const router = useRouter();
    const [productName, setName] = useState('');
    const [description, setDescription] = useState('');
    const [price, setPrice] = useState('');
    const [stock, setStock] = useState('');    
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);

    const handleProductRegi = async (e: FormEvent) => {
        e.preventDefault();
        setError(null);
        setSuccess(null);

        if(!productName) {
            setError('이름을 입력하세요.');
            return;
        }
        if(!description) {
            setError('설명을 입력하세요.');
            return;
        }
        if(!price) {
            setError('가격을 입력하세요.');
            return;
        }
        if(!stock) {
            setError('재고를 입력하세요.');
            return;
        }

        try {
            const response = await axios.post('http://3.105.113.69:8080/product/seller/new', {
                productName,
                description,
                price,
                stock
            });
            console.log('상품 등록 성공:', response.data);
            setSuccess('상품 등록 성공! 메인 페이지로 이동합니다.');
            setTimeout(() => {
                router.push('/main'); // 메인 페이지로 이동
            }, 2000); // 2초 후에 메인 페이지로 이동
        } catch (err: any) {
            console.error(err);
            setError(err.response?.data?.message || '상품 등록 실패');
            alert('상품 등록 실패');
        }
    }

    return (
        <form onSubmit={handleProductRegi} className="flex flex-col gap-4 max-w-sm mx-auto mt-10">
        <input
          type="text"
          placeholder="이름"
          value={productName}
          onChange={(e) => setName(e.target.value)}
          className="border p-2 rounded"
        />
        <input
          type="text"
          placeholder="설명"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          className="border p-2 rounded"
        />
        <input
          type="number"
          placeholder="재고"
          value={stock}
          onChange={(e) => setStock(e.target.value)}
          className="border p-2 rounded"
        />
        <input
          type="number"
          placeholder="가격"
          value={price}
          onChange={(e) => setPrice(e.target.value)}
          className="border p-2 rounded"
        />
        {error && <p className="text-red-500">{error}</p>}
        <button type="submit" className="bg-blue-500 text-white p-2 rounded">상품 등록</button>
      </form>
    )
}