'use client'
import { useState, useEffect, useRef } from 'react';
import axios from '@/lib/axios';
import { useRouter } from 'next/navigation'
import { deleteCookie } from 'cookies-next';
import Link from 'next/link';

interface UserInfo {
    id: number;
    name: string;
    auth: string;
}

interface OrderInfo {
    id: number;
    status: string;
    productId: number[];
    orderEventId: number;
}

interface ProductInfo {
    id: number;
    productName: string;
    price: number;
    stock: number;
    description: string;
}

interface OrderItemInfo {
    id: number;
    price: number;
    quantity: number;
    orderId: number;
    productId: number;
}

export default function orderPage() {

    const router = useRouter();
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);
    const [user, setUser] = useState<UserInfo | null>(null);
    const [order, setOrder] = useState<OrderInfo[]>([]);
    let fetchedProducts: ProductInfo[] = [];
    const [products, setProducts] = useState<ProductInfo[]>([]);
    const [orderItems, setOrderItems] = useState<OrderItemInfo[]>([]);

    const hasFetched = useRef(false);

    useEffect(() => {
        if (hasFetched.current) return;
        hasFetched.current = true;

        const fetchProductAndUser = async () => {
            try {
                const userResponse = await axios.get('http://13.209.93.165:8080/auth/common/get/me');
                const userData = userResponse.data;
                setUser(userData);

                const orderResponse = await axios.get(`http://13.209.93.165:8080/order/common/get/list/${userData.id}`);
                const orderList = orderResponse.data.sort((a: any, b: any) => a.id - b.id);
                setOrder(orderList);

                const orderItemResponses = await Promise.all(
                    orderList.map((order: any) =>
                    axios.get(`http://13.209.93.165:8080/order/common/get/orderItem/${order.id}`))
                );
                const allOrderItems = orderItemResponses.flatMap(res => res.data);
                setOrderItems(allOrderItems)

                const allProductIds = allOrderItems.map(item => item.productId);
                const uniqueProductIds = [...new Set(allProductIds)];

                const productResponses = await Promise.all(
                    uniqueProductIds.map((id) =>
                    axios.get(`http://3.105.113.69:8080/product/common/get/${id}`)
                    )
                );

                const products = productResponses.map((res) => res.data);

                setProducts(products);
            } catch (err: any) {
                console.error(err);
                setError(err.response?.data?.message || '정보를 불러오는 중 오류가 발생했습니다.');
                alert('정보를 불러오는 중 오류가 발생했습니다.');
                setUser(null);
                setTimeout(() => {
                    router.push('/login');
                }, 2000);
            }
        };

        fetchProductAndUser();
    }, []);

    const handleLogout = () => {
        deleteCookie('accessToken');
        router.push('/login');
    };
    async function handleCancelOrder(orderId: number) {
        try {
            axios.post(`http://3.105.113.69:8080/order/common/cancel?orderId=${orderId}`);
            alert(`주문이 취소되었습니다.`)
            router.refresh();
        } catch {
            alert(`주문 취소에 실패하였습니다.`)
            router.refresh();
        }
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

            {/* 주문 리스트 */}
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4">
                {order.map((order) => {
                    const matchedProducts = products.filter((product) => 
                        order.productId.includes(product.id)
                    );

                    const matchedOrderedItem = orderItems.filter((orderItem) =>
                        order.id === orderItem.orderId
                    );

                    const productNameDisplay = matchedProducts.length > 1
                        ? `${matchedProducts[0].productName} 외 ${matchedProducts.length - 1}개`
                        : matchedProducts[0]?.productName || "상품 없음";

                    const totalPrice = matchedOrderedItem.reduce((sum, orderItem) => sum + orderItem.price, 0);
                    const totalStock = matchedOrderedItem.reduce((sum, orderItem) => sum + orderItem.quantity, 0);

                    if (!products || products.length === 0) {
                        return <div key={order.id}>상품 정보를 불러오는 중입니다...</div>;
                    } else {

                    return (
                        <div key={order.id} className="border rounded-xl p-4 shadow">
                            <Link href={`/order/${order.id}`}>
                                <h2 className="text-xl font-semibold text-blue-600 hover:underline cursor-pointer">
                                    {productNameDisplay}
                                </h2>
                            </Link>
                            <p>{matchedProducts[0]?.description}</p> {/* 첫 번째 제품 설명만 표시 */}
                            <p className="text-green-600 font-bold mt-2">
                                {totalPrice.toLocaleString()}원 {/* 전체 가격 합계 */}
                            </p>
                            <p>
                                {order.status}
                            </p>
                            <p>{totalStock}개</p> {/* 전체 재고 합계 */}
                            {
                                order.status != "CANCELED" &&
                                (<button
                                    onClick={() => handleCancelOrder(order.id)}
                                    className="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600"
                                    key={order.id}>
                                    주문 취소
                                </button>)
                            }   
                        </div>
                    );
                }
                })}
            </div>
        </div>
    )
}