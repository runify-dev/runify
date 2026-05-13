export const samplePatch = `diff --git a/Users/hu/work/run/frontend/src/workflow/nodes/note-search-node/content/index.vue b/Users/hu/work/run/frontend/src/workflow/nodes/note-search-node/content/index.vue
--- a/Users/hu/work/run/frontend/src/workflow/nodes/note-search-node/content/index.vue
+++ b/Users/hu/work/run/frontend/src/workflow/nodes/note-search-node/content/index.vue
@@ -1,5 +1,5 @@
 noteTreeAPI.listTree('root').then((res) => {
   const tree = toTree(res.data || [])
   const folders = toTreeSelectNodes(tree)
-  folderOptions.value = [ROOT_OPTION, ...folders]
+  folderOptions.value = folders.length > 0 ? folders : [ROOT_OPTION]
 })
diff --git a/src/new_file.ts b/src/new_file.ts
new file mode 100644
--- /dev/null
+++ b/src/new_file.ts
@@ -0,0 +1,4 @@
+export function newFeature() {
+  return 'this is brand new';
+}
+
diff --git a/src/legacy.ts b/src/legacy.ts
deleted file mode 100644
--- a/src/legacy.ts
+++ /dev/null
@@ -1,2 +0,0 @@
-// deprecated, will be removed
-export const DEPRECATED = true;
diff --git a/src/old_name.ts b/src/new_name.ts
similarity index 100%
rename from src/old_name.ts
rename to src/new_name.ts`
