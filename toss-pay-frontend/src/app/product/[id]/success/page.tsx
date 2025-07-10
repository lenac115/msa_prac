'use client';
export const dynamic = 'force-dynamic';

import { Suspense } from 'react';
import SuccessPageWrapper from './SuccessPageWrapper';

export default function Page() {
    return (
        <Suspense fallback={<div>결제 결과 확인 중...</div>}>
            <SuccessPageWrapper />
        </Suspense>
    );
}
