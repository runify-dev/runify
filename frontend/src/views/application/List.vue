<template>
    <el-row :gutter="10" style="padding: 8px;">
        <el-col v-for="node in nodeList" :key="node.id" :xs="24" :sm="12" :md="8" :lg="6" :xl="6" class="mb-16">
            <CardBox :title="node.name"
                @click="router.push({ name: 'applicationDetails', params: { id: node.id, type: node.subtype } })">
                <template #description>
                    <div v-if="node.excerpt">
                        <MdPreview :modelValue="node.excerpt">
                        </MdPreview>
                    </div>
                    <el-skeleton v-else :rows="4" />
                </template>
                <template #tag>
                    <el-tag class="blue-tag" style="height: 22px">
                        {{ node.subtype }}
                    </el-tag>
                </template>
            </CardBox>
        </el-col>
    </el-row>
</template>
<script setup lang="ts">
import { useRoute } from 'vue-router';
import CardBox from "@/components/card-box/index.vue"
import { MdPreview } from 'md-editor-v3';
import { onMounted, ref, watch } from "vue";
import { type Node, } from '@/api/type/node';
import { useRouter } from 'vue-router';
import NodeApi from "@/api/node"
const router = useRouter()
const nodeList = ref<Array<Node>>([])

const route = useRoute()

const listApplication = () => {
    const {
        params: { id } // id为应用id
    } = route as any
    const query: any = { source: 'application', type: 'file' }
    if (id === 'share') {
        query['share'] = true
    } else if (id === 'star') {
        query['star'] = true
    } else if (id != 'all') {
        query['parentId'] = id
    }
    NodeApi.list(query).then(ok => {
        nodeList.value = ok.data
    })
}
watch(route, () => {
    listApplication()
})
onMounted(() => {
    listApplication()
})
</script>
<style lang="scss" scoped>
:deep(.el-skeleton__p) {
    height: 6px;
    margin-top: 0px;
}

:deep(.md-editor-preview) {
    font-size: 8px;
}
</style>