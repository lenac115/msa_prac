'use client';
export const dynamic = 'force-dynamic';

import { Suspense } from 'react';
import PaymentButton from './ReadyPageWrapper';

export default function ReadyPage() {
    return (
        <Suspense fallback={<div>로딩 중...</div>}>
            <PaymentButton />
        </Suspense>
    );
}