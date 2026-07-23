-- 项目级统一异常配置(JSON,按错误类型留槽:{"validationError": {...}})
ALTER TABLE "project" ADD COLUMN "error_response" TEXT;
