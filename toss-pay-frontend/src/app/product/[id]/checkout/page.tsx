'use client';
export const dynamic = 'force-dynamic';


import {loadTossPayments, TossPaymentsWidgets, ANONYMOUS} from "@tosspayments/tosspayments-sdk";
import React, {useEffect, useState} from "react";
import { useRouter, useSearchParams } from "next/navigation";
import axios from '@/lib/axios';
import { setCookie } from "cookies-next";


function generateRandomString() {
    if (typeof window !== "undefined") {
        return window.btoa(Math.random().toString()).slice(0, 20);
    }
    return ""; // 서버 환경일 경우 기본값 반환
}

interface ProductDto {
    id: number;
    productName: string;
    price: number;
    stock: number;
    description: string;
}

interface UserInfo {
    name: string;
    money: number;
    auth: string;
    email: string;
    phone: string;
}

interface Amount {
    currency: string;
    value: number;
}

const clientKey = "test_gck_docs_Ovk5rk1EwkEbP0W43n07xlzm";
const customerKey = generateRandomString();


export default function CheckoutPage({params}: { params: Promise<{ id: string }> }) {
    const [amount, setAmount] = useState<Amount>({
        currency: "KRW",
        value: 0,
    });
    const [ready, setReady] = useState(false);
    const router = useRouter();
    const [widgets, setWidgets] = useState<TossPaymentsWidgets | null>(null);
    const [product, setProduct] = useState<ProductDto | null>(null);
    const [user, setUser] = useState<UserInfo | null>(null);
    const [error, setError] = useState<string | null>(null);
    const unwrappedParams = React.use(params); // Promise 해제
    const productId = unwrappedParams.id;
    const searchParams = useSearchParams();
    const orderIdParam = searchParams.get('orderId');
    const orderEventIdParam = searchParams.get('orderEventId');
    const paymentKey = searchParams.get('paymentKey');




    useEffect(() => {
        async function fetchPaymentWidgets() {
            // ------  결제위젯 초기화 ------
            const tossPayments = await loadTossPayments(clientKey);
            // 회원 결제
            const widgets = tossPayments.widgets({
                customerKey,
            });
            // 비회원 결제
            // const widgets = tossPayments.widgets({ customerKey: ANONYMOUS });

            setWidgets(widgets);
        }

        fetchPaymentWidgets();
    }, [clientKey, customerKey]);

    useEffect(() => {
        if (!productId) return;

        const fetchProduct = async () => {
            try {
                const response = await axios.get(`http://localhost:8080/product/common/get/${productId}`);
                setProduct(response.data);
                setAmount({
                    currency: "KRW",
                    value: response.data.price});
            } catch (err: any) {
                console.error(err);
                setError(err.response?.data?.message || '올바르지 않은 물품 정보입니다.');
                alert('올바르지 않은 물품 정보입니다.');
                setProduct(null);
                setTimeout(() => {
                    router.push('/main'); // 메인 페이지로 이동
                }, 2000); // 2초 후에 메인 페이지로 이동
            }
        };

        const fetchUser = async () => {
            try {
                const response = await axios.get('http://localhost:8080/auth/common/get/me');
                setUser(response.data);
            } catch (err: any) {
                console.error(err);
                setError(err.response?.data?.message || '올바르지 않은 유저 정보입니다.');
                alert('올바르지 않은 유저 정보입니다.');
                setUser(null);
                setTimeout(() => {
                    router.push('/login'); // 로그인 페이지로 이동
                }, 2000); // 2초 후에 로그인 페이지로 이동
            }
        };

        const readyPayment = async (amount: number) => {
            const status = "PENDING";
            const request = {
                orderEventId: orderEventIdParam,
                amount: amount,
                status: status,
                paymentKey: paymentKey
            }
            try {
                const response = await axios.post(`http://localhost:8080/payment/ready`, request);
            } catch (err: any) {
                console.error(err);
                setError(err.response?.data?.message || '올바르지 않은 결제 정보입니다.');
                alert('올바르지 않은 결제 정보입니다.');
                setTimeout(() => {
                    router.push('/main'); // 메인 페이지로 이동
                }, 2000); // 2초 후에 메인 페이지로 이동
            }
        }

        fetchProduct();
        fetchUser();
        readyPayment(amount.value);
    }, [productId]);

    useEffect(() => {
        async function renderPaymentWidgets() {
            if (widgets == null) {
                return;
            }
            // ------ 주문의 결제 금액 설정 ------
            await widgets.setAmount(amount);

            await Promise.all([
                // ------  결제 UI 렌더링 ------
                widgets.renderPaymentMethods({
                    selector: "#payment-method",
                    variantKey: "DEFAULT",
                }),
                // ------  이용약관 UI 렌더링 ------
                widgets.renderAgreement({
                    selector: "#agreement",
                    variantKey: "AGREEMENT",
                }),
            ]);

            setReady(true);
        }

        renderPaymentWidgets();
    }, [widgets]);

    useEffect(() => {
        if (widgets == null) {
            return;
        }
        console.log(amount)
        widgets.setAmount(amount);
    }, [widgets, amount]);
    

    return (
        <div className="wrapper">
            <div className="box_section">
                {/* 결제 UI */}
                <div id="payment-method"/>
                {/* 이용약관 UI */}
                <div id="agreement"/>
                {/* 쿠폰 체크박스 */}
                <div>
                    <div>
                        <label htmlFor="coupon-box">
                            <input
                                id="coupon-box"
                                type="checkbox"
                                aria-checked="true"
                                disabled={!ready}
                                onChange={(event) => {
                                    setAmount((prev) => ({
                                        ...prev,
                                        value: event.target.checked ? prev.value - 5_000 : prev.value + 5_000,
                                    }));
                                    // ------  주문서의 결제 금액이 변경되었을 경우 결제 금액 업데이트 ------
                                    //setAmount(event.target.checked ? amount - 5_000 : amount + 5_000);
                                }}
                            />
                            <span>5,000원 쿠폰 적용</span>
                        </label>
                    </div>
                </div>

                {/* 결제하기 버튼 */}
                <button
                    className="button"
                    disabled={!ready}
                    onClick={async () => {
                        try {
                            // ------ '결제하기' 버튼 누르면 결제창 띄우기 ------
                            // 결제를 요청하기 전에 orderId, amount를 서버에 저장하세요.
                            // 결제 과정에서 악의적으로 결제 금액이 바뀌는 것을 확인하는 용도입니다.
                            console.log(user)
                            const response = await widgets!.requestPayment({
                                orderId: orderEventIdParam ?? "",
                                orderName: product?.productName ?? "에러",
                                successUrl: window.location.origin + `/product/${productId}/success`,
                                failUrl: window.location.origin + `/product/${productId}/fail`,
                                customerEmail: user?.email,
                                customerName: user?.name,
                                customerMobilePhone: user?.phone,
                            });

                        } catch (error) {
                            // 에러 처리하기
                            console.error(error);
                        }
                    }}
                >
                    결제하기
                </button>
            </div>
        </div>
    );
}