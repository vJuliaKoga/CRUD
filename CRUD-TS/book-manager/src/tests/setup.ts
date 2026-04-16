import { execFileSync } from "child_process";
import { PrismaClient } from "@prisma/client";
import { afterAll, beforeAll, beforeEach } from "vitest";

const testDatabaseUrl =
  "postgresql://bookuser:bookpass123@localhost:5432/bookmanager?schema=bookmanager_test";

process.env.DATABASE_URL = testDatabaseUrl;

const prisma = new PrismaClient({
  datasources: {
    db: {
      url: testDatabaseUrl,
    },
  },
});

beforeAll(async () => {
  const env = {
    ...process.env,
    DATABASE_URL: testDatabaseUrl,
    RUST_LOG: "schema_engine=trace",
  };

  if (process.platform === "win32") {
    execFileSync(process.env.ComSpec ?? "cmd.exe", ["/d", "/s", "/c", "npx prisma db push --force-reset --skip-generate"], {
      env,
      stdio: "ignore",
    });
    return;
  }

  execFileSync("npx", ["prisma", "db", "push", "--force-reset", "--skip-generate"], {
    env,
    stdio: "ignore",
  });
});

beforeEach(async () => {
  await prisma.book.deleteMany();
});

afterAll(async () => {
  await prisma.$disconnect();
});

export { prisma };
