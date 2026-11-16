/** @type {import('next').NextConfig} */
const nextConfig = {
  // Self-contained server bundle, used by the production Docker image.
  output: "standalone",
  poweredByHeader: false,
};

export default nextConfig;
