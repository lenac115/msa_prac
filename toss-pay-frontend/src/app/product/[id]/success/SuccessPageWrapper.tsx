'use client';
export const dynamic = 'force-dynamic';

import { useEffect, useState } from "react";
import { useRouter, useSearchParams, useParams } from "next/navigation";
import axios from '@/lib/axios';

export default function SuccessPageWrapper() {
    const navigate = useRouter();
    const searchParams = useSearchParams();
    const [orderId, setOrderId] = useState<string>();

    const params = useParams();
    const productId = params.id;

    useEffect(() => {
        const requestData = {
            orderId: searchParams.get("orderId"),
            amount: searchParams.get("amount"),
            paymentKey: searchParams.get("paymentKey"),
        };

        async function confirm() {
            try {
                const response = await axios.get(`http://3.105.113.69:8080/order/common/get?orderEventId=${requestData.orderId}`)
                console.log(response.data.orderEventId)
                setOrderId(response.data.orderEventId);

                try {
                    await axios.post(`http://3.105.113.69:8080/payment/confirm`, {
                        payToken: requestData.paymentKey,
                        orderEventId: response.data.orderEventId,
                        totalAmount: requestData.amount
                    });

                    if (window.opener) {
                        window.opener.postMessage({ type: 'payment_completed', paymentKey: requestData.paymentKey }, window.location.origin);
                    }
                    alert('결제 승인에 성공했습니다.');
                    window.close();
                } catch (err) {
                    console.error(err);
                    if (window.opener) {
                        window.opener.postMessage({ type: 'payment_failed', paymentKey: requestData.paymentKey }, window.location.origin);
                    }
                    alert('결제 승인에 실패했습니다.');
                    setTimeout(() => window.close(), 3000);
                }
            } catch (err) {
                if (window.opener) {
                    window.opener.postMessage({ type: 'payment_failed', paymentKey: requestData.paymentKey }, window.location.origin);
                }
                alert('주문 정보 검색에 실패했습니다.');
                setTimeout(() => window.close(), 3000);
            }
        }

        confirm();
    }, []);

    return (
        <div className="result wrapper">
            <div className="box_section">
                <h2>결제 진행중</h2>
                <p>{`주문번호: ${searchParams.get("orderId")}`}</p>
                <p>{`결제 금액: ${Number(searchParams.get("amount") ?? 0).toLocaleString()}원`}</p>
                <p>{`paymentKey: ${searchParams.get("paymentKey")}`}</p>
            </div>
        </div>
    );
}
