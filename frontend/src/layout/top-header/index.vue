<template>
    <div class="flex w-full items-center justify-between" style="background-color: #dadadb;">
        <div class="before">
            <slot name="before"></slot>
        </div>
        <div class="menu-container">
            <template v-for="(route, index) in routeList" :key="route.name">
                <input type="radio" name="tab" :id="`tab${index}`" :checked="r.name === route.name"
                    v-on:click="go(route)" class="tab"
                    :style="{ left: `calc(var(--label - width, 100px) * ${index} + 2px)}` }" />
                <label class="tab_label" :for="`tab${index}`">{{ route.meta?.title }}</label>
            </template>
            <div class="indicator" :style="indicatorStyle"></div>
        </div>
        <div class="after">
            <slot name="after"></slot>
        </div>
    </div>
</template>
<script setup lang="ts">
import { getChildRouteListByPathAndName } from "@/router/index"
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
const r = useRoute()
const go = (route: any) => {
    router.push({ name: route.name })
}
const router = useRouter()
const props = defineProps<{
    rootRouteName: string,

}>()
const indicatorStyle = computed(() => {
    const index = routeList.value.findIndex(item => r.name == item.name)
    return { left: `calc(var(--label-width, 100px) * ${index} + 2px)` }
})
const routeList = computed(() => {
    return getChildRouteListByPathAndName(props.rootRouteName)
})
</script>
<style scoped>
/* Remove this container when use*/
.component-title {
    width: 100%;
    position: absolute;
    z-index: 999;
    top: 30px;
    left: 0;
    padding: 0;
    margin: 0;
    font-size: 1rem;
    font-weight: 700;
    color: #888;
    text-align: center;
}

.menu-container {
    position: relative;
    display: flex;
    flex-direction: row;
    align-items: flex-start;
    padding: 2px;

}

.indicator {
    content: "";
    width: var(--label-width, 100px);
    height: 28px;
    background: #ffffff;
    position: absolute;
    top: 2px;
    left: 2px;
    z-index: 9;
    border: 0.5px solid rgba(0, 0, 0, 0.04);
    box-shadow: 0px 3px 8px rgba(0, 0, 0, 0.12), 0px 3px 1px rgba(0, 0, 0, 0.04);
    border-radius: 7px;
    transition: all 0.2s ease-out;
}

.tab {
    width: var(--label-width, 100px);
    height: 28px;
    position: absolute;
    z-index: 99;
    outline: none;
    opacity: 0;
}

.tab_label {
    width: var(--label-width, 100px);
    height: 28px;
    position: relative;
    z-index: 999;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 0;
    font-size: 0.75rem;
    opacity: 0.6;
    cursor: pointer;
}
</style>