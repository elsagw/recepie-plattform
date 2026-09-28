# Stage 1: build the static assets with Node.
FROM node:22-alpine AS build
WORKDIR /app

# Copy only the manifest first, so this layer (the slow part) is cached and
# skipped on rebuilds that only change source code.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# Vite bakes env vars into the built JS at build time - a static site has no
# server-side code to read them at runtime, so this must be a build arg.
ARG VITE_API_BASE_URL=http://localhost:8080
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

# Stage 2: serve the built files with nginx - no Node.js in the final image.
FROM nginx:alpine AS runtime
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
