FROM node:22-bookworm-slim

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

RUN apt-get update \
    && apt-get install --yes --no-install-recommends openssl \
    && rm -rf /var/lib/apt/lists/*

COPY prisma ./prisma
RUN npx prisma generate

COPY tsconfig.json ./
COPY src ./src
RUN npm run build

ENV NODE_ENV=production
EXPOSE 3000

# ponytail: keep dev dependencies for Prisma migrations and manual demo seeding;
# split into a smaller runtime stage when production image size matters.
CMD ["sh", "-c", "npx prisma migrate deploy && node dist/src/main.js"]
