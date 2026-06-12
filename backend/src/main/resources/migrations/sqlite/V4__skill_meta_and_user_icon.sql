-- Add meta field to skill table
ALTER TABLE "skill" ADD COLUMN "meta" TEXT;

-- Update user icon from ./user.png to ./user.svg
UPDATE "user" SET "icon" = './user.svg' WHERE "icon" = './user.png';
