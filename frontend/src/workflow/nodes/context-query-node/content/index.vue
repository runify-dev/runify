<template>
  <div>
    <Fieldset legend="说明">
      <p class="text-sm text-slate-500 leading-6">
        载入本会话的跨对话上下文（对话开始时放在循环外执行一次）：
      </p>
      <ul class="text-sm text-slate-500 leading-6 list-disc pl-5">
        <li>摘要：上次固化的对话摘要（含游标）</li>
        <li>历史上下文：游标之后的原文回合（排除本次运行）</li>
        <li>便签：应用级 + 对话级常驻便签（含产物）</li>
        <li>载入边界：本轮历史载入到的时间戳（诊断 / 自定义用）</li>
      </ul>
      <p class="text-sm text-slate-500 leading-6 mt-2">
        输出接「上下文压缩」节点的各初始值；无配置项，会话标识取自运行参数；查询失败输出空值放行。
      </p>
    </Fieldset>
  </div>
</template>

<script setup lang="ts">
function validate() {
  return Promise.resolve({ values: {}, errors: {} })
}

function submit() {
  return Promise.resolve({} as Record<string, string>)
}

defineExpose({ validate, submit })
</script>

<style lang="scss" scoped></style>
