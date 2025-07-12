import type { NextConfig } from "next";

const nextConfig: NextConfig = {
    eslint: {
        ignoreDuringBuilds: true,
    },
    devServer: {
        host: '0.0.0.0',
        port: 3000,
    },
};

export default nextConfig;
