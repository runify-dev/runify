<template>
    <div ref="contentRef"
        class="z-10 shadow-lg outline  outline-black/5  dark:shadow-none dark:-outline-offset-1 dark:outline-white/10 rounded-md h-[500px] w-[400px] translate-x-[50%] translate-y-[-50%] left-[80px] absolute bg-white p-2">
        <div class=" w-full h-full" v-for="key in Object.keys(menu)" :key="key">
            <h5 class="relative
                before:block 
                before:absolute before:translate-x-[-50%] 
                before:translate-y-[-50%] 
                before:top-1/2
                before:left-[2px]
                before:w-[2px] before:h-[80%] before:bg-[var(--el-color-primary)] pl-3
                font-medium">
                {{ key }}</h5>
            <div class="grid grid-cols-2 gap-4">
                <div @click="appendNode(item.node)" v-for="item in menu[key]" :key="item.node.type"
                    class="hover:bg-gray-100 hover:text-gray-900 hover:cursor-pointer overflow-hidden shadow-lg outline  outline-black/5  dark:shadow-none dark:-outline-offset-1 dark:outline-white/10 bg-white dark:bg-gray-800 p-2 rounded-lg grid grid-flow-col  grid-rows-1">

                    <div class="col-span-1 flex items-center mr-1">
                        <img class="size-12 shrink-0 object-fill w-8 h-8 rounded-md" :src="item.imgSrc" alt="节点Icon" />
                    </div>
                    <div class="col-span-2 truncate font-normal align-middle text-sm/8">{{
                        item.node.properties.name
                        }}
                    </div>
                </div>

            </div>


        </div>

    </div>
</template>
<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { getNodeMenuList, type WorkflowType } from "@/workflow/common/data"
const contentRef = ref();
const props = defineProps<{
    appendNode: (node: any) => void,
    close: () => void,
    workflowType: WorkflowType
}>()
onMounted(() => {
    document.addEventListener("mousedown", (e) => {
        if (contentRef.value) {
            if (e.target !== contentRef.value && !contentRef.value.contains(e.target)) {
                props.close()
            }
        }

    })
})
const menu: any = getNodeMenuList(props.workflowType)
</script>
<style lang="scss"></style>