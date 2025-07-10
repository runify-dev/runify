<template>
  <el-aside class="note-aside" style="position: relative;" v-show="elAsideWidth !== 0"
    :style="{ '--el-aside-width': elAsideWidth + 'px' }">
    <slot></slot>
  </el-aside>
  <div class="divider" style="position: absolute;" @mouseover="mouseover" @mousedown="startDorp"
    @mouseleave="mouseleave" @mousedown.stop.prevent :style="dividerStyle">

  </div>
</template>
<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from "vue";
const props = withDefaults(
  defineProps<{
    left?: number;
  }>(),
  { left: 60 }
);
const isDorp = ref<boolean>(false);
const clientX = ref<number>();
const elAsideWidth = computed(() => {
  if (!clientX.value) return 180;
  const asideWidth = clientX.value - props.left;
  if (asideWidth < 100) {
    return 0;
  } else if (clientX.value > 500) {
    return 500;
  } else {
    return asideWidth
  }
});
const isDivider = ref<boolean>(false)
const hoverDivider = ref<boolean>(false)
const dividerStyle = computed(() => {
  return hoverDivider.value
    ? {
      "background-color": "var(--el-border-color)",
      left: props.left + elAsideWidth.value + 'px'
    }
    : { left: props.left + elAsideWidth.value + 'px' };
});

const dorp = (event: any) => {
  event.stopPropagation();
  event.preventDefault();
  if (isDorp.value) {
    clientX.value = event.clientX;
  }
};

const stopDorp = (event: any) => {
  isDivider.value = false
  hoverDivider.value = false
  if (isDorp.value) {
    isDorp.value = false;
    document.body.style.cursor = "auto";
    clientX.value = event.clientX;
  }
};

const startDorp = (event: any) => {
  event.stopPropagation();
  event.preventDefault();
  isDorp.value = true;
  document.body.style.cursor = "e-resize";
  clientX.value = event.clientX;
};
const mouseleave = () => {
  isDivider.value = false
  if (!isDorp.value) {
    hoverDivider.value = false
  }
}
const mouseover = () => {
  isDivider.value = true
  setTimeout(() => {
    if (isDivider.value) {
      hoverDivider.value = true
    }
  }, 500)
}
onMounted(() => {
  document.addEventListener("mousemove", dorp);
  document.addEventListener("mouseup", stopDorp);
});
onUnmounted(() => {
  document.removeEventListener("mousemove", dorp);
  document.removeEventListener("mouseup", stopDorp);
});
</script>
<style lang="scss"></style>
