'use client';

import { useEffect, useRef } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import axios from '@/lib/axios';

function generateRandomString() {
    if (typeof window !== "undefined") {
        return window.btoa(Math.random().toString()).slice(0, 20);
    }
    return ""; // 서버 환경일 경우 기본값 반환
}

export default function PaymentButton() {
    const router = useRouter();
    const searchParams = useSearchParams();
    const orderId = searchParams.get('orderId');
    const productId = searchParams.get('productId');
    const orderEventId = searchParams.get('orderEventId');
    const paymentKey = generateRandomString();
    let gotMessage = useRef(false);

    async function cancelPayment() {
        try {
            axios.post(`https://msa-prac.duckdns.org/payment/delete/${paymentKey}`);
            alert(`결제가 취소되었습니다.`)
            router.push(`/main`)
        } catch(err) {
            alert('결제가 존재하지 않습니다.');
            router.push(`/main`)
        }
    }

    useEffect(() => {

        const newWindow = window.open(
            `/product/${productId}/checkout?orderId=${orderId}&orderEventId=${orderEventId}&paymentKey=${paymentKey}`,
            'TossPayments',
            'width=500,height=800'
        );
        if (!newWindow) {
            alert('팝업이 차단되었습니다. 팝업 허용 후 다시 시도해주세요.');
            router.push(`/product/${productId}`);
        }

        const checkInterval = setInterval(() => {
            if (newWindow && newWindow.closed) {
                clearInterval(checkInterval);
                if (!gotMessage.current) {
                    gotMessage.current = true;
                    cancelPayment();
                }
            }
        }, 500);

        // 메시지 리스너 등록
        const handleMessage = (event: MessageEvent) => {
            if (event.data.type === 'payment_completed') {
                gotMessage.current = true;
                console.log("성공")
                // 새 창 닫기
                newWindow?.close();
                clearInterval(checkInterval)
                // 원래 창에서 페이지 이동
                router.push('/main');
            } else if(event.data.type === 'payment_failed') {
                gotMessage.current = true;
                console.log("에러");
                // 새 창 닫기
                newWindow?.close();
                clearInterval(checkInterval)
                cancelPayment();
            }
        };

        window.addEventListener('message', handleMessage);

        // 컴포넌트 언마운트 시 리스너 제거
        return () => {
            window.removeEventListener('message', handleMessage);
        };
    }, []);

    return (
        <div>
            결제 대기중
        </div>
    );
}