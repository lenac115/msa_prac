'use client';
export const dynamic = 'force-dynamic';

import { Suspense } from 'react';
import CheckoutPageWrapper from './CheckoutPageWrapper';

export default function Page() {
    return (
        <Suspense fallback={<div>결제 페이지 로딩 중...</div>}>
            <CheckoutPageWrapper />
        </Suspense>
    );
}
