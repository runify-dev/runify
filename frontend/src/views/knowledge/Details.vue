<template>
    <EditorVue :id="(route.params.id as string)" :type="(route.params.type as string)"></EditorVue>
</template>
<script setup lang="ts">

import EditorVue from "@/views/knowledge/editor/index.vue"

import { useRoute } from 'vue-router';
const route = useRoute()


</script>
<style lang="scss" scoped></style>