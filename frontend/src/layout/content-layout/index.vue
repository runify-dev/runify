<template>
    <div class="a-content-hander">

        <ContentHandler></ContentHandler>
    </div>
    <div class="a-content-container">

    </div>
</template>
<script setup lang="ts">
import ContentHandler from "@/layout/content-layout/components/ContentHandler.vue"
</script>
<style lang="scss" scoped>
.a-content-hander {
    height: 32px;
}

.a-content-container {
    width: 100%;
    height: 100%;


}
</style>