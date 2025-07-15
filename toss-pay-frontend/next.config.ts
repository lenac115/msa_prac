import type { NextConfig } from "next";
import path from "node:path";

const nextConfig: NextConfig = {
    eslint: {
        ignoreDuringBuilds: true,
    },
    devServer: {
        host: '0.0.0.0',
        port: 3000,
    },
    webpack: (config) => {
        config.resolve.alias = {
            ...config.resolve.alias,
            '@polyfills': path.resolve(__dirname, 'src/utils/polyfills'),
        };
    }

};

export default nextConfig;
