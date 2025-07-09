"use client"
import { useRouter ,useSearchParams} from "next/navigation";
import { useEffect } from "react";

export default function FailPage() {
    const searchParams = useSearchParams();

    useEffect(() => {
        if (window.opener) {
            window.opener.postMessage({type: 'payment_failed'}, window.location.origin);
        }
        alert('결제 승인에 실패했습니다.');
        setTimeout(() => {
            window.close();
        }, 3000);

    })



    return (
        <div className="result wrapper">
            <div className="box_section">
                <h2>
                    결제 실패
                </h2>
                <p>{`에러 코드: ${searchParams.get("code")}`}</p>
                <p>{`실패 사유: ${searchParams.get("message")}`}</p>
            </div>
        </div>
    );
}