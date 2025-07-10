"use client"
export const dynamic = 'force-dynamic';

import {useEffect, useState} from "react";
import {useRouter, useSearchParams} from "next/navigation";
import axios from '@/lib/axios';

export default function SuccessPage({params}: { params: Promise<{ id: number }> }) {
    const navigate = useRouter();
    const searchParams = useSearchParams();
    const [orderId, setOrderId] = useState<string>();

    useEffect(() => {
        // 쿼리 파라미터 값이 결제 요청할 때 보낸 데이터와 동일한지 반드시 확인하세요.
        // 클라이언트에서 결제 금액을 조작하는 행위를 방지할 수 있습니다.
        const requestData = {
            orderId: searchParams.get("orderId"),
            amount: searchParams.get("amount"),
            paymentKey: searchParams.get("paymentKey"),
        };

        async function confirm() {
            try {
                const response = await axios.get(`http://localhost:8080/order/common/get?orderEventId=${requestData.orderId}`)
                console.log(response.data.orderEventId)
                setOrderId(response.data.orderEventId);
                try {
                    axios.post(`http://localhost:8080/payment/confirm`, {
                        payToken: requestData.paymentKey,
                        orderEventId: response.data.orderEventId,
                        totalAmount: requestData.amount
                    })
                    if (window.opener) {
                        window.opener.postMessage({ type: 'payment_completed', paymentKey: requestData.paymentKey }, window.location.origin);
                    }
                    alert('결제 승인에 성공했습니다.');
                    window.close();
                } catch (err) {
                    console.error(err);
                    if (window.opener) {
                        window.opener.postMessage({type: 'payment_failed', paymentKey: requestData.paymentKey }, window.location.origin);
                    }
                    alert('결제 승인에 실패했습니다.');
                    setTimeout(() => {
                        window.close();
                    }, 3000);
                }
            } catch (err) {
                if (window.opener) {
                    window.opener.postMessage({type: 'payment_failed', paymentKey: requestData.paymentKey }, window.location.origin);
                }
                alert('주문 정보 검색에 실패했습니다.');
                setTimeout(() => {
                    window.close();
                }, 3000);
            }
        }

        confirm();
    }, []);

    return (
        <div className="result wrapper">
            <div className="box_section">
                <h2>
                    결제 진행중
                </h2>
                <p>{`주문번호: ${searchParams.get("orderId")}`}</p>
                <p>{`결제 금액: ${Number(
                    searchParams.get("amount")
                ).toLocaleString()}원`}</p>
                <p>{`paymentKey: ${searchParams.get("paymentKey")}`}</p>
            </div>
        </div>
    );
}