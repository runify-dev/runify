<template>
  <el-form :model="form" ref="formRef" :rules="rules" label-width="auto" label-position="top">
    <el-row :gutter="10">
      <el-col :span="12">
        <el-form-item label="Host" prop="collection.host">
          <el-input v-model="form.collection.host" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="Port" prop="collection.port">
          <el-input v-model="form.collection.port" />
        </el-form-item>
      </el-col>
    </el-row>
    <el-row :gutter="10">
      <el-col :span="12">
        <el-form-item label="Database" prop="collection.database">
          <el-input v-model="form.collection.database" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="user" prop="collection.user">
          <el-input v-model="form.collection.user" />
        </el-form-item>
      </el-col>
    </el-row>
    <el-row :gutter="10">
      <el-col :span="12">
        <el-form-item label="password" prop="collection.password">
          <el-input v-model="form.collection.password" />
        </el-form-item>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="12">
        <el-form-item label="最大连接池" prop="pool.maxSize">
          <el-input v-model="form.pool.maxSize" />
        </el-form-item>
      </el-col>
    </el-row>
  </el-form>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import type { FormRules, FormInstance } from 'element-plus'
const formRef = ref<FormInstance>()
interface Collection {
  host: string
  port: number
  database: string
  user: string
  password: string
}
interface Pool {
  maxSize: number
}
interface Database {
  pool: Pool
  collection: Collection
}
const form = ref<Database>({
  collection: {
    host: '',
    port: 5432,
    database: '',
    user: 'postgre',
    password: ''
  },
  pool: { maxSize: 10 }
})
const rules = ref<FormRules<Database>>({
  'collection.host': [{ required: true, message: '请输入Host', trigger: 'blur' }],
  'collection.database': [{ required: true, message: '请输入数据库', trigger: 'blur' }],
  'collection.password': [{ required: true, message: '请输入密码', trigger: 'blur' }],
  'collection.port': [{ required: true, message: '请输入端口', trigger: 'blur' }],
  'collection.user': [{ required: true, message: '请输入用户', trigger: 'blur' }],
  'pool.maxSize': [{ required: true, message: '请输入最大连接数量', trigger: 'blur' }]
})
const validate = () => {
  return formRef.value?.validate()
}
const getData = () => {
  return form.value
}
defineExpose({ validate, getData })
</script>
<style lang="scss" scoped></style>
