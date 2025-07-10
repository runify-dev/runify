<script setup lang="ts">
import { onMounted } from "vue";
import { SwaggerUIBundle } from "swagger-ui-dist";
import "swagger-ui-dist/swagger-ui.css";

onMounted(() => {
    const ui = SwaggerUIBundle({
        dom_id: "#swagger-container",
        url: "http://localhost:8080/doc/",
        presets: [SwaggerUIBundle.presets.apis, SwaggerUIBundle.SwaggerUIStandalonePreset],
    });
    console.log(ui)
});
</script>

<template>
    <div class="swagger" id="swagger-container" />
</template>