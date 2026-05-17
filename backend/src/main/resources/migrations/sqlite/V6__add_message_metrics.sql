ALTER TABLE "conversation_message" ADD COLUMN "prompt_tokens" INTEGER;
ALTER TABLE "conversation_message" ADD COLUMN "completion_tokens" INTEGER;
ALTER TABLE "conversation_message" ADD COLUMN "duration" INTEGER;