-- Add Firebase UID for users created or linked through Firebase Google login.
ALTER TABLE "User" ADD COLUMN "firebaseUid" TEXT;

CREATE UNIQUE INDEX "User_firebaseUid_key" ON "User"("firebaseUid");
